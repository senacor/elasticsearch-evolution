package com.senacor.elasticsearch.evolution.rest.abstraction;

import java.util.Arrays;
import java.util.Objects;

public enum HttpMethod {
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        OPTIONS,
        PATCH;

        public static HttpMethod create(String method) throws IllegalArgumentException {
            String normalizedMethod = Objects.requireNonNull(method, "method must not be null")
                    .toUpperCase()
                    .trim();
            return Arrays.stream(values())
                    .filter(m -> m.name().equals(normalizedMethod))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                    "Method '%s' not supported, only %s is supported.".formatted(
                    method, Arrays.toString(values()))));
        }
    }