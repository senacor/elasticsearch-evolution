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
public class ParsedMigrationScript {

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
     * Represents the HTTP request from the migration script
     * non-null
     */
    @Getter
    private MigrationScriptRequest migrationScriptRequest;

    public ParsedMigrationScript setFileNameInfo(FileNameInfo fileNameInfo) {
        this.fileNameInfo = fileNameInfo;
        return this;
    }

    public ParsedMigrationScript setChecksum(int checksum) {
        this.checksum = checksum;
        return this;
    }

    public ParsedMigrationScript setMigrationScriptRequest(MigrationScriptRequest migrationScriptRequest) {
        this.migrationScriptRequest = migrationScriptRequest;
        return this;
    }
}
