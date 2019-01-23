package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationService;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigrationScript;
import com.senacor.elasticsearch.evolution.core.internal.utils.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
        if (!migrationScripts.isEmpty()) {
            try {
                waitUntilUnlocked();
                // set an logical index lock
                if (!historyRepository.lock()) {
                    throw new MigrationException("could not lock the elasticsearch-evolution history index");
                }

                // get scripts which needs to be executed
                List<ParsedMigrationScript> scriptsToExecute = getPendingScriptsToBeExecuted(migrationScripts);

                // execute scripts
                List<MigrationScriptProtocol> protocols = executeScriptsInOrder(scriptsToExecute);

                // write protocols to history index
                historyRepository.saveOrUpdate(protocols);
            } finally {
                // release logical index lock
                historyRepository.unlock();
            }
        }
        return Collections.emptyList();
    }

    private List<MigrationScriptProtocol> executeScriptsInOrder(List<ParsedMigrationScript> scriptsToExecute) {
        // TODO (ak) impl
        return Collections.emptyList();
    }

    List<ParsedMigrationScript> getPendingScriptsToBeExecuted(Collection<ParsedMigrationScript> migrationScripts) {
        // TODO (ak) impl
        return Collections.emptyList();
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
