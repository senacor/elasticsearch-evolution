package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationScriptReader;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;

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
                        return readFromLocation(location);
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
    protected Stream<RawMigrationScript> readFromLocation(String location) throws URISyntaxException, IOException {
        if (location.startsWith(CLASSPATH_PREFIX)) {
            return readScriptsFromClassPath(location);

        } else if (location.startsWith(FILE_PREFIX)) {
            return readScriptsFromFilesystem(location);
        } else {
            throw new MigrationException(String.format("could not read location path %s, " +
                            "should look like this: %ses/migration or this: %s/home/scripts/migration",
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
        final String locationWithoutPrefixAsPackageNotation = location.substring(CLASSPATH_PREFIX.length())
                .replace("/", ".");

        final Collection<URL> urls = ClasspathHelper.forPackage(locationWithoutPrefixAsPackageNotation);
        final Set<String> resources;
        if (urls.isEmpty()) {
            // https://github.com/senacor/elasticsearch-evolution/issues/27
            // when the package is empty or does not exist, Reflections can't find any URLs to scan for
            resources = emptySet();
        } else {
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setScanners(new ResourcesScanner())
                    .filterInputsBy(new FilterBuilder().includePackage(locationWithoutPrefixAsPackageNotation))
                    .setUrls(urls));
            resources = reflections.getResources(this::isValidFilename);
        }

        return resources.stream().flatMap(resource -> {
            logger.debug("reading migration script '{}' from classpath...", resource);
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getInputStream(resource), encoding))) {
                Path p = Paths.get(resource);
                return read(bufferedReader, p.getFileName().toString());
            } catch (IOException e) {
                throw new MigrationException("can't read script from classpath: " + resource, e);
            }
        });
    }

    private Stream<RawMigrationScript> read(BufferedReader reader, String filename) {
        String content = reader.lines()
                .collect(Collectors.joining(System.lineSeparator()));
        if (content.isEmpty()) {
            return Stream.empty();
        }
        return Stream.of(new RawMigrationScript().setFileName(filename).setContent(content));
    }

    public static InputStream getInputStream(String path) {
        ClassLoader classLoader = getDefaultClassLoader();
        if (classLoader != null) {
            return classLoader.getResourceAsStream(path);
        } else {
            return ClassLoader.getSystemResourceAsStream(path);
        }
    }

    private boolean hasValidSuffix(String path) {
        return this.esMigrationSuffixes
                .stream()
                .anyMatch(suffix -> path.toLowerCase().endsWith(suffix.toLowerCase()));
    }

    static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
            logger.trace("getDefaultClassLoader - Thread.currentThread().getContextClassLoader()='{}'", cl);
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = MigrationScriptReaderImpl.class.getClassLoader();
            logger.trace("getDefaultClassLoader - MigrationScriptReaderImpl.class.getClassLoader()='{}'", cl);
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                    logger.trace("getDefaultClassLoader - ClassLoader.getSystemClassLoader()='{}'", cl);
                } catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }
        return cl;
    }

    private boolean isValidFilename(String fileName) {
        return hasValidSuffix(fileName)
                && fileName.startsWith(this.esMigrationPrefix);
    }
}
