package com.senacor.elasticsearch.evolution.rest.abstracion;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EvolutionRestResponseTest {

    @Test
    void asString_returnsAStringRepresentation() {
        final EvolutionRestResponseDummy underTest = new EvolutionRestResponseDummy(
                200, Optional.of("OK"), Optional.of("my body"));

        assertThat(underTest.asString())
                .isEqualTo("Response{statusCode=200, statusReasonPhrase=OK}");
    }

    @Test
    void asString_returnsAStringRepresentationForEmptyOptionals() {
        final EvolutionRestResponseDummy underTest = new EvolutionRestResponseDummy(
                200, Optional.empty(), Optional.empty());

        assertThat(underTest.asString())
                .isEqualTo("Response{statusCode=200, statusReasonPhrase=null}");
    }

    private record EvolutionRestResponseDummy(int statusCode,
                                              Optional<String> statusReasonPhrase,
                                              Optional<String> body) implements EvolutionRestResponse {
    }
}