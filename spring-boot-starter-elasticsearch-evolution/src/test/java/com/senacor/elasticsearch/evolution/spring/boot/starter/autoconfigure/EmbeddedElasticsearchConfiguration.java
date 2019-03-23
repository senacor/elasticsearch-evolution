package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

import java.util.concurrent.TimeUnit;

/**
 * @author Andreas Keefer
 */
public class EmbeddedElasticsearchConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedElasticsearchConfiguration.class);

    @Bean(destroyMethod = "stop")
    public EmbeddedElastic embeddedElastic() throws Exception {
        EmbeddedElastic.Builder builder = EmbeddedElastic.builder()
                .withElasticVersion("6.6.2")
                .withStartTimeout(2, TimeUnit.MINUTES)
                .withEsJavaOpts("-Xms128m -Xmx128m");
        logger.info("starting embedded ElasticSearch...");
        return builder
                .build()
                .start();
    }

    @Bean
    public RestHighLevelClient restHighLevelClient(EmbeddedElastic embeddedElastic) {
        RestClientBuilder builder = RestClient.builder(HttpHost.create("http://localhost:" + embeddedElastic.getHttpPort()));
        return new RestHighLevelClient(builder);
    }
}
