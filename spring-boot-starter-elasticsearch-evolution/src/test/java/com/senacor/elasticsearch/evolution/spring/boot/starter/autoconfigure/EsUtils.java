package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;

import java.io.IOException;

/**
 * @author Andreas Keefer
 */
public class EsUtils {

    private final RestClient restClient;

    public EsUtils(RestClient restClient) {
        this.restClient = restClient;
    }

    public void refreshIndices() {
        try {
            restClient.performRequest(new Request("GET", "/_refresh"));
        } catch (IOException e) {
            throw new IllegalStateException("refreshIndices failed", e);
        }
    }
}

