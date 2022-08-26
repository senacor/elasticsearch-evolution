package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import com.senacor.elasticsearch.evolution.core.ElasticsearchEvolution;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
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
import java.util.concurrent.TimeUnit;

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

        @Value("${spring.elasticsearch.rest.username:}")
        private String username ;

        @Value("${spring.elasticsearch.rest.password:}")
        private String password;

        @Value("${spring.elasticsearch.rest.connectTimeout:5000}")
        private int connectTimeout ;

        @Value("${spring.elasticsearch.rest.socketTimeout:300000}")
        private int socketTimeout ;

        @Value("${spring.elasticsearch.rest.connectionRequestTimeout:3000}")
        private int connectionRequestTimeout;

        @Value("${spring.elasticsearch.rest.maxRetryTimeoutMillis:300000}")
        private int maxRetryTimeoutMillis;

        /**
         * @return default RestHighLevelClient if {@link org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration} is not used
         * and no RestHighLevelClient is available.
         */
        @Bean
        @ConditionalOnMissingBean
        public RestHighLevelClient restHighLevelClient(@Value("${spring.elasticsearch.rest.uris:http://localhost:9200}") String... uris) {
            /**
            HttpHost[] httpHosts = Arrays.stream(uris)
                    .map(HttpHost::create)
                    .toArray(HttpHost[]::new);
            RestClientBuilder builder = RestClient.builder(httpHosts);
            return new RestHighLevelClient(builder);
             **/
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            HttpHost[] httpHostArr = Arrays.stream(uris)
                    .map(HttpHost::create)
                    .toArray(HttpHost[]::new);;

            RestClientBuilder builder = RestClient.builder(httpHostArr).setHttpClientConfigCallback((HttpAsyncClientBuilder httpClientBuilder) ->{
                httpClientBuilder.setKeepAliveStrategy((response, context) -> {
                    return TimeUnit.MINUTES.toMillis(5L);
                });
                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }).setRequestConfigCallback((RequestConfig.Builder requestConfigBuilder) -> {
                requestConfigBuilder.setConnectTimeout(connectTimeout);
                requestConfigBuilder.setSocketTimeout(socketTimeout);
                requestConfigBuilder.setConnectionRequestTimeout(connectionRequestTimeout);
                return requestConfigBuilder;
            }).setMaxRetryTimeoutMillis(maxRetryTimeoutMillis);
            Header[] defaultHeaders = new Header[]{new BasicHeader("Content-Type", "application/json")};
            builder.setDefaultHeaders(defaultHeaders);
            RestHighLevelClient client = new RestHighLevelClient(builder);
            return client;
        }

    }
}
