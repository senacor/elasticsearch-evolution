package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import com.senacor.elasticsearch.evolution.core.ElasticsearchEvolution;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;

import static java.util.Objects.requireNonNull;

/**
 * {@link InitializingBean} used to trigger {@link ElasticsearchEvolution} migration at startup.
 *
 * @author Andreas Keefer
 */
public class ElasticsearchEvolutionInitializer implements InitializingBean, Ordered {

    private final ElasticsearchEvolution elasticsearchEvolution;

    public ElasticsearchEvolutionInitializer(ElasticsearchEvolution elasticsearchEvolution) {
        this.elasticsearchEvolution = requireNonNull(elasticsearchEvolution, "elasticsearchEvolution must not be null");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        elasticsearchEvolution.migrate();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
