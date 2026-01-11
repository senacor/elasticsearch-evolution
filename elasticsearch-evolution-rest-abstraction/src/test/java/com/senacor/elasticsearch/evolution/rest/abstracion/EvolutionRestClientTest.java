package com.senacor.elasticsearch.evolution.rest.abstracion;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EvolutionRestClientTest {

    private final EvolutionRestClient underTest = (method, endpoint, headers, urlParams, body) -> {
        throw new UnsupportedOperationException("not yet implemented");
    };

    @Test
    void info_should_return_className() {
        assertThat(underTest.info())
                .isEqualTo(underTest.getClass().getName());
    }

    @Nested
    class GetContentType {

        @Test
        void whenHeadersIsNull_thenReturnEmptyOptional() {
            final Optional<String> contentType = underTest.getContentType(null);

            assertThat(contentType)
                    .isEmpty();
        }

        @Test
        void whenHeadersDoesNotContainContentType_thenReturnEmptyOptional() {
            final Map<String, String> headers = Map.of(
                    "Some-Header", "some-value"
            );

            final Optional<String> contentType = underTest.getContentType(headers);

            assertThat(contentType)
                    .isEmpty();
        }

        @Test
        void whenHeadersContainContentType_thenReturnContentTypeValue() {
            final Map<String, String> headers = Map.of(
                    "Some-Header", "some-value",
                    EvolutionRestClient.HEADER_NAME_CONTENT_TYPE, "application/json"
            );

            final Optional<String> contentType = underTest.getContentType(headers);

            assertThat(contentType)
                    .contains("application/json");
        }

        @Test
        void whenHeadersContainContentTypeIgnoreCase_thenReturnContentTypeValue() {
            final Map<String, String> headers = Map.of(
                    "Some-Header", "some-value",
                    EvolutionRestClient.HEADER_NAME_CONTENT_TYPE.toLowerCase(), "application/json"
            );

            final Optional<String> contentType = underTest.getContentType(headers);

            assertThat(contentType)
                    .contains("application/json");
        }

        @Test
        void whenHeadersContainBlankContentType_thenReturnEmptyOptional() {
            final Map<String, String> headers = Map.of(
                    "Some-Header", "some-value",
                    EvolutionRestClient.HEADER_NAME_CONTENT_TYPE, " "
            );

            final Optional<String> contentType = underTest.getContentType(headers);

            assertThat(contentType)
                    .isEmpty();
        }

        @Test
        void whenHeadersContainNullContentType_thenReturnEmptyOptional() {
            final Map<String, String> headers = new HashMap<>();
            headers.put(EvolutionRestClient.HEADER_NAME_CONTENT_TYPE, null);


            final Optional<String> contentType = underTest.getContentType(headers);

            assertThat(contentType)
                    .isEmpty();
        }
    }
}