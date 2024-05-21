package com.senacor.elasticsearch.evolution.core.api.config;

import com.senacor.elasticsearch.evolution.core.ElasticsearchEvolution;
import com.senacor.elasticsearch.evolution.core.internal.model.MigrationVersion;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.*;
import static java.util.Objects.requireNonNull;

/**
 * Configuration Properties for ElasticsearchEvolution
 *
 * @author Andreas Keefer
 */
@ConfigurationProperties(prefix = "spring.elasticsearch.evolution")
public class ElasticsearchEvolutionConfig {

    /**
     * Whether to enable elasticsearch-evolution.
     */
    private boolean enabled = true;

    /**
     * Locations of migrations scripts. Supported is classpath:some/path and file:/some/path
     * The location is scanned recursive.
     * NOTE: all scripts in all locations / subdirectories will be flatted and only the version number will be used to
     * order them.
     */
    private List<String> locations = new ArrayList<>(
            Collections.singletonList("classpath:es/migration"));

    /**
     * Encoding of migration files.
     */
    private Charset encoding = StandardCharsets.UTF_8;

    /**
     * Line separator, used only temporary between reading raw migration file line-by-line and parsing it later.
     * <p>
     * NOTE: Only needed for backward compatibility / checksum stability!
     * <p>
     * Should be one of
     * - '\n' (LF - Linux/Unix/OS X)
     * - '\r' (CR - Classic MAC OS)
     * - '\r\n' (CRLF - Windows)
     */
    private String lineSeparator = "\n";

    /**
     * This content type will be used as default if no contentType header is specified in the header section of a migration script.
     * If no charset is defined, the {@link #encoding} charset is used.
     */
    private String defaultContentType = "application/json; charset=UTF-8";

    /**
     * File name prefix for ES migrations.
     */
    private String esMigrationPrefix = "V";

    /**
     * File name suffix(es) for ES migrations.
     */
    private List<String> esMigrationSuffixes = new ArrayList<>(
            Collections.singleton(".http"));

    /**
     * Placeholders and their replacements to apply to migration scripts.
     */
    private Map<String, String> placeholders = new HashMap<>();

    /**
     * Prefix of placeholders in migration scripts.
     */
    private String placeholderPrefix = "${";

    /**
     * Suffix of placeholders in migration scripts.
     */
    private String placeholderSuffix = "}";

    /**
     * Perform placeholder replacement in migration scripts.
     */
    private boolean placeholderReplacement = true;

    /**
     * Name of the history index that will be used by elasticsearch-evolution.
     */
    private String historyIndex = "es_evolution";

    /**
     * The maximum query size while validating already executed scripts.
     * This query size have to be higher than the total count of your migration scripts.
     */
    private int historyMaxQuerySize = 1_000;

    /**
     * Whether to fail when a previously applied migration script has been modified after it was applied.
     */
    private boolean validateOnMigrate = true;

    /**
     * Whether to remove a trailing newline in migration scripts.
     * <p>
     * NOTE: This is only needed for backward compatibility / checksum stability!
     */
    private boolean trimTrailingNewlineInMigrations = false;

    /**
     * version to use as a baseline.
     * The baseline version will be the first one applied, the versions below will be ignored.
     */
    private String baselineVersion = "1.0";

    /**
     * Allows migrations to be run “out of order”.
     * <p>
     * If you already have versions 1.0 and 3.0 applied, and now a version 2.0 is found,
     * it will be applied too instead of being rejected.
     */
    private boolean outOfOrder = false;

    /**
     * Loads this configuration into a new ElasticsearchEvolution instance.
     *
     * @param restClient REST client to interact with Elasticsearch
     * @return The new fully-configured ElasticsearchEvolution instance.
     */
    public ElasticsearchEvolution load(RestClient restClient) {
        return new ElasticsearchEvolution(this, restClient);
    }

    /**
     * Validate this Configuration
     *
     * @return this instance, if validation was successful, otherwise a RuntimeException
     * @throws IllegalStateException if validation failed
     * @throws NullPointerException  if validation failed
     */
    public ElasticsearchEvolutionConfig validate() throws IllegalStateException, NullPointerException {
        if (enabled) {
            requireNotEmpty(locations, "locations must not be empty");
            requireNonNull(encoding, "encoding must not be null");
            requireNotBlank(esMigrationPrefix, "esMigrationPrefix must not be empty");
            requireNotEmpty(esMigrationSuffixes, "esMigrationSuffixes must not be empty");
            requireNotBlank(defaultContentType, "defaultContentType must not be empty");
            if (placeholderReplacement) {
                requireNonNull(placeholders, "placeholders must not be null");
                requireNotBlank(placeholderPrefix, "placeholderPrefix must not be empty");
                requireNotBlank(placeholderSuffix, "placeholderSuffix must not be empty");
                placeholders.forEach((name, value) -> {
                    requireCondition(name, s -> !s.contains(placeholderPrefix),
                            "placeholder name '%s' must not contain placeholderPrefix '%s'", name, placeholderPrefix);
                    requireCondition(name, s -> !s.contains(placeholderSuffix),
                            "placeholder name '%s' must not contain placeholderSuffix '%s'", name, placeholderSuffix);
                    requireCondition(value, s -> !s.contains(placeholderPrefix),
                            "placeholder value '%s' must not contain placeholderPrefix '%s'", value, placeholderPrefix);
                    requireCondition(value, s -> !s.contains(placeholderSuffix),
                            "placeholder value '%s' must not contain placeholderSuffix '%s'", value, placeholderSuffix);
                });
            }
            requireNotBlank(historyIndex, "historyIndex must not be empty");
            requireCondition(historyMaxQuerySize, size -> size > 0, "historyMaxQuerySize value '%s' must be greater than 0", historyMaxQuerySize);
            final MigrationVersion baseline;
            try {
                baseline = MigrationVersion.fromVersion(baselineVersion);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("baselineVersion is invalid", e);
            }
            requireCondition(baseline, version -> version.isAtLeast("1"), "baselineVersion '%s' must be at least 1", baseline);
        }
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ElasticsearchEvolutionConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public List<String> getLocations() {
        return locations;
    }

    public ElasticsearchEvolutionConfig setLocations(List<String> locations) {
        this.locations = locations;
        return this;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public ElasticsearchEvolutionConfig setEncoding(Charset encoding) {
        this.encoding = encoding;
        return this;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public ElasticsearchEvolutionConfig setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
        return this;
    }

    public String getDefaultContentType() {
        return defaultContentType;
    }

    public ElasticsearchEvolutionConfig setDefaultContentType(String defaultContentType) {
        this.defaultContentType = defaultContentType;
        return this;
    }

    public String getHistoryIndex() {
        return historyIndex;
    }

    public ElasticsearchEvolutionConfig setHistoryIndex(String historyIndex) {
        this.historyIndex = historyIndex;
        return this;
    }

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    public ElasticsearchEvolutionConfig setPlaceholders(Map<String, String> placeholders) {
        this.placeholders = placeholders;
        return this;
    }

    public String getPlaceholderPrefix() {
        return placeholderPrefix;
    }

    public ElasticsearchEvolutionConfig setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
        return this;
    }

    public String getPlaceholderSuffix() {
        return placeholderSuffix;
    }

    public ElasticsearchEvolutionConfig setPlaceholderSuffix(String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
        return this;
    }

    public boolean isPlaceholderReplacement() {
        return placeholderReplacement;
    }

    public ElasticsearchEvolutionConfig setPlaceholderReplacement(boolean placeholderReplacement) {
        this.placeholderReplacement = placeholderReplacement;
        return this;
    }

    public String getEsMigrationPrefix() {
        return esMigrationPrefix;
    }

    public ElasticsearchEvolutionConfig setEsMigrationPrefix(String esMigrationPrefix) {
        this.esMigrationPrefix = esMigrationPrefix;
        return this;
    }

    public List<String> getEsMigrationSuffixes() {
        return esMigrationSuffixes;
    }

    public ElasticsearchEvolutionConfig setEsMigrationSuffixes(List<String> esMigrationSuffixes) {
        this.esMigrationSuffixes = esMigrationSuffixes;
        return this;
    }

    public int getHistoryMaxQuerySize() {
        return historyMaxQuerySize;
    }

    public ElasticsearchEvolutionConfig setHistoryMaxQuerySize(int historyMaxQuerySize) {
        this.historyMaxQuerySize = historyMaxQuerySize;
        return this;
    }

    public boolean isValidateOnMigrate() {
        return validateOnMigrate;
    }

    public ElasticsearchEvolutionConfig setValidateOnMigrate(boolean validateOnMigrate) {
        this.validateOnMigrate = validateOnMigrate;
        return this;
    }

    public boolean isTrimTrailingNewlineInMigrations() {
        return trimTrailingNewlineInMigrations;
    }

    public ElasticsearchEvolutionConfig setTrimTrailingNewlineInMigrations(boolean trimTrailingNewlineInMigrations) {
        this.trimTrailingNewlineInMigrations = trimTrailingNewlineInMigrations;
        return this;
    }

    public String getBaselineVersion() {
        return baselineVersion;
    }

    public ElasticsearchEvolutionConfig setBaselineVersion(String baselineVersion) {
        this.baselineVersion = baselineVersion;
        return this;
    }

    public boolean isOutOfOrder() {
        return outOfOrder;
    }

    public ElasticsearchEvolutionConfig setOutOfOrder(boolean outOfOrder) {
        this.outOfOrder = outOfOrder;
        return this;
    }

    @Override
    public String toString() {
        return "ElasticsearchEvolutionConfig{" +
                "enabled=" + enabled +
                ", locations=" + locations +
                ", encoding=" + encoding +
                ", lineSeparator='" + lineSeparator.replace("\n", "\\n").replace("\r", "\\r") + '\'' +
                ", defaultContentType='" + defaultContentType + '\'' +
                ", esMigrationPrefix='" + esMigrationPrefix + '\'' +
                ", esMigrationSuffixes=" + esMigrationSuffixes +
                ", placeholders=" + placeholders +
                ", placeholderPrefix='" + placeholderPrefix + '\'' +
                ", placeholderSuffix='" + placeholderSuffix + '\'' +
                ", placeholderReplacement=" + placeholderReplacement +
                ", historyIndex='" + historyIndex + '\'' +
                ", historyMaxQuerySize=" + historyMaxQuerySize +
                ", validateOnMigrate=" + validateOnMigrate +
                ", trimTrailingNewlineInMigrations=" + trimTrailingNewlineInMigrations +
                ", baselineVersion='" + baselineVersion + '\'' +
                ", outOfOrder='" + outOfOrder + '\'' +
                '}';
    }
}
