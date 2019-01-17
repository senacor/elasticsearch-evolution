package com.senacor.elasticsearch.evolution.core.internal.model.dbhistory;

import java.time.ZonedDateTime;

/**
 * Represents a Script execution in the database.
 *
 * @author Andreas Keefer
 */
public class MigrationScriptProtocol {

    /**
     * not-null
     */
    private String version;

    /**
     * The index this scrip was applied to
     * nullable: in case this migration was not applied for an index pattern lookup
     */
    private String indexName;

    /**
     * migration description
     * not-null
     */
    private String description;

    /**
     * The name of the script to execute for this migration, relative to the configured location.
     * not-null
     */
    private String scriptName;

    /**
     * The checksum of the raw migration script.
     * not-null
     */
    private int checksum;

    /**
     * The timestamp when this migration was applied/executed.
     * not-null
     */
    private ZonedDateTime executionTimestamp;

    /**
     * The the runtime in millis of this migration.
     * not-null
     */
    private int executionRuntimeInMillis;

    /**
     * Flag indicating whether the migration was successful or not.
     * not-null
     */
    private boolean success;

    /**
     * Default Constructor
     */
    public MigrationScriptProtocol() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public ZonedDateTime getExecutionTimestamp() {
        return executionTimestamp;
    }

    public void setExecutionTimestamp(ZonedDateTime executionTimestamp) {
        this.executionTimestamp = executionTimestamp;
    }

    public int getExecutionRuntimeInMillis() {
        return executionRuntimeInMillis;
    }

    public void setExecutionRuntimeInMillis(int executionRuntimeInMillis) {
        this.executionRuntimeInMillis = executionRuntimeInMillis;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
