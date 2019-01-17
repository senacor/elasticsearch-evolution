package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        List<RawMigrationScript> scriptList = new ArrayList<RawMigrationScript>();
        for (String location : this.locations) {
            for (String suffix : this.esMigrationSuffixes) {
                try {
                    Files.walk(Paths.get(location))
                            .filter(path -> {
                                String fileName = path.getFileName().toString();
                                return fileName.startsWith(this.esMigrationPrefix) && fileName.endsWith(suffix);
                            })
                            .map(this::readFile)
                            .forEach(scriptList::add);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return scriptList;
    }

    private RawMigrationScript readFile(Path path) {
        try (Stream<String> stream = Files.lines(path, this.encoding)) {

            String content = stream.collect(Collectors.joining("\n"));

            return new RawMigrationScript().setContent(content).setFileName(path.getFileName().toString());

        } catch (IOException e) {
            e.printStackTrace();
            //should probably handle this better
            return new RawMigrationScript();
        }
    }
}
