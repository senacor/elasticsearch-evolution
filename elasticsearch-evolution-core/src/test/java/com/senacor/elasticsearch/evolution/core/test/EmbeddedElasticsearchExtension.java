package com.senacor.elasticsearch.evolution.core.test;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Extension to test with multiple Embedded Elasticsearch versions
 *
 * @author akeefer
 */
public class EmbeddedElasticsearchExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private static final Set<String> SUPPORTED_ES_VERSIONS = new HashSet<>(Arrays.asList(
            "6.5.4",
            "6.4.3",
            "6.3.2",
            "6.2.4",
            "6.1.4",
            "6.0.1",
            "5.6.14"
    ));

    private static final Namespace NAMESPACE = Namespace.create(ExtensionContext.class);

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // startup all EmbeddedElasticsearch's
        SUPPORTED_ES_VERSIONS.parallelStream()
                .forEach(esVersion -> {
                    try {
                        EmbeddedElastic embeddedElastic = EmbeddedElastic.builder()
                                .withElasticVersion(esVersion)
                                .withStartTimeout(5, TimeUnit.MINUTES)
                                .withSetting(PopularProperties.CLUSTER_NAME, esVersion)
                                .build()
                                .start();
                        context.getStore(NAMESPACE).put(esVersion, embeddedElastic);
                    } catch (IOException | InterruptedException e) {
                        throw new IllegalStateException("could not start embedded elasticsearch", e);
                    }
                });
    }


    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // shutdown all EmbeddedElasticsearch's
        SUPPORTED_ES_VERSIONS.parallelStream()
                .forEach(esVersion ->
                        context.getStore(NAMESPACE).get(esVersion, EmbeddedElastic.class).stop());
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // cleanup Indices and Templates
        SUPPORTED_ES_VERSIONS.parallelStream()
                .forEach(esVersion -> {
                    EmbeddedElastic embeddedElastic = context.getStore(NAMESPACE).get(esVersion, EmbeddedElastic.class);
                    embeddedElastic.deleteTemplates();
                    embeddedElastic.deleteIndices();
                });
    }

    public static RestHighLevelClient createRestHighLevelClient(EmbeddedElastic embeddedElastic) {
        HttpHost host = new HttpHost("127.0.0.1", embeddedElastic.getHttpPort(), "http");
        RestClientBuilder builder = RestClient.builder(host);
        return new RestHighLevelClient(builder);
    }


    /**
     * provides
     * - EmbeddedElastic
     * - RestHighLevelClient
     */
    public static class ElasticsearchArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return SUPPORTED_ES_VERSIONS.stream()
                    .map(esVersion -> {
                        EmbeddedElastic embeddedElastic = context.getStore(NAMESPACE).get(esVersion, EmbeddedElastic.class);
                        return Arguments.of(esVersion, embeddedElastic, createRestHighLevelClient(embeddedElastic));
                    });
        }
    }
}
