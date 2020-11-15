package com.senacor.elasticsearch.evolution.springboot23;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"spring.elasticsearch.rest.uris=http://localhost:" + ApplicationTests.ELASTICSEARCH_PORT})
public class ApplicationTests {

    static final int ELASTICSEARCH_PORT = 18761;

    @Autowired
    private EsUtils esUtils;

    @Test
    public void contextLoads() {
        esUtils.refreshIndices();

        List<String> documents = esUtils.fetchAllDocuments("test_1");

        assertThat(documents).hasSize(1);
    }

    @TestConfiguration
    static class Config {
        @Bean(destroyMethod = "stop")
        public ElasticsearchContainer elasticsearchContainer(@Value("${elasticsearch.version:7.5.2}") String esVersion) {
            ElasticsearchContainer container = new ElasticsearchContainer(DockerImageName
                    .parse("docker.elastic.co/elasticsearch/elasticsearch-oss")
                    .withTag(esVersion)
            ).withEnv("ES_JAVA_OPTS", "-Xms128m -Xmx128m");
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