package com.senacor.elasticsearch.evolution.rest.abstracion;

import lombok.NonNull;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface EvolutionRestClient {

    String HEADER_NAME_CONTENT_TYPE = "Content-Type";
    String APPLICATION_JSON_UTF8 = "application/json; charset=UTF-8";

    /**
     * @return some information about the rest client.
     */
    default String info() {
        return this.getClass().getName();
    }

    /**
     * Execute a REST request with the given parameters
     * and returns the response as {@link EvolutionRestResponse}.
     *
     * @throws IOException if the request fails
     */
    EvolutionRestResponse execute(@NonNull HttpMethod method,
                                  @NonNull String endpoint,
                                  Map<String, String> headers,
                                  Map<String, String> urlParams,
                                  String body) throws IOException;

    /**
     * @see #execute(HttpMethod, String, Map, Map, String)
     */
    default EvolutionRestResponse execute(@NonNull HttpMethod method,
                                          @NonNull String endpoint) throws IOException {
        return execute(method, endpoint, null, null, null);
    }

    /**
     * @return The Content-Type value if available in the given headers
     */
    default Optional<String> getContentType(Map<String, String> headers) {
        if (null == headers) {
            return Optional.empty();
        }
        return headers.entrySet()
                .stream()
                .filter(entry -> HEADER_NAME_CONTENT_TYPE.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .filter(contentType -> !contentType.isBlank())
                .findFirst();
    }
}
