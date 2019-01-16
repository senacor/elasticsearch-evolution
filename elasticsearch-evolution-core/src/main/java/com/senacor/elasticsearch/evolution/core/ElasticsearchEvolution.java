package com.senacor.elasticsearch.evolution.core;

import org.elasticsearch.client.RestHighLevelClient;

import static java.util.Objects.requireNonNull;

/**
 * @author Andreas Keefer
 */
public class ElasticsearchEvolution {

    private final ElasticsearchEvolutionProperties elasticsearchEvolutionProperties;
    private final RestHighLevelClient restHighLevelClient;

    /**
     * @param elasticsearchEvolutionProperties configuration properties
     * @param restHighLevelClient              REST client to call with Elasticsearch
     */
    public ElasticsearchEvolution(ElasticsearchEvolutionProperties elasticsearchEvolutionProperties,
                                  RestHighLevelClient restHighLevelClient) {
        this.elasticsearchEvolutionProperties = requireNonNull(elasticsearchEvolutionProperties, "elasticsearchEvolutionProperties must not be null");
        this.restHighLevelClient = requireNonNull(restHighLevelClient, "restHighLevelClient must not be null");
    }

    /**
     * start migration
     */
    public void migrate() {
        System.out.println("migrate...");
        // TODO (ak) impl
    }
}
