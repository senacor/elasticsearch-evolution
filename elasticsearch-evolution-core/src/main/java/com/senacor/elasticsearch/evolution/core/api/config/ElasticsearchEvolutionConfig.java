package com.senacor.elasticsearch.evolution.core.api.config;

import com.senacor.elasticsearch.evolution.core.ElasticsearchEvolution;
import org.elasticsearch.client.RestHighLevelClient;
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
     */
    private List<String> locations = new ArrayList<>(
            Collections.singletonList("classpath:es/migration"));

    /**
     * Encoding of migration files.
     */
    private Charset encoding = StandardCharsets.UTF_8;

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
     * Loads this configuration into a new ElasticsearchEvolution instance.
     *
     * @param restHighLevelClient REST client to interact with Elasticsearch
     * @return The new fully-configured ElasticsearchEvolution instance.
     */
    public ElasticsearchEvolution load(RestHighLevelClient restHighLevelClient) {
        return new ElasticsearchEvolution(this, restHighLevelClient);
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

    @Override
    public String toString() {
        return "ElasticsearchEvolutionConfig{" +
                "enabled=" + enabled +
                ", locations=" + locations +
                ", encoding=" + encoding +
                ", defaultContentType='" + defaultContentType + '\'' +
                ", esMigrationPrefix='" + esMigrationPrefix + '\'' +
                ", esMigrationSuffixes=" + esMigrationSuffixes +
                ", placeholders=" + placeholders +
                ", placeholderPrefix='" + placeholderPrefix + '\'' +
                ", placeholderSuffix='" + placeholderSuffix + '\'' +
                ", placeholderReplacement=" + placeholderReplacement +
                ", historyIndex='" + historyIndex + '\'' +
                ", historyMaxQuerySize=" + historyMaxQuerySize +
                '}';
    }
}
