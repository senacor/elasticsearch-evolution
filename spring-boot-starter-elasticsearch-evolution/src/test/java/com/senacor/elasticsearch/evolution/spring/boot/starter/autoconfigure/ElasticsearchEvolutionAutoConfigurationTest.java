package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import com.senacor.elasticsearch.evolution.core.ElasticsearchEvolution;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andreas Keefer
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ElasticsearchEvolutionAutoConfiguration.class)
class ElasticsearchEvolutionAutoConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void whenSpringContextIsBootstrapped_thenNoExceptions() {
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
}