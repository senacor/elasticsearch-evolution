package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import com.senacor.elasticsearch.evolution.core.ElasticsearchEvolution;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
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
        "org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientAutoConfiguration" // since spring-boot 2.1
})
public class ElasticsearchEvolutionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RestHighLevelClient.class)
    public ElasticsearchEvolution elasticsearchEvolution(ElasticsearchEvolutionConfig elasticsearchEvolutionConfig,
                                                         RestHighLevelClient restHighLevelClient) {
        return new ElasticsearchEvolution(elasticsearchEvolutionConfig, restHighLevelClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchEvolutionInitializer elasticsearchEvolutionInitializer(ElasticsearchEvolution elasticsearchEvolution) {
        return new ElasticsearchEvolutionInitializer(elasticsearchEvolution);
    }

    @Configuration
    @ConditionalOnClass(RestHighLevelClient.class)
    public static class RestHighLevelClientConfiguration {

        /**
         * @return default RestHighLevelClient if {@link org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration} is not used
         * and no RestHighLevelClient is available.
         */
        @Bean
        @ConditionalOnMissingBean
        public RestHighLevelClient restHighLevelClient(@Value("${spring.elasticsearch.rest.uris:http://localhost:9200}") String... uris) {
            HttpHost[] httpHosts = Arrays.stream(uris)
                    .map(HttpHost::create)
                    .toArray(HttpHost[]::new);
            RestClientBuilder builder = RestClient.builder(httpHosts);
            return new RestHighLevelClient(builder);
        }

    }
}
