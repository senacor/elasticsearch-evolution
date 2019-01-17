package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 * @author Andreas Keefer
 */
public class MigrationScriptReader {

    private final List<String> locations;
    private final Charset encoding;
    private final String esMigrationPrefix;
    private final List<String> esMigrationSuffixes;

    /**
     * @param locations               Locations of migrations scripts.
     * @param encoding                migrations scripts encoding
     * @param esMigrationFilePrefix   File name prefix for ES migrations.
     * @param esMigrationFileSuffixes File name suffix for ES migrations.
     */
    public MigrationScriptReader(List<String> locations,
                                 Charset encoding,
                                 String esMigrationFilePrefix,
                                 List<String> esMigrationFileSuffixes) {
        this.locations = locations;
        this.encoding = encoding;
        this.esMigrationPrefix = esMigrationFilePrefix;
        this.esMigrationSuffixes = esMigrationFileSuffixes;
    }

    /**
     * Reads all migration scrips to {@link RawMigrationScript}'s objects.
     *
     * @return List of {@link RawMigrationScript}'s
     */
    public List<RawMigrationScript> read() {
        // TODO (ak) impl
        return Collections.emptyList();
    }
}
