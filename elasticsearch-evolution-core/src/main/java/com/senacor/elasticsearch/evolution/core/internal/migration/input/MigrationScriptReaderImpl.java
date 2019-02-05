package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationScriptReader;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andreas Keefer
 */

public class MigrationScriptReaderImpl implements MigrationScriptReader {

    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String FILE_PREFIX = "file:";

    private final List<String> locations;
    private final Charset encoding;
    private final String esMigrationPrefix;
    private final List<String> esMigrationSuffixes;

    /**
     * @param locations               Locations of migrations scripts, e.g classpath:es/migration or file:/home/migration
     * @param encoding                migrations scripts encoding
     * @param esMigrationFilePrefix   File name prefix for ES migrations.
     * @param esMigrationFileSuffixes File name suffix for ES migrations.
     */

    public MigrationScriptReaderImpl(List<String> locations,
                                     Charset encoding,
                                     String esMigrationFilePrefix,
                                     List<String> esMigrationFileSuffixes) {
        this.locations = locations;
        this.encoding = encoding;
        this.esMigrationPrefix = esMigrationFilePrefix;
        this.esMigrationSuffixes = esMigrationFileSuffixes;
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
                        return readFromLocation(location).stream();
                    } catch (URISyntaxException | IOException e) {
                        throw new MigrationException(
                                String.format("couldn't read scripts from %s", location), e);
                    }
                })
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Reads migration scripts from a specific location
     *
     * @param location path where to look for migration scripts
     * @return a list of {@link RawMigrationScript}
     * @throws URISyntaxException if the location is not formatted strictly according to RFC2396 and cannot be converted to a URI.
     * @throws IOException        if an I/O error is thrown when accessing the files at the location(s).
     */
    protected List<RawMigrationScript> readFromLocation(String location) throws URISyntaxException, IOException {
        final URI uri;

        if (location.startsWith(CLASSPATH_PREFIX)) {
            URL url = resolveURL(location.substring(CLASSPATH_PREFIX.length()));
            uri = (url != null) ? url.toURI() : null;
        } else if (location.startsWith(FILE_PREFIX)) {
            uri = Paths.get(location.substring(FILE_PREFIX.length())).toUri();
        } else {
            throw new MigrationException(String.format("could not read location path %s, " +
                            "should look like this: %ses/migration or this: %s/home/scripts/migration",
                    location, CLASSPATH_PREFIX, FILE_PREFIX));
        }

        if (uri == null) {
            return Collections.emptyList();
        }
        List<RawMigrationScript> migrationScripts;
        try {
            migrationScripts = processResource(uri, path -> Files
                    .find(path, 10, (pathToCheck, basicFileAttributes) ->
                            !basicFileAttributes.isDirectory()
                                    && hasValidSuffix(pathToCheck)
                                    && basicFileAttributes.size() > 0
                                    && pathToCheck.getFileName().toString().startsWith(this.esMigrationPrefix))
                    .map(file -> {
                        String filename = file.getFileName().toString();
                        try (BufferedReader reader = Files.newBufferedReader(file, this.encoding)) {
                            String content = reader.lines()
                                    .collect(Collectors.joining(System.lineSeparator()));
                            return new RawMigrationScript().setFileName(filename).setContent(content);
                        } catch (IOException e) {
                            throw new IllegalStateException("can't read file: " + file.getFileName(), e);
                        }
                    }).collect(Collectors.toList()));
        } catch (NoSuchFileException e) {
            throw new MigrationException(String.format("The location %s is not a valid location!", location), e);
        }
        return migrationScripts;
    }

    public static URL resolveURL(String path) {
        ClassLoader classLoader = getDefaultClassLoader();
        if (classLoader != null) {
            return classLoader.getResource(path);
        } else {
            return ClassLoader.getSystemResource(path);
        }
    }

    private boolean hasValidSuffix(Path pathToCheck) {
        return this.esMigrationSuffixes
                .stream()
                .anyMatch(suffix -> pathToCheck.toString().toLowerCase().endsWith(suffix.toLowerCase()));
    }


    private static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = MigrationScriptReaderImpl.class.getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }
        return cl;
    }

    @FunctionalInterface
    interface IOFunction<T, R> {
        R accept(T t) throws IOException;
    }

    private static <R> R processResource(URI uri, IOFunction<Path, R> action) throws IOException {
        try {
            Path p = Paths.get(uri);
            return action.accept(p);
        } catch (FileSystemNotFoundException ex) {
            // handle resources in jar files
            try (FileSystem fs = FileSystems.newFileSystem(
                    uri, Collections.emptyMap())) {
                Path p = fs.provider().getPath(uri);
                return action.accept(p);
            }
        }
    }
}
