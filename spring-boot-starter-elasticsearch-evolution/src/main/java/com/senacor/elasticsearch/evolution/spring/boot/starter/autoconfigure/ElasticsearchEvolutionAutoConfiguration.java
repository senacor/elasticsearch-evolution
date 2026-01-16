package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import com.senacor.elasticsearch.evolution.core.ElasticsearchEvolution;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestClient;
import com.senacor.elasticsearch.evolution.rest.abstracion.esclient.EvolutionESRestClient;
import com.senacor.elasticsearch.evolution.rest.abstracion.os.genericclient.EvolutionOpenSearchGenericClient;
import com.senacor.elasticsearch.evolution.rest.abstracion.os.restclient.EvolutionOpenSearchRestClient;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.TransportOptions;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for ElasticsearchEvolution
 *
 * @author Andreas Keefer
 */
@AutoConfiguration(
        beforeName = "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration", // since spring-boot 1.5
        afterName = {
                "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration", // spring-boot 2.3+
                // if org.opensearch.client:spring-data-opensearch-starter is used, it should be preferred over this
                "org.opensearch.spring.boot.autoconfigure.OpenSearchClientAutoConfiguration",
                "org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration"
        })
@ConditionalOnProperty(prefix = "spring.elasticsearch.evolution", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(ElasticsearchEvolution.class)
@EnableConfigurationProperties({ElasticsearchEvolutionConfig.class})
public class ElasticsearchEvolutionAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchEvolutionAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(EvolutionRestClient.class)
    public ElasticsearchEvolution elasticsearchEvolution(ElasticsearchEvolutionConfig elasticsearchEvolutionConfig,
                                                         EvolutionRestClient restClient) {
        return new ElasticsearchEvolution(elasticsearchEvolutionConfig, restClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchEvolutionInitializer elasticsearchEvolutionInitializer(ElasticsearchEvolution elasticsearchEvolution) {
        return new ElasticsearchEvolutionInitializer(elasticsearchEvolution);
    }

    @Configuration
    @ConditionalOnClass({RestClient.class, EvolutionESRestClient.class})
    public static class ElasticsearchRestClientConfiguration {

        /**
         * @return default RestClientBuilder if {@link org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration} is not used
         * and no RestClient is available.
         */
        @Bean
        @ConditionalOnMissingBean({RestClientBuilder.class, RestClient.class})
        public RestClientBuilder elasticRestClientBuilder(
                @Value("${spring.elasticsearch.uris:http://localhost:9200}") String[] uris) {
            final List<String> urisList;
            if (uris != null && uris.length > 0) {
                urisList = Arrays.asList(uris);
            } else {
                throw new IllegalStateException("spring configuration 'spring.elasticsearch.uris' does not exist");
            }

            logger.info("creating Elasticsearch RestClientBuilder with uris {}", urisList);

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
            logger.info("creating Elasticsearch RestClient from {}", restClientBuilder);
            return restClientBuilder.build();
        }

        @Bean
        @ConditionalOnBean(RestClient.class)
        @ConditionalOnMissingBean(EvolutionRestClient.class)
        public EvolutionESRestClient evolutionESRestClient(RestClient restClient) {
            logger.info("creating EvolutionESRestClient from {}", restClient);
            return new EvolutionESRestClient(restClient);
        }
    }

    @Configuration
    @ConditionalOnClass({org.opensearch.client.RestClient.class, EvolutionOpenSearchRestClient.class})
    public static class OpensearchRestClientConfiguration {

        @Bean
        @ConditionalOnMissingBean({
                org.opensearch.client.RestClientBuilder.class,
                org.opensearch.client.RestClient.class})
        public org.opensearch.client.RestClientBuilder opensearchRestClientBuilder(
                @Value("${opensearch.uris:http://localhost:9200}") String[] uris) {
            final List<String> urisList;
            if (uris != null && uris.length > 0) {
                urisList = Arrays.asList(uris);
            } else {
                throw new IllegalStateException("spring configuration 'opensearch.uris' does not exist");
            }

            logger.info("creating OpenSearch RestClientBuilder with uris {}", urisList);

            HttpHost[] httpHosts = urisList.stream()
                    .map(HttpHost::create)
                    .toArray(HttpHost[]::new);
            return org.opensearch.client.RestClient.builder(httpHosts);
        }

        @Bean
        @ConditionalOnBean(org.opensearch.client.RestClientBuilder.class)
        @ConditionalOnMissingBean
        public org.opensearch.client.RestClient opensearchRestClient(org.opensearch.client.RestClientBuilder restClientBuilder) {
            logger.info("creating OpenSearch RestClient from {}", restClientBuilder);
            return restClientBuilder.build();
        }

        @Bean
        @ConditionalOnBean(org.opensearch.client.RestClient.class)
        @ConditionalOnMissingBean(EvolutionRestClient.class)
        public EvolutionOpenSearchRestClient evolutionOpenSearchRestClient(org.opensearch.client.RestClient restClient) {
            logger.info("creating EvolutionOpenSearchRestClient from {}", restClient);
            return new EvolutionOpenSearchRestClient(restClient);
        }
    }

    @Configuration
    @ConditionalOnClass({OpenSearchGenericClient.class, EvolutionOpenSearchGenericClient.class})
    public static class OpenSearchGenericClientConfiguration {

        @Bean
        @ConditionalOnMissingBean({
                OpenSearchGenericClient.class,
                OpenSearchClient.class,
                OpenSearchTransport.class})
        public OpenSearchGenericClient openSearchGenericClientFromUris(
                @Value("${opensearch.uris:http://localhost:9200}") String[] uris) {
            final List<String> urisList;
            if (uris != null && uris.length > 0) {
                urisList = Arrays.asList(uris);
            } else {
                throw new IllegalStateException("spring configuration 'opensearch.uris' does not exist");
            }

            logger.info("creating OpenSearchGenericClient with uris {}", urisList);

            org.apache.hc.core5.http.HttpHost[] httpHosts = urisList.stream()
                    .map(opensearchUri -> {
                        try {
                            return org.apache.hc.core5.http.HttpHost.create(opensearchUri);
                        } catch (URISyntaxException e) {
                            throw new IllegalArgumentException("OpenSearch uri '%s' is invalid".formatted(opensearchUri), e);
                        }
                    })
                    .toArray(org.apache.hc.core5.http.HttpHost[]::new);

            OpenSearchTransport openSearchTransport = ApacheHttpClient5TransportBuilder.builder(httpHosts)
                    .build();

            return new OpenSearchGenericClient(openSearchTransport);
        }

        @Bean
        @ConditionalOnMissingBean({
                OpenSearchGenericClient.class,
                OpenSearchClient.class})
        @ConditionalOnBean(OpenSearchTransport.class)
        public OpenSearchGenericClient openSearchGenericClientFromOpenSearchTransport(OpenSearchTransport openSearchTransport,
                                                                                      @Autowired(required = false) TransportOptions transportOptions) {
            logger.info("creating OpenSearchGenericClient from '{}' and '{}'", openSearchTransport, transportOptions);
            return new OpenSearchGenericClient(openSearchTransport, transportOptions);
        }

        @Bean
        @ConditionalOnBean(OpenSearchGenericClient.class)
        @ConditionalOnMissingBean(EvolutionRestClient.class)
        public EvolutionOpenSearchGenericClient evolutionOpenSearchGenericClientFromOpenSearchGenericClient(OpenSearchGenericClient openSearchGenericClient) {
            logger.info("creating EvolutionOpenSearchGenericClient from {}", openSearchGenericClient);
            return new EvolutionOpenSearchGenericClient(openSearchGenericClient);
        }

        @Bean
        @ConditionalOnBean(OpenSearchClient.class)
        @ConditionalOnMissingBean(EvolutionRestClient.class)
        public EvolutionOpenSearchGenericClient evolutionOpenSearchGenericClientFromOpenSearchClient(OpenSearchClient openSearchClient) {
            logger.info("creating EvolutionOpenSearchGenericClient from {}", openSearchClient);
            return new EvolutionOpenSearchGenericClient(openSearchClient);
        }
    }
}
