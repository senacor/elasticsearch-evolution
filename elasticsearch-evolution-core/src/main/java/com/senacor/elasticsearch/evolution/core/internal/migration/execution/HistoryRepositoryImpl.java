package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.internal.model.MigrationVersion;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestClient;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestResponse;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.requireNotBlank;
import static com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestClient.APPLICATION_JSON_UTF8;
import static com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestClient.HEADER_NAME_CONTENT_TYPE;
import static com.senacor.elasticsearch.evolution.rest.abstracion.HttpMethod.*;
import static java.util.Objects.requireNonNull;

/**
 * @author Andreas Keefer
 */
public class HistoryRepositoryImpl implements HistoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(HistoryRepositoryImpl.class);
    private static final String INTERNAL_LOCK_VERSION = "0.1";
    private static final MigrationVersion INTERNAL_VERSIONS = MigrationVersion.fromVersion("0");
    static final String INDEX_TYPE_DOC = "_doc";

    private final EvolutionRestClient restClient;
    private final String historyIndex;
    private final MigrationScriptProtocolMapper migrationScriptProtocolMapper;
    private final int querySize;
    private final ObjectMapper objectMapper;

    public HistoryRepositoryImpl(EvolutionRestClient restClient,
                                 String historyIndex,
                                 MigrationScriptProtocolMapper migrationScriptProtocolMapper,
                                 int querySize,
                                 ObjectMapper objectMapper) {
        this.restClient = requireNonNull(restClient, "restClient must not be null");
        this.historyIndex = requireNotBlank(historyIndex, "historyIndex must not be blank: %s", historyIndex);
        this.migrationScriptProtocolMapper = requireNonNull(migrationScriptProtocolMapper, "migrationScriptProtocolMapper must not be null");
        this.querySize = querySize;
        this.objectMapper = objectMapper;
    }

    @Override
    public NavigableSet<MigrationScriptProtocol> findAll() throws MigrationException {
        try {
            final EvolutionRestResponse searchResponse = restClient.execute(POST,
                    "/" + historyIndex + "/_search",
                    Map.of(HEADER_NAME_CONTENT_TYPE, APPLICATION_JSON_UTF8),
                    indicesOptions(IndexOptions.lenientExpandOpen()),
                    "{\"size\":" + querySize + "}");
            final Optional<String> bodyAsString = searchResponse.body();
            logger.debug("findAll res: {} (body={})", searchResponse, bodyAsString);
            validateHttpStatusIs2xx(searchResponse, "findAll");

            final SearchResponse body = objectMapper.readValue(bodyAsString.orElse(null), SearchResponse.class);

            // map and order
            return body.getHits().getHitList().stream()
                    .map(Hit::getSource)
                    .map(migrationScriptProtocolMapper::mapFromMap)
                    // filter protocols with 0 major version, because they are used internal
                    .filter(protocol -> protocol.getVersion().isMajorNewerThan(INTERNAL_VERSIONS))
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new MigrationException("findAll failed!", e);
        }
    }

    @Override
    public void saveOrUpdate(MigrationScriptProtocol migrationScriptProtocol) throws MigrationException {
        try {
            final String id = requireNonNull(migrationScriptProtocol.getVersion(), "migrationScriptProtocol.version must not be null").getVersion();
            final Map<String, Object> source = migrationScriptProtocolMapper.mapToMap(migrationScriptProtocol);
            final EvolutionRestResponse res = restClient.execute(PUT,
                    "/" + historyIndex + "/_doc/" + id,
                    Map.of(HEADER_NAME_CONTENT_TYPE, APPLICATION_JSON_UTF8),
                    null,
                    objectMapper.writeValueAsString(source));

            if (logger.isDebugEnabled()) {
                logger.debug("saveOrUpdate res: {} (body={})", res.asString(), res.body());
            }
            validateHttpStatusIs2xx(res, "saveOrUpdate");
        } catch (IOException e) {
            throw new MigrationException("saveOrUpdate of '%s' failed!".formatted(migrationScriptProtocol), e);
        }
    }

    @Override
    public boolean isLocked() throws MigrationException {
        try {
            refresh(historyIndex);

            final String countQuery = "{\"query\":{\"term\":{\"" + MigrationScriptProtocolMapper.LOCKED_FIELD_NAME + "\":{\"value\":true}}}}";
            final long count = executeCountRequest(Optional.of(countQuery));

            if (count == 0L) {
                logger.debug("index '{}' is not locked: no locked documents in index.", historyIndex);
                return false;
            }

            logger.debug("index '{}' is locked: {} locked documents found.", historyIndex, count);
            return true;
        } catch (IOException e) {
            throw new MigrationException("isLocked check failed!", e);
        }
    }

    private long executeCountRequest(Optional<String> countQuery) throws IOException {
        final EvolutionRestResponse countResponse = restClient.execute(POST,
                "/" + historyIndex + "/_count",
                countQuery.isPresent()
                        ? Map.of(HEADER_NAME_CONTENT_TYPE, APPLICATION_JSON_UTF8)
                        : null,
                indicesOptions(IndexOptions.lenientExpandOpen()),
                countQuery.orElse(null));

        validateHttpStatusIs2xx(countResponse, "isLocked");

        final JsonNode countResBody = objectMapper.readTree(countResponse.body().orElseThrow(() -> new IllegalStateException("count response body must be present")));
        return countResBody.get("count").asLong();
    }

    @Override
    public boolean lock() {
        try {
            final long countAll = executeCountRequest(Optional.empty());
            if (countAll == 0L) {
                saveOrUpdate(new MigrationScriptProtocol()
                        .setVersion(INTERNAL_LOCK_VERSION)
                        .setScriptName("-")
                        .setDescription("lock entry")
                        .setExecutionRuntimeInMillis(0)
                        .setSuccess(true)
                        .setChecksum(0)
                        .setExecutionTimestamp(OffsetDateTime.now())
                        .setIndexName(historyIndex)
                        .setLocked(true));
            } else {
                executeLockRequest(true, "lock");
            }
            return true;
        } catch (IOException e) {
            logger.warn("lock failed", e);
            return false;
        }
    }

    @Override
    public boolean unlock() {
        try {
            refresh(historyIndex);

            final Map<String, String> urlParams = new HashMap<>(indicesOptions(IndexOptions.lenientExpandOpen()));
            urlParams.put("requests_per_second", "-1");
            urlParams.put("refresh", "true");
            String body = "{\"script\":{" +
                    "\"source\":\"ctx.op = \\\"delete\\\"\"," +
                    "\"lang\":\"painless\"}," +
                    "\"size\":1000," +
                    "\"query\":{\"term\":{\"" + MigrationScriptProtocolMapper.VERSION_FIELD_NAME + "\":{\"value\":\"" + INTERNAL_LOCK_VERSION + "\"}}}}";

            final EvolutionRestResponse deleteInternalLockRes = restClient.execute(POST,
                    "/" + historyIndex + "/_update_by_query",
                    Map.of(HEADER_NAME_CONTENT_TYPE, APPLICATION_JSON_UTF8),
                    urlParams,
                    body);

            if (logger.isDebugEnabled()) {
                logger.debug("unlock.deleteLockEntry res: {} (body={})", deleteInternalLockRes.asString(), deleteInternalLockRes.body());
            }

            executeLockRequest(false, "unlock.removeLock");
            return true;
        } catch (IOException e) {
            logger.warn("unlock failed", e);
            return false;
        }
    }

    private void executeLockRequest(boolean lock, String debugContext) throws IOException {
        final Map<String, String> urlParams = new HashMap<>(indicesOptions(IndexOptions.lenientExpandOpen()));
        urlParams.put("requests_per_second", "-1");
        urlParams.put("refresh", "true");
        String body = "{\"script\":" +
                "{\"source\":\"ctx._source." + MigrationScriptProtocolMapper.LOCKED_FIELD_NAME + " = params.lock\"," +
                "\"lang\":\"painless\"," +
                "\"params\":{\"lock\":" + lock + "}" +
                "}," +
                "\"size\":1000," +
                "\"query\":{\"term\":{\"" + MigrationScriptProtocolMapper.LOCKED_FIELD_NAME + "\":{\"value\":" + !lock + "}}}}";

        final EvolutionRestResponse updateByQueryResponse = restClient.execute(POST,
                "/" + historyIndex + "/_update_by_query",
                Map.of(HEADER_NAME_CONTENT_TYPE, APPLICATION_JSON_UTF8),
                urlParams,
                body);

        if (logger.isDebugEnabled()) {
            logger.debug("{} res: {} (body={})", debugContext, updateByQueryResponse.asString(), updateByQueryResponse.body());
        }
    }

    @Override
    public boolean createIndexIfAbsent() throws MigrationException {
        try {
            EvolutionRestResponse existsRes = restClient.execute(HEAD, "/" + historyIndex);
            boolean exists = 200 == existsRes.statusCode();
            if (exists) {
                logger.debug("Elasticsearch-Evolution history index '{}' already exists.", historyIndex);
                return false;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Elasticsearch-Evolution history index '{}' does not yet exists. Res={}", historyIndex, existsRes.asString());
            }

            // create index
            EvolutionRestResponse createRes = restClient.execute(PUT, "/" + historyIndex);
            if (hasNotStatusCode2xx(createRes)) {
                throw new IllegalStateException("Could not create Elasticsearch-Evolution history index '" + historyIndex +
                        "'. Create res=" + createRes.asString() + " (body=" + createRes.body() + ")");
            }
            logger.debug("created Elasticsearch-Evolution history index '{}'", historyIndex);
            return true;
        } catch (IOException e) {
            throw new MigrationException("createIndexIfAbsent failed!", e);
        }
    }

    private boolean hasNotStatusCode2xx(EvolutionRestResponse response) {
        return isNotStatusCode2xx(response.statusCode());
    }

    private boolean isNotStatusCode2xx(int statusCode) {
        return statusCode < 200 || statusCode > 299;
    }

    private void validateHttpStatusIs2xx(EvolutionRestResponse response, String description) throws MigrationException {
        validateHttpStatusIs2xx(response.statusCode(), description + " (" + response.statusReasonPhrase() + ")");
    }

    void validateHttpStatusIs2xx(int statusCode, String description) throws MigrationException {
        if (isNotStatusCode2xx(statusCode)) {
            throw new MigrationException("%s - response status is not OK: %s".formatted(description, statusCode));
        }
    }

    /**
     * refresh the index to get all pending documents in the index which are currently in the indexing process.
     * This is a bit like a flush in JPA.
     */
    void refresh(String... indices) {
        try {
            EvolutionRestResponse res = restClient.execute(GET,
                    "/" + expandIndicesForUrl(indices) + "/_refresh",
                    null,
                    indicesOptions(IndexOptions.lenientExpandOpen()),
                    null);

            validateHttpStatusIs2xx(res, "refresh");
        } catch (IOException e) {
            throw new MigrationException("refresh failed!", e);
        }
    }

    private String expandIndicesForUrl(String... indices) {
        return String.join(",", indices);
    }

    private Map<String, String> indicesOptions(IndexOptions indicesOptions) {
        Map<String, String> nameValuePairs = new HashMap<>();
        nameValuePairs.put("ignore_unavailable", Boolean.toString(indicesOptions.ignoreUnavailable()));
        nameValuePairs.put("allow_no_indices", Boolean.toString(indicesOptions.allowNoIndices()));
        String expandWildcards;
        if (!indicesOptions.expandWildcardsOpen() && !indicesOptions.expandWildcardsClosed()) {
            expandWildcards = "none";
        } else {
            StringJoiner joiner = new StringJoiner(",");
            if (indicesOptions.expandWildcardsOpen()) {
                joiner.add("open");
            }
            if (indicesOptions.expandWildcardsClosed()) {
                joiner.add("closed");
            }
            expandWildcards = joiner.toString();
        }
        nameValuePairs.put("expand_wildcards", expandWildcards);
        return nameValuePairs;
    }

    /**
     * simple replacement for {@link org.elasticsearch.action.support.IndicesOptions}
     */
    private static final class IndexOptions {

        private static final IndexOptions LENIENT_EXPAND_OPEN = new IndexOptions(
                true,
                true,
                true,
                false);

        private final boolean ignoreUnavailable;
        private final boolean allowNoIndices;
        private final boolean expandWildcardsOpen;
        private final boolean expandWildcardsClosed;

        public IndexOptions(boolean ignoreUnavailable,
                            boolean allowNoIndices,
                            boolean expandWildcardsOpen,
                            boolean expandWildcardsClosed) {
            this.ignoreUnavailable = ignoreUnavailable;
            this.allowNoIndices = allowNoIndices;
            this.expandWildcardsOpen = expandWildcardsOpen;
            this.expandWildcardsClosed = expandWildcardsClosed;
        }

        public static IndexOptions lenientExpandOpen() {
            return LENIENT_EXPAND_OPEN;
        }

        public boolean ignoreUnavailable() {
            return ignoreUnavailable;
        }

        public boolean allowNoIndices() {
            return allowNoIndices;
        }

        public boolean expandWildcardsOpen() {
            return expandWildcardsOpen;
        }

        public boolean expandWildcardsClosed() {
            return expandWildcardsClosed;
        }
    }

    @Value
    static class SearchResponse {
        long took;
        @JsonProperty("timed_out")
        boolean timedOut;
        Hits hits;
    }

    @Value
    static class Hits {
        TotalHits total;
        @JsonProperty("max_score")
        BigDecimal maxScore;
        @JsonProperty("hits")
        List<Hit> hitList;
    }

    @Value
    static class TotalHits {
        long value;
        /**
         * Values of relation:
         * - "eq" = Accurate
         * - "gte" = "Lower bound, including returned documents"
         */
        String relation;
    }

    @Value
    static class Hit {
        @JsonProperty("_id")
        String id;
        @JsonProperty("_source")
        Map<String, Object> source;
    }
}