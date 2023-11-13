package com.senacor.elasticsearch.evolution.core.api.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Andreas Keefer
 */
class ElasticsearchEvolutionConfigTest {

    @Nested
    class validate {

        @Test
        void defaultConfig() {
            assertThat(new ElasticsearchEvolutionConfig().validate()).isNotNull();
        }

        @Test
        void noValidEncoding() {
            final ElasticsearchEvolutionConfig config = new ElasticsearchEvolutionConfig()
                    .setEncoding(null);

            assertThatThrownBy(config::validate)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("encoding must not be null");
        }

        @Test
        void noValidPlaceholders() {
            final ElasticsearchEvolutionConfig config = new ElasticsearchEvolutionConfig()
                    .setPlaceholders(null);

            assertThatThrownBy(config::validate)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("placeholders must not be null");
        }

        @Test
        void noValidLocations() {
            final ElasticsearchEvolutionConfig config = new ElasticsearchEvolutionConfig()
                    .setLocations(Collections.emptyList());

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("locations must not be empty");
        }

        @Test
        void noValidEsMigrationPrefix() {
            final ElasticsearchEvolutionConfig config = new ElasticsearchEvolutionConfig()
                    .setEsMigrationPrefix("");

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("esMigrationPrefix must not be empty");
        }

        @Test
        void noValidEsMigrationSuffixes() {
            final ElasticsearchEvolutionConfig config = new ElasticsearchEvolutionConfig()
                    .setEsMigrationSuffixes(Collections.emptyList());

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("esMigrationSuffixes must not be empty");
        }

        @Test
        void noValidPlaceholderPrefix() {
            final ElasticsearchEvolutionConfig config = new ElasticsearchEvolutionConfig()
                    .setPlaceholderPrefix("");

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholderPrefix must not be empty");
        }

        @Test
        void noValidPlaceholderSuffix() {
            final ElasticsearchEvolutionConfig config = new ElasticsearchEvolutionConfig()
                    .setPlaceholderSuffix("");

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholderSuffix must not be empty");
        }

        @Test
        void placeholderNameMustNotContainPlaceholderSuffix() {
            final ElasticsearchEvolutionConfig config = new ElasticsearchEvolutionConfig()
                    .setPlaceholders(Collections.singletonMap("x}x", "x"));

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholder name 'x}x' must not contain placeholderSuffix '}'");
        }

        @Test
        void placeholderNameMustNotContainPlaceholderPrefix() {
            final ElasticsearchEvolutionConfig config = new ElasticsearchEvolutionConfig()
                    .setPlaceholders(Collections.singletonMap("x${x", "x"));

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholder name 'x${x' must not contain placeholderPrefix '${'");
        }

        @Test
        void placeholderValueMustNotContainPlaceholderSuffix() {
            final ElasticsearchEvolutionConfig config = new ElasticsearchEvolutionConfig()
                    .setPlaceholders(Collections.singletonMap("x", "x}x"));

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholder value 'x}x' must not contain placeholderSuffix '}'");
        }

        @Test
        void placeholderValueMustNotContainPlaceholderPrefix() {
            final ElasticsearchEvolutionConfig config = new ElasticsearchEvolutionConfig()
                    .setPlaceholders(Collections.singletonMap("x", "x${x"));

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholder value 'x${x' must not contain placeholderPrefix '${'");
        }

        @Test
        void noValidHistoryMaxQuerySize_mustBeGreaterThan0() {
            final ElasticsearchEvolutionConfig config = new ElasticsearchEvolutionConfig()
                    .setHistoryMaxQuerySize(0);

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("historyMaxQuerySize value '0' must be greater than 0");
        }

        @Test
        void baselineVersion_must_be_at_least_1() {
            final ElasticsearchEvolutionConfig config = new ElasticsearchEvolutionConfig()
                    .setBaselineVersion("0");

            assertThatThrownBy(config::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("baselineVersion '0' must be at least 1");
        }
    }
}