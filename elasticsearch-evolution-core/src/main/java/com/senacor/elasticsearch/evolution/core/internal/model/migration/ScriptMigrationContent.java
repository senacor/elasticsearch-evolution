package com.senacor.elasticsearch.evolution.core.internal.model.migration;

/**
 * @param content raw content of the migration file
 */
public record ScriptMigrationContent(String content) implements MigrationContent {
    @Override
    public int checksum() {
        return content.hashCode();
    }
}
