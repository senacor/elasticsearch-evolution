package com.senacor.elasticsearch.evolution.core.api.migration.java;

import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import lombok.NonNull;

import java.util.Collection;
import java.util.function.Function;

/**
 * A facility to obtain classes.
 *
 * @param <I> The interface the classes must implement.
 */
@FunctionalInterface
public interface ClassProvider<I> extends Function<ElasticsearchEvolutionConfig, Collection<Class<? extends I>>> {

    /**
     * Retrieve classes which implement the specified interface.
     *
     * @param config The configuration of Elasticsearch Evolution. Can be usefull to filter or select the provided classes.
     * @return The non-abstract classes that were found.
     */
    @Override
    Collection<Class<? extends I>> apply(@NonNull ElasticsearchEvolutionConfig config);
}
