package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.internal.model.migration.MigrationScriptExecuteOptions;

import java.util.List;

public class MigrationScriptExecuteOptionsConfiguration {
    private List<MigrationScriptExecuteOptions> migrationScriptsOptions;

    public List<MigrationScriptExecuteOptions> getMigrationScriptsOptions() {
        return migrationScriptsOptions;
    }

    public void setMigrationScriptsOptions(List<MigrationScriptExecuteOptions> migrationScriptsOptions) {
        this.migrationScriptsOptions = migrationScriptsOptions;
    }
}
