package com.senacor.elasticsearch.evolution.core.internal.model.migration;

import java.util.*;

public class MigrationScriptExecuteOptions {

    /**
     * script file name without any packages/directories.
     * Should match an existing migration script name
     */
    private String fileName = "";

    /**
     * List of valid options:
     */

    /**
     * Option : ignore Http status codes. When set, those specified codes are always treated as success.
     * Example: "404"
     */
    private List<Integer> ignoredHttpStatusCodes = Collections.emptyList();

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<Integer> getIgnoredHttpStatusCodes() {
        return ignoredHttpStatusCodes;
    }

    public void setIgnoredHttpStatusCodes(Collection<Integer> ignoredHttpStatusCodes) {
        this.ignoredHttpStatusCodes = new ArrayList<>(ignoredHttpStatusCodes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MigrationScriptExecuteOptions that = (MigrationScriptExecuteOptions) o;
        return fileName.equals(that.fileName) && ignoredHttpStatusCodes.equals(that.ignoredHttpStatusCodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, ignoredHttpStatusCodes);
    }

    @Override
    public String toString() {
        return "MigrationScriptExecuteOptions{" +
                "fileName='" + fileName + '\'' +
                ", ignoredHttpStatusCodes=" + ignoredHttpStatusCodes +
                '}';
    }
}
