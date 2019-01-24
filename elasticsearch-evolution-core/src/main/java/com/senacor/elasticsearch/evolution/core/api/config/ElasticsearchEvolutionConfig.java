package com.senacor.elasticsearch.evolution.core.api.config;

import com.senacor.elasticsearch.evolution.core.ElasticsearchEvolution;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.requireNotEmpty;
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
     * Locations of migrations scripts.
     */
    private List<String> locations = new ArrayList<>(
            Collections.singletonList("classpath:es/migration"));

    /**
     * Encoding of migration files.
     */
    private Charset encoding = StandardCharsets.UTF_8;

    /**
     * File name prefix for ES migrations.
     */
    private String esMigrationPrefix = "V";

    /**
     * File name suffix for ES migrations.
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
     * Name of the schema schema history index that will be used by elasticsearch-evolution.
     */
    private String historyIndex = "es_evolution";

    /**
     * Default Constructor
     */
    public ElasticsearchEvolutionConfig() {
    }

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
            requireNotEmpty(esMigrationPrefix, "esMigrationPrefix must not be empty");
            requireNotEmpty(esMigrationSuffixes, "esMigrationSuffixes must not be empty");

            if (placeholderReplacement) {
                requireNonNull(placeholders, "placeholders must not be null");
                requireNotEmpty(placeholderPrefix, "placeholderPrefix must not be empty");
                requireNotEmpty(placeholderSuffix, "placeholderSuffix must not be empty");
            }
            requireNotEmpty(historyIndex, "historyIndex must not be empty");
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

    @Override
    public String toString() {
        return "ElasticsearchEvolutionProperties{" +
                "enabled=" + enabled +
                ", locations=" + locations +
                ", encoding=" + encoding +
                ", historyIndex='" + historyIndex + '\'' +
                ", placeholders=" + placeholders +
                ", placeholderPrefix='" + placeholderPrefix + '\'' +
                ", placeholderSuffix='" + placeholderSuffix + '\'' +
                ", placeholderReplacement=" + placeholderReplacement +
                ", esMigrationPrefix='" + esMigrationPrefix + '\'' +
                ", esMigrationSuffixes=" + esMigrationSuffixes +
                '}';
    }
}
