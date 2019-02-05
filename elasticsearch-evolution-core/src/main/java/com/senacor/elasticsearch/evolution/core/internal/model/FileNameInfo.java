package com.senacor.elasticsearch.evolution.core.internal.model;

/**
 * @author Andreas Keefer
 */
public interface FileNameInfo {

    /**
     * @return non-null
     */
    MigrationVersion getVersion();

    /**
     * @return migration description, not-null
     */
    String getDescription();

    /**
     * @return The name of the script to execute for this migration, relative to the configured location, not-null
     */
    String getScriptName();
}
