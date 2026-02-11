package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * @author Andreas Keefer
 */
public class EmbeddedElasticsearchConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedElasticsearchConfiguration.class);

    @Bean(destroyMethod = "stop")
    public ElasticsearchContainer elasticsearchContainer() {
        ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.19.11")
                .withEnv("ES_JAVA_OPTS", "-Xms128m -Xmx128m")
                // since elasticsearch 8 security / https is enabled per default - but for testing it should be disabled
                .withEnv("xpack.security.enabled", "false");
        logger.info("starting embedded ElasticSearch...");
        container.start();
        return container;
    }

    @Bean
    public RestClient restClient(ElasticsearchContainer elasticsearchContainer) {
        RestClientBuilder builder = RestClient.builder(HttpHost.create(elasticsearchContainer.getHttpHostAddress()));
        return builder.build();
    }

    @Bean
    public EsUtils esUtils(RestClient restClient) {
        return new EsUtils(restClient);
    }
}
