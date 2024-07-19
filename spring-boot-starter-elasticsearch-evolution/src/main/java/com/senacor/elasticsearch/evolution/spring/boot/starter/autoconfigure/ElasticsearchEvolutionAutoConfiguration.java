package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import com.senacor.elasticsearch.evolution.core.ElasticsearchEvolution;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for ElasticsearchEvolution
 *
 * @author Andreas Keefer
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.elasticsearch.evolution", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(ElasticsearchEvolution.class)
@EnableConfigurationProperties({ElasticsearchEvolutionConfig.class})
@AutoConfigureBefore(name = {
        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration" // since spring-boot 1.5
})
@AutoConfigureAfter(name = {
        "org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientAutoConfiguration", // spring-boot 2.1 / 2.2
        "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration" // spring-boot 2.3+
})
public class ElasticsearchEvolutionAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchEvolutionAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RestClient.class)
    public ElasticsearchEvolution elasticsearchEvolution(ElasticsearchEvolutionConfig elasticsearchEvolutionConfig,
                                                         RestClient restClient) {
        return new ElasticsearchEvolution(elasticsearchEvolutionConfig, restClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchEvolutionInitializer elasticsearchEvolutionInitializer(ElasticsearchEvolution elasticsearchEvolution) {
        return new ElasticsearchEvolutionInitializer(elasticsearchEvolution);
    }

    @Configuration
    @ConditionalOnClass(RestClient.class)
    public static class RestClientConfiguration {

        /**
         * @return default RestClientBuilder if {@link org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration} is not used
         * and no RestClient is available.
         */
        @Bean
        @ConditionalOnMissingBean({RestClientBuilder.class, RestClient.class})
        public RestClientBuilder restClientBuilder(
                @Value("${spring.elasticsearch.rest.uris:http://localhost:9200}") String[] urisDeprecated,
                @Value("${spring.elasticsearch.uris:}") String[] uris) {
            final List<String> urisList;
            if (uris != null && uris.length > 0) {
                // prefer the new spring-boot (since 2.6) config properties
                urisList = Arrays.asList(uris);
            } else if (urisDeprecated != null && urisDeprecated.length > 0) {
                // fallback to old deprecated spring-boot config properties
                urisList = Arrays.asList(urisDeprecated);
            } else {
                throw new IllegalStateException("spring configuration 'spring.elasticsearch.uris' does not exist");
            }

            logger.info("creating RestClientBuilder with uris {}", urisList);

            HttpHost[] httpHosts = urisList.stream()
                    .map(HttpHost::create)
                    .toArray(HttpHost[]::new);
            return RestClient.builder(httpHosts);
        }

        /**
         * @return default RestClient no RestClient is available.
         */
        @Bean
        @ConditionalOnBean(RestClientBuilder.class)
        @ConditionalOnMissingBean
        public RestClient elasticRestClient(RestClientBuilder restClientBuilder) {
            logger.info("creating RestClient from {}", restClientBuilder);
            return restClientBuilder.build();
        }
    }
}
