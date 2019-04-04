package com.senacor.elasticsearch.evolution.springboot20;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.elasticsearch.rest.uris=http://localhost:" + ApplicationTests.ELASTICSEARCH_PORT})
public class ApplicationTests {

    static final int ELASTICSEARCH_PORT = 18759;

    @Autowired
    private EmbeddedElastic embeddedElastic;

    @Test
    public void contextLoads() throws UnknownHostException {
        embeddedElastic.refreshIndices();

        List<String> documents = embeddedElastic.fetchAllDocuments("test_1");

        assertThat(documents).hasSize(1);
    }

    @TestConfiguration
    static class Config {
        @Bean
        public EmbeddedElastic embeddedElastic() throws Exception {
            EmbeddedElastic.Builder builder = EmbeddedElastic.builder()
                    .withElasticVersion("6.7.1")
                    .withStartTimeout(2, TimeUnit.MINUTES)
                    .withSetting(PopularProperties.HTTP_PORT, ELASTICSEARCH_PORT)
                    .withEsJavaOpts("-Xms128m -Xmx128m");
            return builder
                    .build()
                    .start();
        }
    }
}

