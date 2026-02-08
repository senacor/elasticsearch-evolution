package com.senacor.elasticsearch.evolution.rest.abstraction;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EvolutionRestResponseTest {

    @Test
    void asString_returnsAStringRepresentation() {
        final EvolutionRestResponse underTest = new EvolutionRestResponseImpl(
                200, Optional.of("OK"), Optional.of("my body"));

        assertThat(underTest.asString())
                .isEqualTo("Response{statusCode=200, statusReasonPhrase=OK}");
    }

    @Test
    void asString_returnsAStringRepresentationForEmptyOptionals() {
        final EvolutionRestResponse underTest = new EvolutionRestResponseImpl(
                200, Optional.empty(), Optional.empty());

        assertThat(underTest.asString())
                .isEqualTo("Response{statusCode=200, statusReasonPhrase=null}");
    }
}