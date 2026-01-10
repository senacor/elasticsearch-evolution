package com.senacor.elasticsearch.evolution.rest.abstracion;

import java.io.IOException;
import java.util.Optional;

public interface EvolutionRestResponse {

    /**
     * @return HTTP status code
     */
    int statusCode();

    /**
     * @return the associated textual phrase for the status code
     */
    Optional<String> statusReasonPhrase();

    /**
     * @return the response body as string
     * @throws IOException if an error occurs reading the body
     */
    Optional<String> body() throws IOException;

    default String asString() {
        return "Response{statusCode=" + statusCode() +
                ", statusReasonPhrase=" + statusReasonPhrase().orElse(null) + "}";
    }
}
