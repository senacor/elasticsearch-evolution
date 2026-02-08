package com.senacor.elasticsearch.evolution.core.internal.model.migration;

import com.senacor.elasticsearch.evolution.core.api.migration.MigrationVersion;
import com.senacor.elasticsearch.evolution.core.internal.model.FileNameInfo;
import lombok.NonNull;
import lombok.Value;

import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.requireNotEmpty;

/**
 * @author Andreas Keefer
 */
@Value
public class FileNameInfoImpl implements FileNameInfo {

    @NonNull
    MigrationVersion version;

    /**
     * migration description
     */
    @NonNull
    String description;

    /**
     * The name of the script to execute for this migration, relative to the configured location.
     */
    @NonNull
    String scriptName;

    public FileNameInfoImpl(@NonNull MigrationVersion version,
                            String description,
                            String scriptName) {
        this.version = version;
        this.description = requireNotEmpty(description, "description must not be empty");
        this.scriptName = requireNotEmpty(scriptName, "scriptName must not be empty");
    }
}
