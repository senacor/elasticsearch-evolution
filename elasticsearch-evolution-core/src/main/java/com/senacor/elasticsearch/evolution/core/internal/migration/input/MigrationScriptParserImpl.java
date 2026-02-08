package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationScriptParser;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationVersion;
import com.senacor.elasticsearch.evolution.core.api.migration.java.JavaMigration;
import com.senacor.elasticsearch.evolution.core.internal.model.FileNameInfo;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.*;
import com.senacor.elasticsearch.evolution.rest.abstraction.HttpMethod;
import lombok.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.requireCondition;
import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.requireNotBlank;
import static java.util.Objects.requireNonNull;

/**
 * Parses the filename and the content of the migration file
 *
 * @author Andreas Keefer
 */
public class MigrationScriptParserImpl implements MigrationScriptParser {

    private static final String VERSION_DESCRIPTION_SEPARATOR = "__";

    private final String esMigrationPrefix;
    private final List<String> esMigrationSuffixes;
    private final Map<String, String> placeholders;
    private final String placeholderPrefix;
    private final String placeholderSuffix;
    private final boolean placeholderReplacement;
    private final String lineSeparator;

    /**
     * create Parser
     */
    public MigrationScriptParserImpl(String esMigrationPrefix,
                                     List<String> esMigrationSuffixes,
                                     Map<String, String> placeholders,
                                     String placeholderPrefix,
                                     String placeholderSuffix,
                                     boolean placeholderReplacement,
                                     String lineSeparator) {
        this.esMigrationPrefix = esMigrationPrefix;
        this.esMigrationSuffixes = esMigrationSuffixes;
        this.placeholders = placeholders;
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
        this.placeholderReplacement = placeholderReplacement;
        this.lineSeparator = lineSeparator;
    }

    @Override
    public Collection<ParsedMigration<?>> parse(Collection<RawMigrationScript<?>> rawMigrationScripts) {
        requireNonNull(rawMigrationScripts, "rawMigrationScripts must not be null");
        return rawMigrationScripts.stream()
                .map(this::parse)
                .collect(Collectors.toUnmodifiableList());
    }

    ParsedMigration<?> parse(RawMigrationScript<?> rawMigrationScript) {
        if (rawMigrationScript.getContent() instanceof ScriptMigrationContent scriptMigrationContent) {
            return new ParsedMigration<>()
                    .setFileNameInfo(parseFileNameFromScriptMigration(rawMigrationScript.getFileName()))
                    .setChecksum(rawMigrationScript.getContent().checksum())
                    .setMigrationRequest(parseContent(scriptMigrationContent));
        } else if (rawMigrationScript.getContent() instanceof JavaMigrationRequestContent javaMigrationRequestContent) {
            return new ParsedMigration<>()
                    .setFileNameInfo(parseFileNameFromJavaMigration(rawMigrationScript.getFileName(), javaMigrationRequestContent.javaMigration()))
                    .setChecksum(rawMigrationScript.getContent().checksum())
                    .setMigrationRequest(javaMigrationRequestContent);
        }

        throw new IllegalStateException("Unsupported migration content type: '%s'".formatted(rawMigrationScript.getContent().getClass().getName()));
    }

    private MigrationScriptRequest parseContent(ScriptMigrationContent scriptMigrationContent) {
        String contentReplaced = placeholderReplacement
                ? replaceParams(scriptMigrationContent.content())
                : scriptMigrationContent.content();
        MigrationScriptRequest res = new MigrationScriptRequest();

        final AtomicReference<ParseState> state = new AtomicReference<>(ParseState.METHOD_PATH);
        for (String line : contentReplaced.split(lineSeparator, -1)) {
            if (!line.trim().startsWith("#") && !line.trim().startsWith("//")) {
                switch (state.get()) {
                    case METHOD_PATH -> {
                        parseMethodWithPath(res, line);
                        state.set(ParseState.HEADER);
                    }
                    case HEADER -> {
                        if (line.trim().isEmpty()) {
                            state.set(ParseState.CONTENT);
                        } else {
                            parseHeader(res, line);
                        }
                    }
                    case CONTENT -> {
                        if (!res.isBodyEmpty()) {
                            res.addToBody(lineSeparator);
                        }
                        res.addToBody(line);
                    }
                    default -> throw new UnsupportedOperationException("state '" + state + "' not supported");
                }
            }
        }
        return res;
    }

    private void parseHeader(MigrationScriptRequest res, String line) {
        String[] header = line.trim().split("[:=]", 2);
        if (header.length != 2) {
            throw new MigrationException(
                    "can't parse header: '%s'. Header must be separated by ':' and should look like this: 'Content-Type: application/json'".formatted(
                    line));
        }
        res.addHttpHeader(header[0].trim(), header[1].trim());
    }

    private void parseMethodWithPath(MigrationScriptRequest res, String line) {
        String[] methodAndPath = line.trim().split(" +", 2);
        if (methodAndPath.length != 2) {
            throw new MigrationException(
                    "can't parse method and path: '%s'. Method and path must be separated by space and should look like this: 'PUT /my_index'".formatted(
                    line));
        }
        res.setHttpMethod(HttpMethod.create(methodAndPath[0]))
                .setPath(methodAndPath[1].trim());
    }

    FileNameInfo parseFileNameFromScriptMigration(String fileName) {
        return parseFileName(fileName,
                esMigrationPrefix,
                VERSION_DESCRIPTION_SEPARATOR,
                esMigrationSuffixes);
    }

    private FileNameInfo parseFileNameFromJavaMigration(String fileName,
                                                        @NonNull JavaMigration javaMigration) {
        if (javaMigration.getMetadata() != null) {
            // Use version and description from JavaMigration implementation
            final MigrationVersion version = requireMajorVersionIsGreaterThan0(javaMigration.getMetadata().version(), fileName);
            final String description = requireNotBlank(javaMigration.getMetadata().description(),
                    "defined description in JavaMigration file '%s' must not be blank", fileName);
            return new FileNameInfoImpl(version, description, fileName);
        }

        // determine version and description from file name ignoring the suffixes
        return parseFileName(fileName,
                esMigrationPrefix,
                VERSION_DESCRIPTION_SEPARATOR,
                List.of(""));
    }

    String replaceParams(String template) {
        return placeholders.entrySet().stream()
                .reduce(template,
                        (s, e) -> s.replace(placeholderPrefix + e.getKey() + placeholderSuffix, e.getValue()),
                        (s, s2) -> s);
    }

    /**
     * Extracts the schema version and the description from a migration name formatted as 1_2__Description.
     *
     * @param migrationName The migration name to parse. Should not contain any folders or packages.
     * @param prefix        The migration prefix.
     * @param separator     The migration separator.
     * @param suffixes      The migration suffixes
     * @return The extracted schema version.
     */
    private FileNameInfo parseFileName(String migrationName,
                               String prefix,
                               String separator,
                               List<String> suffixes) {
        String cleanMigrationName = cleanMigrationName(migrationName, prefix, suffixes);

        int separatorPos = cleanMigrationName.indexOf(separator);

        String version;
        String description;
        if (separatorPos < 0) {
            throw new MigrationException(
                    "Description in migration filename is required: '%s'. It should look like this: '%s1.2%ssome_desctiption here%s'".formatted(
                    migrationName, prefix, separator, suffixes.get(0)));
        }

        description = cleanMigrationName.substring(separatorPos + separator.length()).replace("_", " ");
        version = requireNotBlank(cleanMigrationName.substring(0, separatorPos),
                "Wrong versioned migration name format: '" + migrationName
                        + "' (It must contain a version and should look like this: "
                        + prefix + "1.2" + separator + description + suffixes.get(0) + ")");

        MigrationVersion migrationVersion = requireMajorVersionIsGreaterThan0(MigrationVersion.fromVersion(version), migrationName);
        return new FileNameInfoImpl(migrationVersion, description, migrationName);
    }

    private static MigrationVersion requireMajorVersionIsGreaterThan0(MigrationVersion migrationVersion,
                                                                      String migrationName) {
        return requireCondition(migrationVersion,
                vers -> vers.isMajorNewerThan("0"),
                "used version '%s' in migration file '%s' is not allowed. Major version must be greater than 0",
                migrationVersion, migrationName);
    }

    /**
     * remove prefix and suffix
     */
    private String cleanMigrationName(String migrationName, String prefix, List<String> suffixes) {
        for (String suffix : suffixes) {
            if (migrationName.toLowerCase().endsWith(suffix.toLowerCase())) {
                return migrationName.substring(
                        prefix.length(),
                        migrationName.length() - suffix.length());
            }
        }
        throw new MigrationException(
                "Wrong versioned migration name format: '%s'. It must end with a configured suffix: '%s'".formatted(
                migrationName,
                suffixes));
    }

    private enum ParseState {
        METHOD_PATH,
        HEADER,
        CONTENT
    }
}
