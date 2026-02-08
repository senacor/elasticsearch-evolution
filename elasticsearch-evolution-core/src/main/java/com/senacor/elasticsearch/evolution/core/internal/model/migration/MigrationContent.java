package com.senacor.elasticsearch.evolution.core.internal.model.migration;

public sealed interface MigrationContent permits JavaMigrationRequestContent, ScriptMigrationContent {
    /**
     * @return the checksum of the content
     */
    int checksum();
}
