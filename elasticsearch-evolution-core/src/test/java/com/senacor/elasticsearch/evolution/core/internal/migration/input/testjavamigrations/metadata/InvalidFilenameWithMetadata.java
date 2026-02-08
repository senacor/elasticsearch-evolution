package com.senacor.elasticsearch.evolution.core.internal.migration.input.testjavamigrations.metadata;

import com.senacor.elasticsearch.evolution.core.api.migration.MigrationVersion;
import com.senacor.elasticsearch.evolution.core.api.migration.java.Context;
import com.senacor.elasticsearch.evolution.core.api.migration.java.JavaMigration;
import com.senacor.elasticsearch.evolution.core.api.migration.java.JavaMigrationMetadata;

public class InvalidFilenameWithMetadata implements JavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        // do nothing
    }

    @Override
    public JavaMigrationMetadata getMetadata() {
        return new JavaMigrationMetadata(
                MigrationVersion.fromVersion("1.3"),
                "Invalid Filename With Metadata");
    }

    @Override
    public int getChecksum() {
        return 42;
    }
}
