package com.senacor.elasticsearch.evolution.core.internal.model.migration;

public sealed interface MigrationRequest permits MigrationScriptRequest, JavaMigrationRequestContent {
}
