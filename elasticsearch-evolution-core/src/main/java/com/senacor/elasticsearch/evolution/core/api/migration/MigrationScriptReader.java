package com.senacor.elasticsearch.evolution.core.api.migration;

import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;

import java.util.List;

/**
 * @author Andreas Keefer
 */
public interface MigrationScriptReader {
    /**
     * Reads all migration scrips to {@link RawMigrationScript}'s objects.
     *
     * @return List of {@link RawMigrationScript}'s
     */
    List<RawMigrationScript> read();
}
