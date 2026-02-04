package com.senacor.elasticsearch.evolution.core.internal.model.migration;

import com.senacor.elasticsearch.evolution.core.internal.model.FileNameInfo;
import com.senacor.elasticsearch.evolution.core.internal.model.MigrationVersion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.requireNotEmpty;
import static java.util.Objects.requireNonNull;

/**
 * @author Andreas Keefer
 */
@ToString
@EqualsAndHashCode
public class FileNameInfoImpl implements FileNameInfo {

    /**
     * not-null
     */
    private final MigrationVersion version;

    /**
     * migration description
     * not-null
     */
    @Getter(onMethod_ = {@Override})
    private final String description;

    /**
     * The name of the script to execute for this migration, relative to the configured location.
     * not-null
     */
    @Getter(onMethod_ = @Override)
    private final String scriptName;

    public FileNameInfoImpl(MigrationVersion version, String description, String scriptName) {
        this.version = requireNonNull(version, "version must not be null");
        this.description = requireNotEmpty(description, "description must not be empty");
        this.scriptName = requireNotEmpty(scriptName, "scriptName must not be empty");
    }

    @Override
    public MigrationVersion getVersion() {
        return version;
    }
}
