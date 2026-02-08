package com.senacor.elasticsearch.evolution.core.api.config;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Andreas Keefer
 */
class ElasticsearchEvolutionConfigTest {

    @Test
    void springConfigurationMmetadataJson_should_have_been_generated_by_springBootConfigurationProcessor() {
        final List<String> springConfigMetadata;
        try (ScanResult scanResult = new ClassGraph().acceptPathsNonRecursive("META-INF/").scan()) {
            springConfigMetadata = scanResult.getResourcesWithLeafName("spring-configuration-metadata.json")
                    .stream()
                    .map(resource -> {
                        try {
                            return resource.getContentAsString();
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .toList();
        }

        assertThat(springConfigMetadata)
                .anySatisfy(metadataContent -> assertThat(metadataContent).contains(ElasticsearchEvolutionConfigImpl.class.getName()));
    }

    @Nested
    class validate {

        @Test
        void defaultConfig() {
            assertThat(new ElasticsearchEvolutionConfigImpl().validate()).isNotNull();
        }

        @Test
        void noValidEncoding() {
            final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl()
                    .setEncoding(null);

            assertThatThrownBy(config::validate)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("encoding must not be null");
        }

        @Test
        void noValidPlaceholders() {
            final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl()
                    .setPlaceholders(null);

            assertThatThrownBy(config::validate)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("placeholders must not be null");
        }

        @Test
        void noValidLocations() {
            final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl()
                    .setLocations(Collections.emptyList());

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("locations must not be empty");
        }

        @Test
        void noValidEsMigrationPrefix() {
            final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl()
                    .setEsMigrationPrefix("");

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("esMigrationPrefix must not be empty");
        }

        @Test
        void noValidEsMigrationSuffixes() {
            final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl()
                    .setEsMigrationSuffixes(Collections.emptyList());

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("esMigrationSuffixes must not be empty");
        }

        @Test
        void noValidPlaceholderPrefix() {
            final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl()
                    .setPlaceholderPrefix("");

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholderPrefix must not be empty");
        }

        @Test
        void noValidPlaceholderSuffix() {
            final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl()
                    .setPlaceholderSuffix("");

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholderSuffix must not be empty");
        }

        @Test
        void placeholderNameMustNotContainPlaceholderSuffix() {
            final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl()
                    .setPlaceholders(Collections.singletonMap("x}x", "x"));

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholder name 'x}x' must not contain placeholderSuffix '}'");
        }

        @Test
        void placeholderNameMustNotContainPlaceholderPrefix() {
            final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl()
                    .setPlaceholders(Collections.singletonMap("x${x", "x"));

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholder name 'x${x' must not contain placeholderPrefix '${'");
        }

        @Test
        void placeholderValueMustNotContainPlaceholderSuffix() {
            final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl()
                    .setPlaceholders(Collections.singletonMap("x", "x}x"));

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholder value 'x}x' must not contain placeholderSuffix '}'");
        }

        @Test
        void placeholderValueMustNotContainPlaceholderPrefix() {
            final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl()
                    .setPlaceholders(Collections.singletonMap("x", "x${x"));

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholder value 'x${x' must not contain placeholderPrefix '${'");
        }

        @Test
        void noValidHistoryMaxQuerySize_mustBeGreaterThan0() {
            final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl()
                    .setHistoryMaxQuerySize(0);

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("historyMaxQuerySize value '0' must be greater than 0");
        }

        @Test
        void baselineVersion_must_be_at_least_1() {
            final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl()
                    .setBaselineVersion("0");

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("baselineVersion '0' must be at least 1");
        }
    }
}