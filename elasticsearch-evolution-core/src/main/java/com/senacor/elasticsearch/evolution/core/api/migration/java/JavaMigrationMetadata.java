package com.senacor.elasticsearch.evolution.core.api.migration.java;

import com.senacor.elasticsearch.evolution.core.api.migration.MigrationVersion;
import lombok.NonNull;

public record JavaMigrationMetadata(@NonNull MigrationVersion version, @NonNull String description) {
}
