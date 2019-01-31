package com.senacor.elasticsearch.evolution.core.internal.model.migration;

import java.util.Objects;

/**
 * Represents a Migration Script
 *
 * @author Andreas Keefer
 */
public class RawMigrationScript {

    /**
     * script file name without any packages/directories
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
        return "filename: " + fileName + ", content: " + content;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawMigrationScript script = (RawMigrationScript) o;
        return fileName.equals(script.getFileName()) &&
                content.equals(script.getContent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileName(), getContent());
    }

}
