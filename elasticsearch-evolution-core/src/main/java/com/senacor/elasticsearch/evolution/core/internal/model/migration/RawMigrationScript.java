package com.senacor.elasticsearch.evolution.core.internal.model.migration;

/**
 * Represents a Migration Script
 *
 * @author Andreas Keefer
 */
public class RawMigrationScript {

    /**
     * script file name
     */
    private String fileName;

    /**
     * raw content of the migration file
     */
    private String content;

    public String getFileName() {
        return fileName;
    }

    public RawMigrationScript setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getContent() {
        return content;
    }

    public RawMigrationScript setContent(String content) {
        this.content = content;
        return this;
    }

    public String toString() {
        return "Filename: " + fileName + ", content: " + content;
    }
}
