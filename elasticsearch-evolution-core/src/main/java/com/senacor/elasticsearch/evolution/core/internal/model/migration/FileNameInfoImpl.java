package com.senacor.elasticsearch.evolution.core.internal.model.migration;

import com.senacor.elasticsearch.evolution.core.internal.model.FileNameInfo;
import com.senacor.elasticsearch.evolution.core.internal.model.MigrationVersion;

import java.util.Objects;

import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.requireNotEmpty;
import static java.util.Objects.requireNonNull;

/**
 * @author Andreas Keefer
 */
public class FileNameInfoImpl implements FileNameInfo {

    /**
     * not-null
     */
    private final MigrationVersion version;

    /**
     * migration description
     * not-null
     */
    private final String description;

    /**
     * The name of the script to execute for this migration, relative to the configured location.
     * not-null
     */
    private final String scriptName;

    public FileNameInfoImpl(MigrationVersion version, String description, String scriptName) {
        this.version = requireNonNull(version, "version must not be null");
        this.description = requireNotEmpty(description, "description must not be empty");
        this.scriptName = requireNotEmpty(scriptName, "scriptName must not be empty");
    }

    public MigrationVersion getVersion() {
        return version;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getScriptName() {
        return scriptName;
    }

    @Override
    public String toString() {
        return "FileNameInfoImpl{" +
                "version=" + version +
                ", description='" + description + '\'' +
                ", scriptName='" + scriptName + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, description, scriptName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final FileNameInfoImpl other = (FileNameInfoImpl) obj;
        return Objects.equals(this.version, other.version)
                && Objects.equals(this.description, other.description)
                && Objects.equals(this.scriptName, other.scriptName);
    }
}
