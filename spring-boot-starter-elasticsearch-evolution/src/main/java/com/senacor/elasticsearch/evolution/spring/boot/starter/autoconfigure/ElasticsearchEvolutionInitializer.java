package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import com.senacor.elasticsearch.evolution.core.ElasticsearchEvolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;

import static java.util.Objects.requireNonNull;

/**
 * {@link InitializingBean} used to trigger {@link ElasticsearchEvolution} migration at startup.
 *
 * @author Andreas Keefer
 */
public class ElasticsearchEvolutionInitializer implements InitializingBean, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchEvolutionInitializer.class);

    private final ElasticsearchEvolution elasticsearchEvolution;

    public ElasticsearchEvolutionInitializer(ElasticsearchEvolution elasticsearchEvolution) {
        this.elasticsearchEvolution = requireNonNull(elasticsearchEvolution, "elasticsearchEvolution must not be null");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        long startTimeStamp = System.currentTimeMillis();
        int sucessfullExecutedScriots = elasticsearchEvolution.migrate();
        logger.info("ElasticsearchEvolution executed successfully {} migration scripts in {} ms",
                sucessfullExecutedScriots, System.currentTimeMillis() - startTimeStamp);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
