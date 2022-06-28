package com.senacor.elasticsearch.evolution.core.api.migration;

import com.senacor.elasticsearch.evolution.core.internal.model.migration.MigrationScriptExecuteOptions;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigrationScript;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;

import java.util.Collection;
import java.util.Map;

/**
 * @author Andreas Keefer
 */
public interface MigrationScriptOptionsParser {
    /**
     * parses all migration scripts
     *
     * @param rawMigrationScripts the migration scripts to parse
     * @return a map of migrationScriptFilename -> {@link MigrationScriptExecuteOptions}
     */
    Map<String,MigrationScriptExecuteOptions> parse(Collection<RawMigrationScript> rawMigrationScripts);
}
