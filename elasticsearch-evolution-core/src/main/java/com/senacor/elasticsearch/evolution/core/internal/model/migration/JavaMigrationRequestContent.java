package com.senacor.elasticsearch.evolution.core.internal.model.migration;

import com.senacor.elasticsearch.evolution.core.api.migration.java.JavaMigration;
import lombok.NonNull;

public record JavaMigrationRequestContent(@NonNull JavaMigration javaMigration) implements MigrationRequest, MigrationContent {
    @Override
    public int checksum() {
        return javaMigration.getChecksum();
    }
}
