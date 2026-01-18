package com.senacor.elasticsearch.evolution.springboot35;

import org.junit.jupiter.api.Test;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(properties = {"opensearch.uris=http://localhost:" + ApplicationTests.OPENSEARCH_PORT})
class ApplicationTests {

    static final int OPENSEARCH_PORT = 18775;

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
        public OpenSearchContainer<?> opensearchContainer(@Value("${opensearch.version:2.19.4}") String osVersion) {
            OpenSearchContainer<?> container = new OpenSearchContainer<>(DockerImageName
                    .parse("quay.io/xtermi2/opensearch")
                    .asCompatibleSubstituteFor("opensearchproject/opensearch")
                    .withTag(osVersion));
            container.withEnv("OPENSEARCH_JAVA_OPTS", "-Xms196m -Xmx196m")
                    .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
                    .withEnv("cluster.routing.allocation.disk.watermark.low", "97%")
                    .withEnv("cluster.routing.allocation.disk.watermark.high", "98%")
                    .withEnv("cluster.routing.allocation.disk.watermark.flood_stage", "99%")
                    .setPortBindings(List.of(OPENSEARCH_PORT + ":9200"));
            container.start();
            return container;
        }

        @Bean
        public EsUtils esUtils(OpenSearchContainer<?> openSearchContainer) {
            return new EsUtils(RestClient.create(openSearchContainer.getHttpHostAddress()));
        }
    }
}