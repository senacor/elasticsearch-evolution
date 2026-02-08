package com.senacor.elasticsearch.evolution.core.internal.model.migration;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Represents a Migration Script
 *
 * @author Andreas Keefer
 */
@EqualsAndHashCode
public class RawMigrationScript<T extends MigrationContent> {

    /**
     * script file name without any packages/directories
     */
    @Getter
    private String fileName;

    /**
     * raw content of the migration
     */
    @Getter
    private T content;

    public RawMigrationScript<T> setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public RawMigrationScript<T> setContent(T content) {
        this.content = content;
        return this;
    }

    @Override
    public String toString() {
        return "filename: " + fileName + ", content: " + content;
    }
}
