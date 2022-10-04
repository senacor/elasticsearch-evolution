package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationService;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigrationScript;
import com.senacor.elasticsearch.evolution.core.internal.utils.RandomUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.util.*;
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
    private final RestClient restClient;
    private final ContentType defaultContentType;
    private final Charset encoding;
    private final boolean validateOnMigrate;

    public MigrationServiceImpl(HistoryRepository historyRepository,
                                int waitUntilUnlockedMinTimeInMillis,
                                int waitUntilUnlockedMaxTimeInMillis,
                                RestClient restClient,
                                ContentType defaultContentType,
                                Charset encoding,
                                boolean validateOnMigrate) {
        this.historyRepository = requireNonNull(historyRepository, "historyRepository must not be null");
        this.restClient = requireNonNull(restClient, "restClient must not be null");
        this.defaultContentType = requireNonNull(defaultContentType);
        this.encoding = requireNonNull(encoding);
        this.validateOnMigrate = validateOnMigrate;
        this.waitUntilUnlockedMinTimeInMillis = requireCondition(waitUntilUnlockedMinTimeInMillis,
                min -> min >= 0 && min <= waitUntilUnlockedMaxTimeInMillis,
                "waitUntilUnlockedMinTimeInMillis (%s) must not be negative and must not be greater than waitUntilUnlockedMaxTimeInMillis (%s)",
                waitUntilUnlockedMinTimeInMillis, waitUntilUnlockedMaxTimeInMillis);
        this.waitUntilUnlockedMaxTimeInMillis = waitUntilUnlockedMaxTimeInMillis;
    }

    @Override
    public List<MigrationScriptProtocol> executePendingScripts(Collection<ParsedMigrationScript> migrationScripts) throws MigrationException {
        List<MigrationScriptProtocol> executedScripts = new ArrayList<>();
        if (!migrationScripts.isEmpty()) {
            try {
                historyRepository.createIndexIfAbsent();
                waitUntilUnlocked();
                // set an logical index lock
                if (!historyRepository.lock()) {
                    throw new MigrationException("could not lock the elasticsearch-evolution history index");
                }

                // get scripts which needs to be executed
                List<ParsedMigrationScript> scriptsToExecute = getPendingScriptsToBeExecuted(migrationScripts);

                // now execute scripts and write protocols to history index
                for (ParsedMigrationScript script : scriptsToExecute) {
                    // execute scripts
                    ExecutionResult res = executeScript(script);
                    MigrationScriptProtocol executedScriptProtocol = res.getProtocol();
                    logger.info("executed migration script {}", executedScriptProtocol);
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
        }
        return executedScripts;
    }

    /**
     * executes the given script and returns a protocol ready to save in the history index
     *
     * @param scriptToExecute the script
     * @return unsaved protocol
     */
    ExecutionResult executeScript(ParsedMigrationScript scriptToExecute) {
        logger.info("executing script {}", scriptToExecute.getFileNameInfo().getScriptName());
        boolean success = false;
        long startTimeInMillis = System.currentTimeMillis();
        Optional<RuntimeException> error = Optional.empty();
        try {
            Request request = new Request(scriptToExecute.getMigrationScriptRequest().getHttpMethod().name(),
                    scriptToExecute.getMigrationScriptRequest().getPath());
            if (null != scriptToExecute.getMigrationScriptRequest().getBody()
                    && !scriptToExecute.getMigrationScriptRequest().getBody().trim().isEmpty()) {
                ContentType contentType = scriptToExecute.getMigrationScriptRequest().getContentType()
                        .orElse(defaultContentType);
                if (null == contentType.getCharset()) {
                    logger.debug("no charset is defined for {}, setting to configured encoding {}", scriptToExecute.getFileNameInfo(), encoding);
                    contentType = contentType.withCharset(encoding);
                }
                request.setEntity(new NStringEntity(scriptToExecute.getMigrationScriptRequest().getBody(), contentType));
            }

            RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
            scriptToExecute.getMigrationScriptRequest().getHttpHeader()
                    .forEach(builder::addHeader);
            request.setOptions(builder);

            Response response = restClient.performRequest(request);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                success = true;
            } else {
                error = Optional.of(new MigrationException(String.format(
                        "execution of script '%s' failed with HTTP status %s: %s",
                        scriptToExecute.getFileNameInfo(),
                        statusCode,
                        response.toString())));
            }
        } catch (RuntimeException | IOException e) {
            error = Optional.of(new MigrationException(String.format("execution of script '%s' failed", scriptToExecute.getFileNameInfo()), e));
        }

        return new ExecutionResult(
                new MigrationScriptProtocol()
                        .setExecutionRuntimeInMillis((int) (System.currentTimeMillis() - startTimeInMillis))
                        .setSuccess(success)
                        .setVersion(scriptToExecute.getFileNameInfo().getVersion())
                        .setScriptName(scriptToExecute.getFileNameInfo().getScriptName())
                        .setDescription(scriptToExecute.getFileNameInfo().getDescription())
                        .setChecksum(scriptToExecute.getChecksum())
                        .setExecutionTimestamp(OffsetDateTime.now())
                        .setLocked(true),
                error);
    }

    /**
     * This method returns only those scripts, which must be executed.
     * Already executed scripts will be filtered out.
     * the returned scripts must be executed in the returned order.
     *
     * @param migrationScripts all migration scripts that were potentially executed earlier.
     * @return list of ordered scripts which must be executed
     */
    List<ParsedMigrationScript> getPendingScriptsToBeExecuted(Collection<ParsedMigrationScript> migrationScripts) {
        // order migrationScripts by version
        List<ParsedMigrationScript> orderedScripts = new ArrayList<>(migrationScripts.stream()
                .collect(Collectors.toMap(
                        script -> script.getFileNameInfo().getVersion(),
                        script -> script,
                        (oldValue, newValue) -> newValue,
                        TreeMap::new))
                .values());

        List<MigrationScriptProtocol> history = new ArrayList<>(historyRepository.findAll());
        List<ParsedMigrationScript> res = new ArrayList<>(orderedScripts);
        for (int i = 0; i < history.size(); i++) {
            // do some checks
            MigrationScriptProtocol protocol = history.get(i);
            if (orderedScripts.size() <= i) {
                logger.warn(String.format("there are less migration scripts than already executed history entries! " +
                        "You should never delete migration scripts you have already executed. " +
                        "Or maybe you have to cleanup the Elasticsearch-Evolution history index manually! " +
                        "history version at position %s is %s", i, protocol.getVersion()));
                break;
            }
            ParsedMigrationScript parsedMigrationScript = orderedScripts.get(i);
            if (!protocol.getVersion().equals(parsedMigrationScript.getFileNameInfo().getVersion())) {
                throw new MigrationException(String.format(
                        "The logged execution in the Elasticsearch-Evolution history index at position %s " +
                                "is version %s and in the same position in the given migration scripts is version %s! " +
                                "Out of order execution is not supported. Or maybe you have added new migration scripts " +
                                "in between or have to cleanup the Elasticsearch-Evolution history index manually",
                        i, protocol.getVersion(), parsedMigrationScript.getFileNameInfo().getVersion()));
            }
            // failed scripts can be edited and retried, but successfully executed scripts may not be modified afterwards
            if (validateOnMigrate && protocol.isSuccess() && protocol.getChecksum() != parsedMigrationScript.getChecksum()) {
                throw new MigrationException(String.format(
                        "The logged execution for the migration script at position %s (%s) " +
                                "has a different checksum from the given migration script! " +
                                "Modifying already-executed scripts is not supported.",
                        i, protocol.getScriptName()));
            }

            if (protocol.isSuccess()) {
                res.remove(parsedMigrationScript);
            }
        }

        return res;
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

    static class ExecutionResult {
        private final MigrationScriptProtocol protocol;
        private final Optional<RuntimeException> error;

        private ExecutionResult(MigrationScriptProtocol protocol, Optional<RuntimeException> error) {
            this.protocol = protocol;
            this.error = error;
        }

        public MigrationScriptProtocol getProtocol() {
            return protocol;
        }

        public Optional<RuntimeException> getError() {
            return error;
        }
    }
}
