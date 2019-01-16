package com.senacor.elasticsearch.evolution.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Configuration Properties for ElasticsearchEvolution
 *
 * @author Andreas Keefer
 */
@ConfigurationProperties(prefix = "spring.elasticsearch.evolution")
public class ElasticsearchEvolutionProperties {

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
     * Encoding of SQL migrations.
     */
    private Charset encoding = StandardCharsets.UTF_8;

    /**
     * Name of the schema schema history index that will be used by elasticsearch-evolution.
     */
    private String index = "es_evolution";

    /**
     * Placeholders and their replacements to apply to sql migration scripts.
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
     * File name prefix for ES migrations.
     */
    private String esMigrationPrefix = "V";

    /**
     * File name suffix for ES migrations.
     */
    private List<String> esMigrationSuffixes = new ArrayList<>(
            Collections.singleton(".http"));

    /**
     * Default Constructor
     */
    public ElasticsearchEvolutionProperties() {
    }

    private ElasticsearchEvolutionProperties(Builder builder) {
        setEnabled(builder.enabled);
        setLocations(builder.locations);
        setEncoding(builder.encoding);
        setIndex(builder.index);
        setPlaceholders(builder.placeholders);
        setPlaceholderPrefix(builder.placeholderPrefix);
        setPlaceholderSuffix(builder.placeholderSuffix);
        setPlaceholderReplacement(builder.placeholderReplacement);
        setEsMigrationPrefix(builder.esMigrationPrefix);
        setEsMigrationSuffixes(builder.esMigrationSuffixes);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ElasticsearchEvolutionProperties copy) {
        Builder builder = new Builder();
        builder.enabled = copy.getEnabled();
        builder.locations = copy.getLocations();
        builder.encoding = copy.getEncoding();
        builder.index = copy.getIndex();
        builder.placeholders = copy.getPlaceholders();
        builder.placeholderPrefix = copy.getPlaceholderPrefix();
        builder.placeholderSuffix = copy.getPlaceholderSuffix();
        builder.placeholderReplacement = copy.getPlaceholderReplacement();
        builder.esMigrationPrefix = copy.getEsMigrationPrefix();
        builder.esMigrationSuffixes = copy.getEsMigrationSuffixes();
        return builder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private boolean getEnabled() {
        return isEnabled();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(Map<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    public String getPlaceholderPrefix() {
        return placeholderPrefix;
    }

    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
    }

    public String getPlaceholderSuffix() {
        return placeholderSuffix;
    }

    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
    }

    public boolean isPlaceholderReplacement() {
        return placeholderReplacement;
    }

    private boolean getPlaceholderReplacement() {
        return isPlaceholderReplacement();
    }

    public void setPlaceholderReplacement(boolean placeholderReplacement) {
        this.placeholderReplacement = placeholderReplacement;
    }

    public String getEsMigrationPrefix() {
        return esMigrationPrefix;
    }

    public void setEsMigrationPrefix(String esMigrationPrefix) {
        this.esMigrationPrefix = esMigrationPrefix;
    }

    public List<String> getEsMigrationSuffixes() {
        return esMigrationSuffixes;
    }

    public void setEsMigrationSuffixes(List<String> esMigrationSuffixes) {
        this.esMigrationSuffixes = esMigrationSuffixes;
    }

    @Override
    public String toString() {
        return "ElasticsearchEvolutionProperties{" +
                "enabled=" + enabled +
                ", locations=" + locations +
                ", encoding=" + encoding +
                ", index='" + index + '\'' +
                ", placeholders=" + placeholders +
                ", placeholderPrefix='" + placeholderPrefix + '\'' +
                ", placeholderSuffix='" + placeholderSuffix + '\'' +
                ", placeholderReplacement=" + placeholderReplacement +
                ", esMigrationPrefix='" + esMigrationPrefix + '\'' +
                ", esMigrationSuffixes=" + esMigrationSuffixes +
                '}';
    }

    public static final class Builder {
        private boolean enabled;
        private List<String> locations;
        private Charset encoding;
        private String index;
        private Map<String, String> placeholders;
        private String placeholderPrefix;
        private String placeholderSuffix;
        private boolean placeholderReplacement;
        private String esMigrationPrefix;
        private List<String> esMigrationSuffixes;

        private Builder() {
        }

        public Builder withEnabled(boolean val) {
            enabled = val;
            return this;
        }

        public Builder withLocations(List<String> val) {
            locations = val;
            return this;
        }

        public Builder withEncoding(Charset val) {
            encoding = val;
            return this;
        }

        public Builder withIndex(String val) {
            index = val;
            return this;
        }

        public Builder withPlaceholders(Map<String, String> val) {
            placeholders = val;
            return this;
        }

        public Builder withPlaceholderPrefix(String val) {
            placeholderPrefix = val;
            return this;
        }

        public Builder withPlaceholderSuffix(String val) {
            placeholderSuffix = val;
            return this;
        }

        public Builder withPlaceholderReplacement(boolean val) {
            placeholderReplacement = val;
            return this;
        }

        public Builder withEsMigrationPrefix(String val) {
            esMigrationPrefix = val;
            return this;
        }

        public Builder withEsMigrationSuffixes(List<String> val) {
            esMigrationSuffixes = val;
            return this;
        }

        public ElasticsearchEvolutionProperties build() {
            return new ElasticsearchEvolutionProperties(this);
        }
    }
}
