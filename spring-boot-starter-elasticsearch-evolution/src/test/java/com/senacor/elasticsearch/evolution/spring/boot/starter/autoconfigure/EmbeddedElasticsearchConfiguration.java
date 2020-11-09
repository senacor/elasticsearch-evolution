package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
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
        ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:6.8.13")
                .withEnv("ES_JAVA_OPTS", "-Xms128m -Xmx128m");
        logger.info("starting embedded ElasticSearch...");
        container.start();
        return container;
    }

    @Bean
    public RestHighLevelClient restHighLevelClient(ElasticsearchContainer elasticsearchContainer) {
        RestClientBuilder builder = RestClient.builder(HttpHost.create(elasticsearchContainer.getHttpHostAddress()));
        return new RestHighLevelClient(builder);
    }

    @Bean
    public EsUtils esUtils(RestHighLevelClient restHighLevelClient) {
        return new EsUtils(restHighLevelClient.getLowLevelClient());
    }
}
