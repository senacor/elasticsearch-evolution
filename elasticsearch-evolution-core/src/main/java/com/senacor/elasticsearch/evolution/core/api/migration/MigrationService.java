package com.senacor.elasticsearch.evolution.core.api.migration;

import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigrationScript;

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
     */
    List<MigrationScriptProtocol> executePendingScripts(Collection<ParsedMigrationScript> migrationScripts);
}
