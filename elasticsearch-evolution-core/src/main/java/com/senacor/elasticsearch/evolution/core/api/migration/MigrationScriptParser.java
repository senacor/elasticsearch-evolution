package com.senacor.elasticsearch.evolution.core.api.migration;

import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigrationScript;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;

import java.util.Collection;

/**
 * @author Andreas Keefer
 */
public interface MigrationScriptParser {
    /**
     * parses all migration scripts
     *
     * @param rawMigrationScripts the migration scripts to parse
     * @return List of {@link RawMigrationScript}'s
     */
    Collection<ParsedMigrationScript> parse(Collection<RawMigrationScript> rawMigrationScripts);
}
