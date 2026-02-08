package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationService;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationVersion;
import com.senacor.elasticsearch.evolution.core.api.migration.java.Context;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.JavaMigrationRequestContent;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.MigrationScriptRequest;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigration;
import com.senacor.elasticsearch.evolution.core.internal.utils.RandomUtils;
import com.senacor.elasticsearch.evolution.rest.abstraction.EvolutionRestClient;
import com.senacor.elasticsearch.evolution.rest.abstraction.EvolutionRestResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.requireCondition;
import static java.util.Objects.requireNonNull;

/**
 * @author Andreas Keefer
 */
public class MigrationServiceImpl implements MigrationService {

    private static final Logger logger = LoggerFactory.getLogger(MigrationServiceImpl.class);

    private final HistoryRepository historyRepository;
    private final int waitUntilUnlockedMinTimeInMillis;
    private final int waitUntilUnlockedMaxTimeInMillis;
    private final EvolutionRestClient<?> restClient;
    private final String defaultContentType;
    private final Charset encoding;
    private final boolean validateOnMigrate;
    private final ElasticsearchEvolutionConfig config;
    private final boolean outOfOrder;

    private final String baselineVersion;

    public MigrationServiceImpl(HistoryRepository historyRepository,
                                int waitUntilUnlockedMinTimeInMillis,
                                int waitUntilUnlockedMaxTimeInMillis,
                                EvolutionRestClient<?> restClient,
                                @NonNull ElasticsearchEvolutionConfig config) {
        this.historyRepository = requireNonNull(historyRepository, "historyRepository must not be null");
        this.restClient = requireNonNull(restClient, "restClient must not be null");
        this.defaultContentType = requireNonNull(config.getDefaultContentType());
        this.encoding = requireNonNull(config.getEncoding());
        this.validateOnMigrate = config.isValidateOnMigrate();
        this.config = config;
        this.waitUntilUnlockedMinTimeInMillis = requireCondition(waitUntilUnlockedMinTimeInMillis,
                min -> min >= 0 && min <= waitUntilUnlockedMaxTimeInMillis,
                "waitUntilUnlockedMinTimeInMillis (%s) must not be negative and must not be greater than waitUntilUnlockedMaxTimeInMillis (%s)",
                waitUntilUnlockedMinTimeInMillis, waitUntilUnlockedMaxTimeInMillis);
        this.waitUntilUnlockedMaxTimeInMillis = waitUntilUnlockedMaxTimeInMillis;
        this.baselineVersion = config.getBaselineVersion();
        this.outOfOrder = config.isOutOfOrder();
    }

    @Override
    @NonNull
    public List<MigrationScriptProtocol> executePendingScripts(@NonNull Collection<ParsedMigration<?>> migrationScripts)
            throws MigrationException {
        if (!getPendingScriptsToBeExecuted(migrationScripts).isEmpty()) {
            return executePendingScriptsWithLock(migrationScripts);
        } else {
            return new ArrayList<>();
        }
    }

    private List<MigrationScriptProtocol> executePendingScriptsWithLock(Collection<ParsedMigration<?>> migrationScripts)
            throws MigrationException {
        final List<MigrationScriptProtocol> executedScripts = new ArrayList<>();
        try {
            historyRepository.createIndexIfAbsent();
            waitUntilUnlocked();
            // set a logical index lock
            if (!historyRepository.lock()) {
                throw new MigrationException("could not lock the elasticsearch-evolution history index");
            }

            // get scripts which needs to be executed
            List<ParsedMigration<?>> scriptsToExecute = getPendingScriptsToBeExecuted(migrationScripts);

            // now execute scripts and write protocols to history index
            for (ParsedMigration<?> script : scriptsToExecute) {
                // execute scripts
                ExecutionResult res = executeMigration(script);
                MigrationScriptProtocol executedScriptProtocol = res.getProtocol();
                logger.info("executed migration {}", executedScriptProtocol);
                executedScripts.add(executedScriptProtocol);
                // write protocols to history index
                historyRepository.saveOrUpdate(executedScriptProtocol);
                if (res.getError().isPresent()) {
                    throw res.getError().get();
                }
            }
        } finally {
            // release logical index lock
            if (!historyRepository.unlock()) {
                throw new MigrationException("could not release the elasticsearch-evolution history index lock! Maybe you have to release it manually.");
            }
        }
        return executedScripts;
    }

    /**
     * executes the given migrations and returns a protocol ready to save in the history index
     *
     * @param migrationToExecute the migrations to execute
     * @return unsaved protocol
     */
    ExecutionResult executeMigration(ParsedMigration<?> migrationToExecute) {
        logger.info("executing migration {}", migrationToExecute.getFileNameInfo().getScriptName());
        boolean success = false;
        long startTimeInMillis = System.currentTimeMillis();
        Optional<RuntimeException> error = Optional.empty();
        try {
            if (migrationToExecute.getMigrationRequest() instanceof MigrationScriptRequest migrationScriptRequest) {
                Map<String, String> headers = new HashMap<>(migrationScriptRequest.getHttpHeader());
                if (null != migrationScriptRequest.getBody()
                        && !migrationScriptRequest.getBody().trim().isEmpty()) {
                    String contentType = restClient.getContentType(migrationScriptRequest.getHttpHeader())
                            .orElse(defaultContentType);
                    if (!contentType.contains("charset=")) {
                        logger.debug("no charset is defined for {}, setting to configured encoding {}", migrationToExecute.getFileNameInfo(), encoding);
                        contentType += "; charset=" + encoding;
                    }
                    // remove any existing content-type header (ignore case)
                    headers.entrySet()
                            .removeIf(entry -> EvolutionRestClient.HEADER_NAME_CONTENT_TYPE.equalsIgnoreCase(entry.getKey()));
                    headers.put(EvolutionRestClient.HEADER_NAME_CONTENT_TYPE, contentType);
                }
                EvolutionRestResponse response = restClient.execute(
                        migrationScriptRequest.getHttpMethod(),
                        migrationScriptRequest.getPath(),
                        headers,
                        null,
                        migrationScriptRequest.getBody()
                );

                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    success = true;
                } else {
                    error = Optional.of(new MigrationException(
                            "execution of script '%s' failed with HTTP status %s: %s (body=%s)".formatted(
                                    migrationToExecute.getFileNameInfo(),
                                    statusCode,
                                    response.asString(),
                                    response.body())));
                }
            } else if (migrationToExecute.getMigrationRequest() instanceof JavaMigrationRequestContent javaMigrationRequest) {
                javaMigrationRequest.javaMigration().migrate(Context.of(config, restClient));
                success = true;
            } else {
                throw new IllegalArgumentException("migration request of type '%s' is not supported".formatted(
                        migrationToExecute.getMigrationRequest().getClass()));
            }
        } catch (Exception e) {
            error = Optional.of(new MigrationException("execution of migration '%s' failed".formatted(migrationToExecute.getFileNameInfo()), e));
        }

        return new ExecutionResult(
                new MigrationScriptProtocol()
                        .setExecutionRuntimeInMillis((int) (System.currentTimeMillis() - startTimeInMillis))
                        .setSuccess(success)
                        .setVersion(migrationToExecute.getFileNameInfo().getVersion())
                        .setScriptName(migrationToExecute.getFileNameInfo().getScriptName())
                        .setDescription(migrationToExecute.getFileNameInfo().getDescription())
                        .setChecksum(migrationToExecute.getChecksum())
                        .setExecutionTimestamp(OffsetDateTime.now())
                        .setLocked(true),
                error);
    }

    @Override
    @NonNull
    public List<ParsedMigration<?>> getPendingScriptsToBeExecuted(@NonNull Collection<ParsedMigration<?>> migrationScripts) throws MigrationException {
        if (migrationScripts.isEmpty()) {
            return new ArrayList<>();
        }

        // order migrationScripts by version
        final TreeMap<MigrationVersion, ParsedMigration<?>> scriptsInFilesystemMap = migrationScripts.stream()
                .filter(script -> script.getFileNameInfo().getVersion().isAtLeast(baselineVersion))
                .collect(Collectors.toMap(
                        script -> script.getFileNameInfo().getVersion(),
                        Function.identity(),
                        (oldValue, newValue) -> {
                            throw new MigrationException(
                                    "There are multiple migrations with the same version '%s': [%s, %s]".formatted(
                                            oldValue.getFileNameInfo().getVersion(),
                                            oldValue.getFileNameInfo().getScriptName(),
                                            newValue.getFileNameInfo().getScriptName()));
                        },
                        TreeMap::new));

        List<MigrationScriptProtocol> history = new ArrayList<>(historyRepository.findAll());
        List<ParsedMigration<?>> res = new ArrayList<>(scriptsInFilesystemMap.values());
        if (outOfOrder) {
            for (MigrationScriptProtocol protocol : history) {
                final ParsedMigration<?> parsedMigration = scriptsInFilesystemMap.get(protocol.getVersion());
                if (null == parsedMigration) {
                    logger.warn("""
                            there are less migration scripts than already executed history entries! \
                            You should never delete migration scripts you have already executed. \
                            Or maybe you have to cleanup the Elasticsearch-Evolution history index manually! \
                            Already executed history version {} is not present in migration files\
                            """, protocol.getVersion());
                } else {
                    validateOnMigrateIfEnabled(protocol, parsedMigration);

                    if (protocol.isSuccess()) {
                        res.remove(parsedMigration);
                    }
                }
            }
        } else {
            List<ParsedMigration<?>> orderedScripts = new ArrayList<>(scriptsInFilesystemMap.values());
            for (int i = 0; i < history.size(); i++) {
                // do some checks
                MigrationScriptProtocol protocol = history.get(i);
                if (orderedScripts.size() <= i) {
                    logger.warn("""
                            there are less migration scripts than already executed history entries! \
                            You should never delete migration scripts you have already executed. \
                            Or maybe you have to cleanup the Elasticsearch-Evolution history index manually! \
                            history version at position {} is {}\
                            """, i, protocol.getVersion());
                    break;
                }
                ParsedMigration<?> parsedMigration = orderedScripts.get(i);
                if (!protocol.getVersion().equals(parsedMigration.getFileNameInfo().getVersion())) {
                    throw new MigrationException((
                            """
                            The logged execution in the Elasticsearch-Evolution history index at position %s \
                            is version %s and in the same position in the given migration scripts is version %s! \
                            Out of order execution is not supported. Or maybe you have added new migration scripts \
                            in between or have to cleanup the Elasticsearch-Evolution history index manually\
                            """).formatted(
                            i, protocol.getVersion(), parsedMigration.getFileNameInfo().getVersion()));
                }
                validateOnMigrateIfEnabled(protocol, parsedMigration);

                if (protocol.isSuccess()) {
                    res.remove(parsedMigration);
                }
            }
        }

        return res;
    }

    private void validateOnMigrateIfEnabled(MigrationScriptProtocol protocol,
                                            ParsedMigration<?> parsedMigration) {
        // failed scripts can be edited and retried, but successfully executed scripts may not be modified afterward
        if (validateOnMigrate && protocol.isSuccess() && protocol.getChecksum() != parsedMigration.getChecksum()) {
            throw new MigrationException((
                    """
                    The logged execution for the migration version %s (%s) \
                    has a different checksum from the given migration! \
                    Modifying already-executed migrations is not supported.\
                    """).formatted(
                    protocol.getVersion(), protocol.getScriptName()));
        }
    }

    /**
     * wait until the elasticsearch-evolution history index is unlocked
     */
    void waitUntilUnlocked() {
        while (historyRepository.isLocked()) {
            try {
                int waitTime = RandomUtils.getRandomInt(waitUntilUnlockedMinTimeInMillis, waitUntilUnlockedMaxTimeInMillis);
                logger.info("Elasticsearch-Evolution history index is locked, waiting {}ms until retry...", waitTime);
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                logger.warn("waitUntilUnlocked was interrupted!", e);
            }
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class ExecutionResult {
        @Getter
        private final MigrationScriptProtocol protocol;
        @Getter
        private final Optional<RuntimeException> error;
    }
}
