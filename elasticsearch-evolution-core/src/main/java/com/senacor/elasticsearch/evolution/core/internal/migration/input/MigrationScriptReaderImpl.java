package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationScriptReader;
import com.senacor.elasticsearch.evolution.core.api.migration.java.ClassProvider;
import com.senacor.elasticsearch.evolution.core.api.migration.java.JavaMigration;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.JavaMigrationRequestContent;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ScriptMigrationContent;
import io.github.classgraph.*;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final boolean trimTrailingNewlineInMigrations;
    @NonNull
    private final ElasticsearchEvolutionConfig config;

    public MigrationScriptReaderImpl(@NonNull ElasticsearchEvolutionConfig config) {
        this.config = config;
        this.locations = config.getLocations();
        this.encoding = config.getEncoding();
        this.esMigrationPrefix = config.getEsMigrationPrefix();
        this.esMigrationSuffixes = config.getEsMigrationSuffixes();
        this.lineSeparator = config.getLineSeparator();
        this.trimTrailingNewlineInMigrations = config.isTrimTrailingNewlineInMigrations();
    }

    /**
     * Reads all migrations from the specified locations that match prefixes and suffixes
     * <p>
     * Also reads JavaMigrations from the specified locations and from the custom JavaMigrations ClassProvider (if provided)
     * and creates corresponding RawMigrationScripts for them.
     * <p>
     * Also converts provided additional JavaMigration instances from the config to corresponding RawMigrationScripts.
     *
     * @return a list of {@link RawMigrationScript}
     */
    @Override
    public List<RawMigrationScript<?>> read() {
        final Stream<RawMigrationScript<?>> rarMigrationsFromLocations = this.locations.stream()
                .flatMap(location -> {
                    try {
                        return readFromLocation(location);
                    } catch (URISyntaxException | IOException e) {
                        throw new MigrationException(
                                "couldn't read migrations from %s".formatted(location), e);
                    }
                });
        final Stream<RawMigrationScript<?>> rarMigrationsFromCustomJavaMigrationsClassProvider = readFromCustomJavaMigrationsClassProvider(config.getJavaMigrationClassProvider());
        final Stream<RawMigrationScript<?>> additionalRawJavaMigrations = config.getJavaMigrations().stream()
                .map(this::createRawMigrationScript);
        return Stream.concat(Stream.concat(rarMigrationsFromLocations, rarMigrationsFromCustomJavaMigrationsClassProvider), additionalRawJavaMigrations)
                .distinct()
                .toList();
    }

    private Stream<RawMigrationScript<?>> readFromCustomJavaMigrationsClassProvider(ClassProvider<JavaMigration> customJavaMigrationsClassProvider) {
        if (null == customJavaMigrationsClassProvider) {
            return Stream.empty();
        }
        return customJavaMigrationsClassProvider.apply(config).stream()
                .map(javaMigrationClass -> {
                    logger.debug("reading JavaMigration '{}' from custom JavaMigrations ClassProvider...", javaMigrationClass.getName());
                    try {
                        final JavaMigration javaMigrationInstance = javaMigrationClass.getDeclaredConstructor().newInstance();
                        return createRawMigrationScript(javaMigrationInstance);
                    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                             InvocationTargetException e) {
                        throw new MigrationException("Couldn't create instance of JavaMigration: " + javaMigrationClass.getName(), e);
                    }
                });
    }

    /**
     * Reads migration scripts from a specific location
     *
     * @param location path where to look for migration scripts
     * @return a list of {@link RawMigrationScript}
     * @throws URISyntaxException if the location is not formatted strictly according to RFC2396 and cannot be converted to a URI.
     * @throws IOException        if an I/O error is thrown when accessing the files at the location(s).
     */
    protected Stream<RawMigrationScript<?>> readFromLocation(String location) throws URISyntaxException, IOException {
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

    private Stream<RawMigrationScript<?>> readScriptsFromFilesystem(String location) throws IOException {
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

    private Stream<RawMigrationScript<?>> readScriptsFromClassPath(String location) {
        if (!location.endsWith("/")) {
            // fixes https://github.com/senacor/elasticsearch-evolution/issues/36
            // otherwise e.g. "...location_some_suffix" will also be found when search for "...location".
            location = location + "/";
        }

        final String locationWithoutPrefix = location.substring(CLASSPATH_PREFIX.length());

        List<RawMigrationScript<?>> res = new ArrayList<>();
        // scan for script resources
        try (ScanResult scanResult = new ClassGraph()
                .acceptPaths(locationWithoutPrefix)
                .scan();
             ResourceList resourceList = scanResult.getAllResources()) {

            resourceList.filter(resource -> isValidFilename(Paths.get(resource.getPath()).getFileName().toString()))
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

        if (null == config.getJavaMigrationClassProvider()) {
            // scan for JavaMigrations
            String packageToScan = locationWithoutPrefix
                    .substring(0, locationWithoutPrefix.length() - 1)
                    .replace('/', '.');
            try (ScanResult scanResult = new ClassGraph()
                    .enableClassInfo()
                    .enableMethodInfo()
                    .acceptPackages(packageToScan)
                    .scan()) {
                scanResult.getClassesImplementing(JavaMigration.class)
                        .filter(classInfo -> !classInfo.isAbstract())
                        .filter(classInfo -> !classInfo.isInterface())
                        .forEach(classInfo -> {
                            logger.debug("reading JavaMigration '{}' ...", classInfo.getName());
                            MethodInfo constructorInfo = findPublicNoArgsConstructor(classInfo)
                                    .orElseThrow(() -> new MigrationException("JavaMigration " + classInfo.getName() + " does not have a public no-args constructor!"));
                            final Constructor<?> constructor = constructorInfo.loadClassAndGetConstructor();
                            final JavaMigration javaMigrationInstance;
                            try {
                                javaMigrationInstance = (JavaMigration) constructor.newInstance();
                            } catch (Exception e) {
                                throw new MigrationException("Couldn't create instance of JavaMigration: " + classInfo.getName(), e);
                            }
                            res.add(createRawMigrationScript(javaMigrationInstance));
                        });
            }
        }
        return res.stream();
    }

    private RawMigrationScript<JavaMigrationRequestContent> createRawMigrationScript(JavaMigration javaMigrationInstance) {
        final String fileName = javaMigrationInstance.getClass().getSimpleName();
        if (javaMigrationInstance.getMetadata() == null
                && !fileName.startsWith(this.esMigrationPrefix)) {
            throw new MigrationException("JavaMigration " + javaMigrationInstance.getClass().getName() +
                    " does not provide metadata and does not match migration file name pattern!");
        }
        return new RawMigrationScript<JavaMigrationRequestContent>()
                .setFileName(fileName)
                .setContent(new JavaMigrationRequestContent(javaMigrationInstance));
    }

    private Optional<MethodInfo> findPublicNoArgsConstructor(ClassInfo classInfo) {
        return classInfo.getDeclaredConstructorInfo()
                .filter(ClassMemberInfo::isPublic)
                .filter(constructorInfo -> constructorInfo.getParameterInfo().length == 0)
                .stream()
                .findFirst();
    }

    Stream<RawMigrationScript<ScriptMigrationContent>> read(BufferedReader reader, String filename) throws IOException {
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

        if (trimTrailingNewlineInMigrations && content.endsWith(lineSeparator)) {
            content = content.substring(0, content.length() - lineSeparator.length());
        }

        return Stream.of(new RawMigrationScript<ScriptMigrationContent>().setFileName(filename).setContent(new ScriptMigrationContent(content)));
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
