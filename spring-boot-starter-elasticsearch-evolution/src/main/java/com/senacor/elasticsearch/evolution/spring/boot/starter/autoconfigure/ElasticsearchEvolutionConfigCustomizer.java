package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import com.senacor.elasticsearch.evolution.core.ElasticsearchEvolution;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfigImpl;

import java.util.function.Consumer;

/**
 * Extension point for customizing the {@link ElasticsearchEvolutionConfig} used by the auto-configured {@link ElasticsearchEvolution} bean.
 */
@FunctionalInterface
public interface ElasticsearchEvolutionConfigCustomizer extends Consumer<ElasticsearchEvolutionConfigImpl> {
}
