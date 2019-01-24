package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationScriptReader;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;

import java.io.BufferedReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * @author Andreas Keefer
 */

public class MigrationScriptReaderImpl implements MigrationScriptReader {

    private final List<String> locations;
    private final Charset encoding;
    private final String esMigrationPrefix;
    private final List<String> esMigrationSuffixes;

    /**
     * @param locations               Locations of migrations scripts, e.g classpath:es/migration
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
        List<RawMigrationScript> migrationScripts = new ArrayList<>();

        this.locations.stream()
                .map(MigrationScriptReaderImpl::extractLocationString)
                .map(location -> {
                    try {
                        return readFromLocation(location);
                    } catch (URISyntaxException | IOException e) {
                        throw new MigrationException(
                                String.format("couldn't read scripts from %s", location), e);
                    }
                }).forEach((migrationList) -> {
            migrationScripts.removeAll(migrationList);
            migrationScripts.addAll(migrationList);
        });
        return migrationScripts;
    }

    /**
     * Reads migration scripts from a specific location
     *
     * @param location path where to look for migration scripts
     * @return a list of {@link RawMigrationScript}
     * @throws URISyntaxException
     * @throws IOException
     */
    public List<RawMigrationScript> readFromLocation(String location) throws URISyntaxException, IOException {
        URL url = resolveURL(location);
        URI uri = url.toURI();

        return processResource(uri, path -> Files
                .find(path, 10, (p, basicFileAttributes) ->
                        !basicFileAttributes.isDirectory()
                                && this.esMigrationSuffixes.contains(p.toString().substring(p.toString().lastIndexOf(".")))
                                && basicFileAttributes.size() > 0
                                && p.getFileName().toString().startsWith(this.esMigrationPrefix))
                .map(file -> {
                    String filename = file.getFileName().toString();
                    try (BufferedReader reader = Files.newBufferedReader(file, this.encoding)) {
                        String content = reader.lines()
                                .collect(Collectors.joining(System.lineSeparator()));
                        return new RawMigrationScript().setFileName(filename).setContent(content);
                    } catch (IOException e) {
                        throw new IllegalStateException("can't read file: " + file.getFileName().toString(), e);
                    }
                }).collect(Collectors.toList()));
    }

    public static URL resolveURL(String path) {
        ClassLoader classLoader = getDefaultClassLoader();
        if (classLoader != null) {
            return classLoader.getResource(path);
        } else {
            return ClassLoader.getSystemResource(path);
        }
    }

    public static String extractLocationString(String location) throws MigrationException {
        String parsedPath = "";
        if (location.startsWith("classpath:")) {
            parsedPath = location.substring("classpath:".length());
            return parsedPath;
        } else if (location.matches("^\\w*:.*")) {
            throw new MigrationException(String.format("could not read location path %s, " +
                    "should look like this: classpath:es/migration", location));
        }
        return parsedPath;
    }


    public static ClassLoader getDefaultClassLoader() {
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

    public static <R> R processResource(URI uri, IOFunction<Path, R> action) throws IOException {
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
