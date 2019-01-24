package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationService;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigrationScript;
import com.senacor.elasticsearch.evolution.core.internal.utils.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.requireCondition;

/**
 * @author Andreas Keefer
 */
public class MigrationServiceImpl implements MigrationService {

    private static final Logger logger = LoggerFactory.getLogger(MigrationServiceImpl.class);

    private final HistoryRepository historyRepository;
    private final int waitUntilUnlockedMinTimeInMillis;
    private final int waitUntilUnlockedMaxTimeInMillis;

    public MigrationServiceImpl(HistoryRepository historyRepository, int waitUntilUnlockedMinTimeInMillis, int waitUntilUnlockedMaxTimeInMillis) {
        this.historyRepository = historyRepository;
        this.waitUntilUnlockedMinTimeInMillis = requireCondition(waitUntilUnlockedMinTimeInMillis,
                min -> min >= 0 && min <= waitUntilUnlockedMaxTimeInMillis,
                "waitUntilUnlockedMinTimeInMillis (%s) must not be negative and must not be greater than waitUntilUnlockedMaxTimeInMillis (%s)",
                waitUntilUnlockedMinTimeInMillis, waitUntilUnlockedMaxTimeInMillis);
        this.waitUntilUnlockedMaxTimeInMillis = waitUntilUnlockedMaxTimeInMillis;
    }

    @Override
    public List<MigrationScriptProtocol> executePendingScripts(Collection<ParsedMigrationScript> migrationScripts) {
        List<MigrationScriptProtocol> executedScripts = new ArrayList<>();
        if (!migrationScripts.isEmpty()) {
            try {
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
                    MigrationScriptProtocol protocol = executeScript(script);
                    executedScripts.add(protocol);
                    // write protocols to history index
                    historyRepository.saveOrUpdate(protocol);
                    if (!protocol.isSuccess()) {
                        break;
                    }
                }
            } finally {
                // release logical index lock
                historyRepository.unlock();
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
    MigrationScriptProtocol executeScript(ParsedMigrationScript scriptToExecute) {
        // TODO (ak) impl
        return new MigrationScriptProtocol()
                // TODO (ak) impl
//                .setExecutionRuntimeInMillis()
//                .setSuccess()
                .setVersion(scriptToExecute.getFileNameInfo().getVersion())
                .setScriptName(scriptToExecute.getFileNameInfo().getScriptName())
                .setDescription(scriptToExecute.getFileNameInfo().getDescription())
                .setChecksum(scriptToExecute.getChecksum())
                .setExecutionTimestamp(ZonedDateTime.now())
                .setLocked(true);
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
                        "The logged execution in the Elasticsearch-Evolution history index at position %s is version %s and in the same position in the given migration scripts is version %s! Out of order execution is not supported. Or maybe you have added new migration scripts in between or have to cleanup the Elasticsearch-Evolution history index manually",
                        i, protocol.getVersion(), parsedMigrationScript.getFileNameInfo().getVersion()));
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
}
