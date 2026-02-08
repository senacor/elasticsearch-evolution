package com.senacor.elasticsearch.evolution.core.internal.model.dbhistory;

import com.senacor.elasticsearch.evolution.core.api.migration.MigrationVersion;
import com.senacor.elasticsearch.evolution.core.internal.model.FileNameInfo;
import lombok.Getter;
import lombok.NonNull;

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
    @Getter
    private MigrationVersion version;

    /**
     * The index this scrip was applied to
     * nullable: in case this migration was not applied for an index pattern lookup
     */
    @Getter
    private String indexName;

    /**
     * migration description
     * not-null
     */
    @Getter
    private String description;

    /**
     * The name of the script to execute for this migration, relative to the configured location.
     * not-null
     */
    @Getter
    private String scriptName;

    /**
     * The checksum of the raw migration script.
     * not-null
     */
    @Getter
    private int checksum;

    /**
     * The timestamp when this migration was applied/executed.
     * not-null
     */
    @Getter
    private OffsetDateTime executionTimestamp;

    /**
     * The runtime in millis of this migration.
     * not-null
     */
    @Getter
    private int executionRuntimeInMillis;

    /**
     * Flag indicating whether the migration was successful or not.
     * not-null
     */
    @Getter
    private boolean success;

    /**
     * a flag to implement a "index lock". If a document in the index is locked, the application will not continue.
     * not-null
     */
    @Getter
    private boolean locked = true;

    public MigrationScriptProtocol setVersion(String version) {
        this.version = MigrationVersion.fromVersion(version);
        return this;
    }

    public MigrationScriptProtocol setVersion(@NonNull MigrationVersion version) {
        this.version = version;
        return this;
    }

    public MigrationScriptProtocol setIndexName(String indexName) {
        this.indexName = indexName;
        return this;
    }

    public MigrationScriptProtocol setDescription(@NonNull String description) {
        this.description = description;
        return this;
    }

    public MigrationScriptProtocol setScriptName(@NonNull String scriptName) {
        this.scriptName = scriptName;
        return this;
    }

    public MigrationScriptProtocol setChecksum(int checksum) {
        this.checksum = checksum;
        return this;
    }

    public MigrationScriptProtocol setExecutionTimestamp(@NonNull OffsetDateTime executionTimestamp) {
        this.executionTimestamp = executionTimestamp;
        return this;
    }

    public MigrationScriptProtocol setExecutionRuntimeInMillis(int executionRuntimeInMillis) {
        this.executionRuntimeInMillis = executionRuntimeInMillis;
        return this;
    }

    public MigrationScriptProtocol setSuccess(boolean success) {
        this.success = success;
        return this;
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
