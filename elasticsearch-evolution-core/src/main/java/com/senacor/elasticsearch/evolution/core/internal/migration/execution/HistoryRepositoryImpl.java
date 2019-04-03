package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.internal.model.MigrationVersion;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.requireNotBlank;
import static java.util.Objects.requireNonNull;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

/**
 * @author Andreas Keefer
 */
public class HistoryRepositoryImpl implements HistoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(HistoryRepositoryImpl.class);
    private static final String INTERNAL_LOCK_VERSION = "0.1";
    private static final MigrationVersion INTERNAL_VERSIONS = MigrationVersion.fromVersion("0");
    static final String INDEX_TYPE_DOC = "_doc";

    private final RestHighLevelClient restHighLevelClient;
    private final String historyIndex;
    private final MigrationScriptProtocolMapper migrationScriptProtocolMapper;

    public HistoryRepositoryImpl(RestHighLevelClient restHighLevelClient, String historyIndex, MigrationScriptProtocolMapper migrationScriptProtocolMapper) {
        this.restHighLevelClient = requireNonNull(restHighLevelClient, "restHighLevelClient must not be null");
        this.historyIndex = requireNotBlank(historyIndex, "historyIndex must not be blank: {}", historyIndex);
        this.migrationScriptProtocolMapper = requireNonNull(migrationScriptProtocolMapper, "migrationScriptProtocolMapper must not be null");
    }

    @Override
    public NavigableSet<MigrationScriptProtocol> findAll() throws MigrationException {
        try {
            SearchResponse searchResponse = restHighLevelClient.search(
                    new SearchRequest(historyIndex)
                            .source(new SearchSourceBuilder()
                                    // TODO (ak) make this configurable
                                    .size(1000))
                            .indicesOptions(IndicesOptions.lenientExpandOpen()),
                    DEFAULT);
            logger.debug("findAll res: {}", searchResponse);
            validateHttpStatus2xxOK(searchResponse.status(), "findAll");

            // map and order
            TreeSet<MigrationScriptProtocol> res = new TreeSet<>();
            Arrays.stream(searchResponse.getHits().getHits())
                    .map(SearchHit::getSourceAsMap)
                    .map(migrationScriptProtocolMapper::mapFromMap)
                    // filter protocols with 0 major version, because they are used internal
                    .filter(protocol -> protocol.getVersion().isMajorNewerThan(INTERNAL_VERSIONS))
                    .forEach(res::add);
            return res;
        } catch (IOException e) {
            throw new MigrationException("findAll failed!", e);
        }
    }

    @Override
    public void saveOrUpdate(MigrationScriptProtocol migrationScriptProtocol) throws MigrationException {
        try {
            HashMap<String, Object> source = migrationScriptProtocolMapper.mapToMap(migrationScriptProtocol);
            IndexResponse res = restHighLevelClient.index(
                    new IndexRequest(historyIndex)
                            .type(INDEX_TYPE_DOC)
                            .id(requireNonNull(migrationScriptProtocol.getVersion(), "migrationScriptProtocol.version must not be null").getVersion())
                            .source(source),
                    DEFAULT);
            logger.debug("saveOrUpdate res: {}", res);
            validateHttpStatus2xxOK(res.status(), "saveOrUpdate");
        } catch (IOException e) {
            throw new MigrationException(String.format("saveOrUpdate of '%s' failed!", migrationScriptProtocol), e);
        }
    }

    @Override
    public boolean isLocked() throws MigrationException {
        try {
            refresh(historyIndex);
            CountRequest countRequest = new CountRequest(historyIndex)
                    .source(new SearchSourceBuilder()
                            .query(QueryBuilders
                                    .termQuery(MigrationScriptProtocolMapper.LOCKED_FIELD_NAME, true)))
                    .indicesOptions(IndicesOptions.lenientExpandOpen());
            CountResponse countResponse = restHighLevelClient.count(countRequest, DEFAULT);
            validateHttpStatus2xxOK(countResponse.status(), "isLocked");

            if (countResponse.getCount() == 0L) {
                logger.debug("index '{}' is not locked: no locked documents in index.", historyIndex);
                return false;
            }
            logger.debug("index '{}' is locked: {} locked documents found.", historyIndex, countResponse.getCount());
            return true;
        } catch (IOException e) {
            throw new MigrationException("isLocked check failed!", e);
        }
    }

    @Override
    public boolean lock() {
        try {
            CountRequest countAllReq = new CountRequest(historyIndex)
                    .indicesOptions(IndicesOptions.lenientExpandOpen());
            CountResponse countAllRes = restHighLevelClient.count(countAllReq, DEFAULT);
            validateHttpStatus2xxOK(countAllRes.status(), "lock.count");

            if (countAllRes.getCount() == 0L) {
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
                BulkByScrollResponse bulkByScrollResponse = restHighLevelClient.updateByQuery(createLockQuery(true), DEFAULT);
                logger.debug("lock res: {}", bulkByScrollResponse);
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
            BulkByScrollResponse deleteInternalLockRes = restHighLevelClient.updateByQuery(
                    new UpdateByQueryRequest(historyIndex)
                            .setRefresh(true)
                            .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                            .setQuery(QueryBuilders.termQuery(MigrationScriptProtocolMapper.VERSION_FIELD_NAME, INTERNAL_LOCK_VERSION))
                            .setScript(new Script(ScriptType.INLINE,
                                    "painless",
                                    "ctx.op = \"delete\"",
                                    Collections.emptyMap())),
                    DEFAULT);
            logger.debug("unlock.deleteLockEntry res: {}", deleteInternalLockRes);

            BulkByScrollResponse bulkByScrollResponse = restHighLevelClient.updateByQuery(createLockQuery(false), DEFAULT);
            logger.debug("unlock.removeLock res: {}", bulkByScrollResponse);
            return true;
        } catch (IOException e) {
            logger.warn("unlock failed", e);
            return false;
        }
    }

    private UpdateByQueryRequest createLockQuery(boolean lock) {
        return new UpdateByQueryRequest(historyIndex)
                .setRefresh(true)
                .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                .setQuery(QueryBuilders.termQuery(MigrationScriptProtocolMapper.LOCKED_FIELD_NAME, !lock))
                .setScript(new Script(ScriptType.INLINE,
                        "painless",
                        "ctx._source." + MigrationScriptProtocolMapper.LOCKED_FIELD_NAME + " = params.lock",
                        Collections.singletonMap("lock", lock)));
    }

    @Override
    public boolean createIndexIfAbsent() throws MigrationException {
        try {
            Response existsRes = restHighLevelClient.getLowLevelClient().performRequest(new Request("HEAD", "/" + historyIndex));
            boolean exists = 200 == existsRes.getStatusLine().getStatusCode();
            if (exists) {
                logger.debug("Elasticsearch-Evolution history index '{}' already exists.", historyIndex);
                return false;
            }
            logger.debug("Elasticsearch-Evolution history index '{}' does not yet exists. Res={}", historyIndex, existsRes);

            // create index
            Response createRes = restHighLevelClient.getLowLevelClient().performRequest(new Request("PUT", "/" + historyIndex));
            if (existsRes.getStatusLine().getStatusCode() < 200 || createRes.getStatusLine().getStatusCode() > 299) {
                throw new IllegalStateException("Could not create Elasticsearch-Evolution history index '" + historyIndex + "'. Create res=" + createRes);
            }
            logger.debug("created Elasticsearch-Evolution history index '{}'", historyIndex);
            return true;
        } catch (IOException e) {
            throw new MigrationException("createIndexIfAbsent failed!", e);
        }
    }

    /**
     * validates that HTTP status code is a 2xx code.
     *
     * @param status      status
     * @param description is used in case of a non 2xx status code in the exception message.
     * @throws MigrationException when the given status code is not a 2xx code.
     */
    void validateHttpStatus2xxOK(RestStatus status, String description) throws MigrationException {
        int statusCode = status.getStatus();
        if (statusCode < 200 || statusCode >= 300) {
            throw new MigrationException(String.format("%s - response status is not OK: %s", description, statusCode));
        }
    }

    /**
     * refresh the index to get all pending documents in the index which are currently in the indexing process.
     * This is a bit like a flush in JPA.
     */
    void refresh(String... indices) {
        try {
            RefreshResponse res = restHighLevelClient.indices().refresh(
                    new RefreshRequest(indices)
                            .indicesOptions(IndicesOptions.lenientExpandOpen())
                    , DEFAULT);
            validateHttpStatus2xxOK(res.getStatus(), "refresh");
        } catch (IOException e) {
            throw new MigrationException("refresh failed!", e);
        }
    }
}
