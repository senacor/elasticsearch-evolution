package com.senacor.elasticsearch.evolution.core.internal.model.migration;

import com.senacor.elasticsearch.evolution.core.internal.model.FileNameInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Parsed MigrationScript
 *
 * @author Andreas Keefer
 */
@ToString
@EqualsAndHashCode
public class ParsedMigration<T extends MigrationRequest> {

    /**
     * information about the filename of the migration script
     * non-null
     */
    @Getter
    private FileNameInfo fileNameInfo;

    /**
     * the checksum of the raw migration script
     * non-null
     */
    @Getter
    private int checksum;

    /**
     * Represents the HTTP request from the migration script or a Java Migration
     * non-null
     */
    @Getter
    private T migrationRequest;

    public ParsedMigration<T> setFileNameInfo(FileNameInfo fileNameInfo) {
        this.fileNameInfo = fileNameInfo;
        return this;
    }

    public ParsedMigration<T> setChecksum(int checksum) {
        this.checksum = checksum;
        return this;
    }

    public ParsedMigration<T> setMigrationRequest(T migrationScriptRequest) {
        this.migrationRequest = migrationScriptRequest;
        return this;
    }
}
