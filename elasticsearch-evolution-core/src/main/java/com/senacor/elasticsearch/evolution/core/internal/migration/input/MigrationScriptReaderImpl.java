package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationScriptReader;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Andreas Keefer
 */

public class MigrationScriptReaderImpl implements MigrationScriptReader {

    private static final Logger logger = LoggerFactory.getLogger(MigrationScriptReaderImpl.class);

    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String FILE_PREFIX = "file:";

    private final List<String> locations;
    private final Charset encoding;
    private final String esMigrationPrefix;
    private final List<String> esMigrationSuffixes;
    private final String lineSeparator;

    /**
     * @param locations               Locations of migrations scripts, e.g classpath:es/migration or file:/home/migration
     * @param encoding                migrations scripts encoding
     * @param esMigrationFilePrefix   File name prefix for ES migrations.
     * @param esMigrationFileSuffixes File name suffix for ES migrations.
     * @param lineSeparator           Line separator. should be '\n' per default and only something else for backward compatibility / hash stability
     */

    public MigrationScriptReaderImpl(List<String> locations,
                                     Charset encoding,
                                     String esMigrationFilePrefix,
                                     List<String> esMigrationFileSuffixes,
                                     String lineSeparator) {
        this.locations = locations;
        this.encoding = encoding;
        this.esMigrationPrefix = esMigrationFilePrefix;
        this.esMigrationSuffixes = esMigrationFileSuffixes;
        this.lineSeparator = lineSeparator;
    }

    /**
     * Reads all migration scripts from the specified locations that match prefixes and suffixes
     *
     * @return a list of {@link RawMigrationScript}
     */
    public List<RawMigrationScript> read() {
        return this.locations.stream()
                .flatMap(location -> {
                    try {
                        return readFromLocation(location);
                    } catch (URISyntaxException | IOException e) {
                        throw new MigrationException(
                                "couldn't read scripts from %s".formatted(location), e);
                    }
                })
                .distinct()
                .toList();
    }

    /**
     * Reads migration scripts from a specific location
     *
     * @param location path where to look for migration scripts
     * @return a list of {@link RawMigrationScript}
     * @throws URISyntaxException if the location is not formatted strictly according to RFC2396 and cannot be converted to a URI.
     * @throws IOException        if an I/O error is thrown when accessing the files at the location(s).
     */
    protected Stream<RawMigrationScript> readFromLocation(String location) throws URISyntaxException, IOException {
        if (location.startsWith(CLASSPATH_PREFIX)) {
            return readScriptsFromClassPath(location);

        } else if (location.startsWith(FILE_PREFIX)) {
            return readScriptsFromFilesystem(location);
        } else {
            throw new MigrationException(("""
                    could not read location path %s, \
                    should look like this: %ses/migration or this: %s/home/scripts/migration\
                    """).formatted(
                    location, CLASSPATH_PREFIX, FILE_PREFIX));
        }
    }

    private Stream<RawMigrationScript> readScriptsFromFilesystem(String location) throws IOException {
        String locationWithoutPrefix = location.substring(FILE_PREFIX.length());
        URI uri = Paths.get(locationWithoutPrefix).toUri();
        logger.debug("URI of location '{}' = '{}'", location, uri);
        if (uri == null) {
            return Stream.empty();
        }
        Path path = Paths.get(uri);
        return Files.find(path, 10, (pathToCheck, basicFileAttributes) ->
                !basicFileAttributes.isDirectory()
                        && basicFileAttributes.size() > 0
                        && isValidFilename(pathToCheck.getFileName().toString()))
                .flatMap(file -> {
                    logger.debug("reading migration script '{}' from filesystem...", file);
                    String filename = file.getFileName().toString();
                    try (BufferedReader reader = Files.newBufferedReader(file, this.encoding)) {
                        return read(reader, filename);
                    } catch (IOException e) {
                        throw new MigrationException("can't read script from filesystem: " + file.getFileName(), e);
                    }
                });
    }

    private Stream<RawMigrationScript> readScriptsFromClassPath(String location) {
        if (!location.endsWith("/")) {
            // fixes https://github.com/senacor/elasticsearch-evolution/issues/36
            // otherwise e.g. "...location_some_suffix" will also be found when search for "...location".
            location = location + "/";
        }

        final String locationWithoutPrefix = location.substring(CLASSPATH_PREFIX.length());

        List<RawMigrationScript> res = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph()
                .acceptPaths(locationWithoutPrefix)
                .scan()) {
            scanResult.getAllResources()
                    .filter(resource -> isValidFilename(Paths.get(resource.getPath()).getFileName().toString()))
                    .forEach(resource -> {
                        logger.debug("reading migration script '{}' from classpath...", resource);
                        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(resource.load()), encoding))) {
                            Path p = Paths.get(resource.getPath());
                            res.addAll(read(bufferedReader, p.getFileName().toString()).toList());
                        } catch (IOException e) {
                            throw new MigrationException("can't read script from classpath: " + resource, e);
                        }
                    });
        }
        return res.stream();
    }

    Stream<RawMigrationScript> read(BufferedReader reader, String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            sb.append((char) ch);
        }
        // use static line separator ('\n' per default) to get predictable and system independent checksum later
        String content = sb.toString().replaceAll("\\R", lineSeparator);

        if (content.isEmpty()) {
            return Stream.empty();
        }
        return Stream.of(new RawMigrationScript().setFileName(filename).setContent(content));
    }

    private boolean hasValidSuffix(String path) {
        return this.esMigrationSuffixes
                .stream()
                .anyMatch(suffix -> path.toLowerCase().endsWith(suffix.toLowerCase()));
    }

    private boolean isValidFilename(String fileName) {
        return hasValidSuffix(fileName)
                && fileName.startsWith(this.esMigrationPrefix);
    }
}
