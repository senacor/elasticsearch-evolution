package com.senacor.elasticsearch.evolution.core.internal.model.migration;

import com.senacor.elasticsearch.evolution.core.internal.model.FileNameInfo;

import java.util.Objects;

/**
 * Parsed MigrationScript
 *
 * @author Andreas Keefer
 */
public class ParsedMigrationScript {

    /**
     * information about the filename of the migration script
     * non-null
     */
    private FileNameInfo fileNameInfo;

    /**
     * the checksum of the raw migration script
     * non-null
     */
    private int checksum;

    /**
     * Represents the HTTP request from the migration script
     * non-null
     */
    private MigrationScriptRequest migrationScriptRequest;

    public FileNameInfo getFileNameInfo() {
        return fileNameInfo;
    }

    public ParsedMigrationScript setFileNameInfo(FileNameInfo fileNameInfo) {
        this.fileNameInfo = fileNameInfo;
        return this;
    }

    public int getChecksum() {
        return checksum;
    }

    public ParsedMigrationScript setChecksum(int checksum) {
        this.checksum = checksum;
        return this;
    }

    public MigrationScriptRequest getMigrationScriptRequest() {
        return migrationScriptRequest;
    }

    public ParsedMigrationScript setMigrationScriptRequest(MigrationScriptRequest migrationScriptRequest) {
        this.migrationScriptRequest = migrationScriptRequest;
        return this;
    }

    @Override
    public String toString() {
        return "ParsedMigrationScript{" +
                "fileNameInfo=" + fileNameInfo +
                ", checksum=" + checksum +
                ", migrationScriptRequest=" + migrationScriptRequest +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileNameInfo, checksum, migrationScriptRequest);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ParsedMigrationScript other = (ParsedMigrationScript) obj;
        return Objects.equals(this.fileNameInfo, other.fileNameInfo)
                && Objects.equals(this.checksum, other.checksum)
                && Objects.equals(this.migrationScriptRequest, other.migrationScriptRequest);
    }
}
