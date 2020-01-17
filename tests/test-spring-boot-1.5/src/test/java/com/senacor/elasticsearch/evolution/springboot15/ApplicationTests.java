package com.senacor.elasticsearch.evolution.springboot15;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.elasticsearch.rest.uris=http://localhost:" + ApplicationTests.ELASTICSEARCH_PORT})
public class ApplicationTests {

    static final int ELASTICSEARCH_PORT = 18749;

    @Autowired
    private EsUtils esUtils;

    @Test
    public void contextLoads() throws UnknownHostException {
        esUtils.refreshIndices();

        List<String> documents = esUtils.fetchAllDocuments("test_1");

        assertThat(documents).hasSize(1);
    }

    @TestConfiguration
    static class Config {
        @Bean(destroyMethod = "stop")
        public ElasticsearchContainer elasticsearchContainer() {
            ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:6.8.6")
                    .withEnv("ES_JAVA_OPTS", "-Xms128m -Xmx128m");
            container.setPortBindings(Collections.singletonList(ELASTICSEARCH_PORT + ":9200"));
            container.start();
            return container;
        }

        @Bean
        public EsUtils esUtils(ElasticsearchContainer elasticsearchContainer) {
            return new EsUtils(RestClient.builder(HttpHost.create(elasticsearchContainer.getHttpHostAddress())).build());
        }
    }
}