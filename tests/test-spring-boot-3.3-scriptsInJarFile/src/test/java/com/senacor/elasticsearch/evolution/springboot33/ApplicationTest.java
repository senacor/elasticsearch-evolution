package com.senacor.elasticsearch.evolution.springboot33;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"spring.elasticsearch.uris=http://localhost:" + ApplicationTest.ELASTICSEARCH_PORT})
class ApplicationTest {

    static final int ELASTICSEARCH_PORT = 18771;

    @Autowired
    private EsUtils esUtils;

    @Test
    void contextLoads() {
        esUtils.refreshIndices();

        List<String> documents = esUtils.fetchAllDocuments("test_1");

        assertThat(documents).hasSize(1);
    }

    @TestConfiguration
    static class Config {
        @Bean(destroyMethod = "stop")
        public ElasticsearchContainer elasticsearchContainer(@Value("${elasticsearch.version:7.5.2}") String esVersion) {
            ElasticsearchContainer container = new ElasticsearchContainer(DockerImageName
                    .parse("docker.elastic.co/elasticsearch/elasticsearch")
                    .withTag(esVersion)) {
                @Override
                protected void containerIsStarted(InspectContainerResponse containerInfo) {
                    // since testcontainers 1.17 it detects if ES 8.x is running and copies a certificate in this case
                    // but we don't want security
                }
            }
                    .withEnv("ES_JAVA_OPTS", "-Xms128m -Xmx128m")
                    // since elasticsearch 8 security / https is enabled per default - but for testing it should be disabled
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("cluster.routing.allocation.disk.watermark.low", "97%")
                    .withEnv("cluster.routing.allocation.disk.watermark.high", "98%")
                    .withEnv("cluster.routing.allocation.disk.watermark.flood_stage", "99%");
            container.setPortBindings(Collections.singletonList(ELASTICSEARCH_PORT + ":9200"));
            container.start();
            return container;
        }

        @Bean
        public EsUtils esUtils(ElasticsearchContainer elasticsearchContainer) {
            return new EsUtils(RestClient.create("http://" + elasticsearchContainer.getHttpHostAddress()));
        }
    }
}