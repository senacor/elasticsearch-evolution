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
            assertThatThrownBy(() -> new ElasticsearchEvolutionConfig().validate().setEncoding(null).validate())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("encoding must not be null");
        }

        @Test
        void noValidPlaceholders() {
            assertThatThrownBy(() -> new ElasticsearchEvolutionConfig().validate().setPlaceholders(null).validate())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("placeholders must not be null");
        }

        @Test
        void noValidLocations() {
            assertThatThrownBy(() -> new ElasticsearchEvolutionConfig().validate().setLocations(Collections.emptyList()).validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("locations must not be empty");
        }

        @Test
        void noValidEsMigrationPrefix() {
            assertThatThrownBy(() -> new ElasticsearchEvolutionConfig().validate().setEsMigrationPrefix("").validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("esMigrationPrefix must not be empty");
        }

        @Test
        void noValidEsMigrationSuffixes() {
            assertThatThrownBy(() -> new ElasticsearchEvolutionConfig().validate().setEsMigrationSuffixes(Collections.emptyList()).validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("esMigrationSuffixes must not be empty");
        }

        @Test
        void noValidPlaceholderPrefix() {
            assertThatThrownBy(() -> new ElasticsearchEvolutionConfig().validate().setPlaceholderPrefix("").validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholderPrefix must not be empty");
        }

        @Test
        void noValidPlaceholderSuffix() {
            assertThatThrownBy(() -> new ElasticsearchEvolutionConfig().validate().setPlaceholderSuffix("").validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholderSuffix must not be empty");
        }

        @Test
        void placeholderNameMustNotContainPlaceholderSuffix() {
            assertThatThrownBy(() -> new ElasticsearchEvolutionConfig().validate()
                    .setPlaceholders(Collections.singletonMap("x}x", "x"))
                    .validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholder name 'x}x' must not contain placeholderSuffix '}'");
        }

        @Test
        void placeholderNameMustNotContainPlaceholderPrefix() {
            assertThatThrownBy(() -> new ElasticsearchEvolutionConfig().validate()
                    .setPlaceholders(Collections.singletonMap("x${x", "x"))
                    .validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholder name 'x${x' must not contain placeholderPrefix '${'");
        }

        @Test
        void placeholderValueMustNotContainPlaceholderSuffix() {
            assertThatThrownBy(() -> new ElasticsearchEvolutionConfig().validate()
                    .setPlaceholders(Collections.singletonMap("x", "x}x"))
                    .validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholder value 'x}x' must not contain placeholderSuffix '}'");
        }

        @Test
        void placeholderValueMustNotContainPlaceholderPrefix() {
            assertThatThrownBy(() -> new ElasticsearchEvolutionConfig().validate()
                    .setPlaceholders(Collections.singletonMap("x", "x${x"))
                    .validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("placeholder value 'x${x' must not contain placeholderPrefix '${'");
        }
    }
}