package com.senacor.elasticsearch.evolution.rest.abstraction;

import lombok.NonNull;

import java.util.Optional;

public record EvolutionRestResponseImpl(int statusCode,
                                        @NonNull Optional<String> statusReasonPhrase,
                                        @NonNull Optional<String> body) implements EvolutionRestResponse {
}
