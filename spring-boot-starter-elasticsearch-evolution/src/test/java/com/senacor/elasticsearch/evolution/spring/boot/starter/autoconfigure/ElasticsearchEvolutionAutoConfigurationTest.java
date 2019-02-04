package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import com.senacor.elasticsearch.evolution.core.ElasticsearchEvolution;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andreas Keefer
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ElasticsearchEvolutionAutoConfiguration.class, ElasticsearchEvolutionAutoConfigurationTest.Config.class})
class ElasticsearchEvolutionAutoConfigurationTest {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchEvolutionAutoConfigurationTest.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void whenSpringContextIsBootstrapped_thenNoExceptions() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void restHighLevelClient_existsAsBean() {
        assertThat(applicationContext.getBean(RestHighLevelClient.class)).isNotNull();
    }

    @Test
    void elasticsearchEvolution_existsAsBean() {
        assertThat(applicationContext.getBean(ElasticsearchEvolution.class)).isNotNull();
    }

    @Test
    void elasticsearchEvolutionInitializer_existsAsBean() {
        assertThat(applicationContext.getBean(ElasticsearchEvolutionInitializer.class)).isNotNull();
    }

    @TestConfiguration
    public static class Config {
        @Bean
        public ElasticsearchEvolutionInitializer elasticsearchEvolutionInitializer(ElasticsearchEvolution elasticsearchEvolution) {
            return new ElasticsearchEvolutionInitializer(elasticsearchEvolution) {
                @Override
                public void afterPropertiesSet() throws Exception {
                    logger.info("don't migrate!");
                }
            };
        }
    }
}