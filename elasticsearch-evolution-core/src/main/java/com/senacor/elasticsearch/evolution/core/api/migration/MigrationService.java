package com.senacor.elasticsearch.evolution.core.api.migration;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigration;
import lombok.NonNull;

import java.util.Collection;
import java.util.List;

/**
 * @author Andreas Keefer
 */
public interface MigrationService {
    /**
     * Executes all Migration Scripts after the last succeeded reported History version.
     * The already executed scrips will not be executed again. Therefor after the execution the scripts
     * will be logged in an elasticsearch index where Elasticsearch-Evolution keeps its state.
     *
     * @param migrationScripts all parsed migration scripts which should be executed.
     * @return executed Scripts
     * @throws MigrationException if execution failed
     */
    @NonNull
    List<MigrationScriptProtocol> executePendingScripts(@NonNull Collection<ParsedMigration<?>> migrationScripts) throws MigrationException;

    /**
     * This method returns only those scripts, which must be executed.
     * Already executed scripts will be filtered out.
     * The returned scripts must be executed in the returned order.
     *
     * @param migrationScripts all migration scripts that were potentially executed earlier.
     * @return list of ordered scripts which must be executed
     * @throws MigrationException if validateOnMigrate failed
     */
    @NonNull
    List<ParsedMigration<?>> getPendingScriptsToBeExecuted(@NonNull Collection<ParsedMigration<?>> migrationScripts) throws MigrationException;
}
