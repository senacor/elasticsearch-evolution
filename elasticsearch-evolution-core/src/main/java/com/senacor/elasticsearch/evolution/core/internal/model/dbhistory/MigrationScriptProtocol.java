package com.senacor.elasticsearch.evolution.core.internal.model.dbhistory;

import com.senacor.elasticsearch.evolution.core.internal.model.FileNameInfo;
import com.senacor.elasticsearch.evolution.core.internal.model.MigrationVersion;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Represents a Script execution in the database.
 *
 * @author Andreas Keefer
 */
public class MigrationScriptProtocol implements FileNameInfo, Comparable<MigrationScriptProtocol> {

    /**
     * not-null
     */
    private MigrationVersion version;

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
    private OffsetDateTime executionTimestamp;

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
     * a flag to implement a "index lock". If a document in the index is locked, the application will not continue.
     * not-null
     */
    private boolean locked = true;

    /**
     * Default Constructor
     */
    public MigrationScriptProtocol() {
    }

    public MigrationScriptProtocol setVersion(String version) {
        this.version = MigrationVersion.fromVersion(version);
        return this;
    }

    public String getVersionAsString() {
        return version.getVersion();
    }

    @Override
    public MigrationVersion getVersion() {
        return version;
    }

    public MigrationScriptProtocol setVersion(MigrationVersion version) {
        this.version = version;
        return this;
    }

    public String getIndexName() {
        return indexName;
    }

    public MigrationScriptProtocol setIndexName(String indexName) {
        this.indexName = indexName;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public MigrationScriptProtocol setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String getScriptName() {
        return scriptName;
    }

    public MigrationScriptProtocol setScriptName(String scriptName) {
        this.scriptName = scriptName;
        return this;
    }

    public int getChecksum() {
        return checksum;
    }

    public MigrationScriptProtocol setChecksum(int checksum) {
        this.checksum = checksum;
        return this;
    }

    public OffsetDateTime getExecutionTimestamp() {
        return executionTimestamp;
    }

    public MigrationScriptProtocol setExecutionTimestamp(OffsetDateTime executionTimestamp) {
        this.executionTimestamp = executionTimestamp;
        return this;
    }

    public int getExecutionRuntimeInMillis() {
        return executionRuntimeInMillis;
    }

    public MigrationScriptProtocol setExecutionRuntimeInMillis(int executionRuntimeInMillis) {
        this.executionRuntimeInMillis = executionRuntimeInMillis;
        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public MigrationScriptProtocol setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public boolean isLocked() {
        return locked;
    }

    public MigrationScriptProtocol setLocked(boolean locked) {
        this.locked = locked;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final MigrationScriptProtocol other = (MigrationScriptProtocol) obj;
        return Objects.equals(this.version, other.version);
    }

    @Override
    public int compareTo(MigrationScriptProtocol o) {
        if (o == null) {
            return 1;
        }
        return version.compareTo(o.version);
    }

    @Override
    public String toString() {
        return "MigrationScriptProtocol{" +
                "version=" + version +
                ", indexName='" + indexName + '\'' +
                ", description='" + description + '\'' +
                ", scriptName='" + scriptName + '\'' +
                ", checksum=" + checksum +
                ", executionTimestamp=" + executionTimestamp +
                ", executionRuntimeInMillis=" + executionRuntimeInMillis +
                ", success=" + success +
                ", locked=" + locked +
                '}';
    }
}
