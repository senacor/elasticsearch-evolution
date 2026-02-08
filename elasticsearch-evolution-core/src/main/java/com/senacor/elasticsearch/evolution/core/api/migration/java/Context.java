package com.senacor.elasticsearch.evolution.core.api.migration.java;

import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import com.senacor.elasticsearch.evolution.rest.abstraction.EvolutionRestClient;
import lombok.NonNull;
import lombok.Value;

/**
 * The context relevant to a Java-based migration.
 */
@Value(staticConstructor = "of")
public class Context {

    /**
     * The configuration currently in use.
     */
    @NonNull
    ElasticsearchEvolutionConfig configuration;

    /**
     * The EvolutionRestClient to use for communicating with Elasticsearch.
     */
    @NonNull
    EvolutionRestClient<?> evolutionRestClient;
}
