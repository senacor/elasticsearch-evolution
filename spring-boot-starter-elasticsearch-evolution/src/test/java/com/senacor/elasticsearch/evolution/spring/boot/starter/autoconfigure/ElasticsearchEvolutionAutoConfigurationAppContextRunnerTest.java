package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import com.senacor.elasticsearch.evolution.core.ElasticsearchEvolution;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestClient;
import com.senacor.elasticsearch.evolution.rest.abstracion.esclient.EvolutionESRestClient;
import com.senacor.elasticsearch.evolution.rest.abstracion.os.genericclient.EvolutionOpenSearchGenericClient;
import com.senacor.elasticsearch.evolution.rest.abstracion.os.restclient.EvolutionOpenSearchRestClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ElasticsearchEvolutionAutoConfiguration} using {@link ApplicationContextRunner}.
 * This tests all the conditional bean creation scenarios.
 *
 * @author Andreas Keefer
 */
class ElasticsearchEvolutionAutoConfigurationAppContextRunnerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ElasticsearchEvolutionAutoConfiguration.class))
            // to prevent elasticsearch evolution migration execution during tests
            .withUserConfiguration(CustomInitializerConfig.class);

    @Test
    void whenEnabledPropertyIsTrue_thenAutoConfigurationIsActive() {
        contextRunner
                .withPropertyValues("spring.elasticsearch.evolution.enabled=true")
                .withUserConfiguration(MockRestClientConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ElasticsearchEvolutionConfig.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                });
    }

    @Test
    void whenEnabledPropertyIsMissing_thenAutoConfigurationIsActiveByDefault() {
        contextRunner
                .withUserConfiguration(MockRestClientConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ElasticsearchEvolutionConfig.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                });
    }

    @Test
    void whenEnabledPropertyIsFalse_thenAutoConfigurationIsNotActive() {
        contextRunner
                .withPropertyValues("spring.elasticsearch.evolution.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ElasticsearchEvolutionConfig.class)
                            .doesNotHaveBean(ElasticsearchEvolution.class);
                });
    }

    @Test
    void whenCustomElasticsearchEvolutionExists_thenItIsUsed() {
        contextRunner
                .withUserConfiguration(CustomElasticsearchEvolutionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ElasticsearchEvolution.class);
                    assertThat(context.getBean(ElasticsearchEvolution.class))
                            .isSameAs(context.getBean(CustomElasticsearchEvolutionConfig.class).elasticsearchEvolution);
                });
    }

    // ========== Elasticsearch RestClient Configuration Tests ==========

    @Test
    void whenEvolutionRestClientIsMissing_thenADefautOneIsCreatedAndAlsoElasticsearchEvolution() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(EvolutionOpenSearchGenericClient.class, EvolutionOpenSearchRestClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ElasticsearchEvolutionConfig.class)
                            .hasSingleBean(RestClient.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                });
    }

    @Test
    void whenElasticsearchRestClientExists_thenEvolutionESRestClientIsCreated() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(EvolutionOpenSearchGenericClient.class, EvolutionOpenSearchRestClient.class))
                .withUserConfiguration(ElasticsearchRestClientConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(RestClient.class)
                            .hasSingleBean(EvolutionESRestClient.class)
                            .hasSingleBean(EvolutionRestClient.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                });
    }

    @Test
    void whenElasticsearchRestClientBuilderExists_thenRestClientIsCreated() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(EvolutionOpenSearchGenericClient.class, EvolutionOpenSearchRestClient.class))
                .withUserConfiguration(ElasticsearchRestClientBuilderConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(RestClientBuilder.class)
                            .hasSingleBean(RestClient.class)
                            .hasSingleBean(EvolutionRestClient.class)
                            .hasSingleBean(EvolutionESRestClient.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                });
    }

    @Test
    void whenNoElasticsearchRestClientOrBuilderExists_andUrisAreProvided_thenDefaultRestClientIsCreated() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(EvolutionOpenSearchGenericClient.class, EvolutionOpenSearchRestClient.class))
                .withPropertyValues("spring.elasticsearch.uris=http://localhost:9200,http://localhost:9201")
                .run(context -> {
                    assertThat(context).hasSingleBean(RestClientBuilder.class)
                            .hasSingleBean(RestClient.class)
                            .hasSingleBean(EvolutionRestClient.class)
                            .hasSingleBean(EvolutionESRestClient.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                });
    }

    @Test
    void whenCustomEvolutionRestClientExists_thenDefaultEvolutionESRestClientIsNotCreated() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(EvolutionOpenSearchGenericClient.class, EvolutionOpenSearchRestClient.class))
                .withUserConfiguration(CustomEvolutionRestClientConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(EvolutionRestClient.class)
                            .doesNotHaveBean(EvolutionESRestClient.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                    assertThat(context.getBean(EvolutionRestClient.class))
                            .isSameAs(context.getBean(CustomEvolutionRestClientConfig.class).evolutionRestClient);
                });
    }

    // ========== OpenSearch RestClient Configuration Tests ==========

    @Test
    void whenOpenSearchRestClientExists_thenEvolutionOpenSearchRestClientIsCreated() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(EvolutionESRestClient.class, EvolutionOpenSearchGenericClient.class))
                .withUserConfiguration(OpenSearchRestClientConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(org.opensearch.client.RestClient.class)
                            .hasSingleBean(EvolutionOpenSearchRestClient.class)
                            .hasSingleBean(EvolutionRestClient.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                });
    }

    @Test
    void whenOpenSearchRestClientBuilderExists_thenOpenSearchRestClientIsCreated() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(EvolutionOpenSearchGenericClient.class, EvolutionESRestClient.class))
                .withUserConfiguration(OpenSearchRestClientBuilderConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(org.opensearch.client.RestClientBuilder.class)
                            .hasSingleBean(org.opensearch.client.RestClient.class)
                            .hasSingleBean(EvolutionRestClient.class)
                            .hasSingleBean(EvolutionOpenSearchRestClient.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                });
    }

    @Test
    void whenNoOpenSearchRestClientOrBuilderExists_andUrisAreProvided_thenDefaultOpenSearchRestClientIsCreated() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(EvolutionOpenSearchGenericClient.class, EvolutionESRestClient.class))
                .withPropertyValues("opensearch.uris=http://localhost:9200,http://localhost:9201")
                .run(context -> {
                    assertThat(context).hasSingleBean(org.opensearch.client.RestClientBuilder.class)
                            .hasSingleBean(org.opensearch.client.RestClient.class)
                            .hasSingleBean(EvolutionRestClient.class)
                            .hasSingleBean(EvolutionOpenSearchRestClient.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                });
    }

    // ========== OpenSearch GenericClient Configuration Tests ==========

    @Test
    void whenOpenSearchGenericClientExists_thenEvolutionOpenSearchGenericClientIsCreated() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(EvolutionESRestClient.class, EvolutionOpenSearchRestClient.class))
                .withUserConfiguration(OpenSearchGenericClientConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenSearchGenericClient.class)
                            .hasSingleBean(EvolutionOpenSearchGenericClient.class)
                            .hasSingleBean(EvolutionRestClient.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                });
    }

    @Test
    void whenOpenSearchClientExists_thenEvolutionOpenSearchGenericClientIsCreated() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(EvolutionESRestClient.class, EvolutionOpenSearchRestClient.class))
                .withUserConfiguration(OpenSearchClientConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenSearchClient.class)
                            .hasSingleBean(EvolutionOpenSearchGenericClient.class)
                            .hasSingleBean(EvolutionRestClient.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                });
    }

    @Test
    void whenOpenSearchTransportExists_thenOpenSearchGenericClientIsCreated() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(EvolutionESRestClient.class, EvolutionOpenSearchRestClient.class))
                .withUserConfiguration(OpenSearchTransportConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenSearchTransport.class)
                            .hasSingleBean(OpenSearchGenericClient.class)
                            .hasSingleBean(EvolutionRestClient.class)
                            .hasSingleBean(EvolutionOpenSearchGenericClient.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                });
    }

    @Test
    void whenNoOpenSearchClientExists_andUrisAreProvided_thenDefaultOpenSearchGenericClientIsCreated() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(EvolutionESRestClient.class, EvolutionOpenSearchRestClient.class))
                .withPropertyValues("opensearch.uris=http://localhost:9200")
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenSearchGenericClient.class)
                            .hasSingleBean(EvolutionOpenSearchGenericClient.class)
                            .hasSingleBean(EvolutionRestClient.class)
                            .hasSingleBean(ElasticsearchEvolution.class);
                });
    }

    // ========== Test Configurations ==========

    @Configuration
    static class MockRestClientConfig {
        @Bean
        public EvolutionRestClient evolutionRestClient() {
            return mock(EvolutionRestClient.class);
        }
    }

    @Configuration
    static class CustomElasticsearchEvolutionConfig {

        final ElasticsearchEvolution elasticsearchEvolution = mock(ElasticsearchEvolution.class);

        @Bean
        public ElasticsearchEvolution elasticsearchEvolution() {
            return elasticsearchEvolution;
        }

        @Bean
        public EvolutionRestClient evolutionRestClient() {
            return mock(EvolutionRestClient.class);
        }
    }

    @Configuration
    static class CustomInitializerConfig {
        @Bean
        public ElasticsearchEvolutionInitializer elasticsearchEvolutionInitializer() {
            return mock(ElasticsearchEvolutionInitializer.class);
        }
    }

    @Configuration
    static class ElasticsearchRestClientConfig {
        @Bean
        public RestClient restClient() {
            return mock(RestClient.class);
        }
    }

    @Configuration
    static class ElasticsearchRestClientBuilderConfig {
        @Bean
        public RestClientBuilder restClientBuilder() {
            return mock(RestClientBuilder.class, Mockito.RETURNS_DEEP_STUBS);
        }
    }

    @Configuration
    static class CustomEvolutionRestClientConfig {
        final EvolutionRestClient evolutionRestClient = mock(EvolutionRestClient.class);
        @Bean
        public EvolutionRestClient evolutionRestClient() {
            return evolutionRestClient;
        }
    }

    @Configuration
    static class OpenSearchRestClientConfig {
        @Bean
        public org.opensearch.client.RestClient restClient() {
            return mock(org.opensearch.client.RestClient.class);
        }
    }

    @Configuration
    static class OpenSearchRestClientBuilderConfig {
        @Bean
        public org.opensearch.client.RestClientBuilder restClientBuilder() {
            return mock(org.opensearch.client.RestClientBuilder.class, Mockito.RETURNS_DEEP_STUBS);
        }
    }

    @Configuration
    static class OpenSearchGenericClientConfig {
        @Bean
        public OpenSearchGenericClient openSearchGenericClient() {
            return mock(OpenSearchGenericClient.class);
        }
    }

    @Configuration
    static class OpenSearchClientConfig {
        @Bean
        public OpenSearchClient openSearchClient() {
            return mock(OpenSearchClient.class);
        }
    }

    @Configuration
    static class OpenSearchTransportConfig {
        @Bean
        public OpenSearchTransport openSearchTransport() {
            return mock(OpenSearchTransport.class);
        }
    }
}
