package com.senacor.elasticsearch.evolution.core.test;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Extension to test with multiple Embedded Elasticsearch versions.
 * Use in combination with {@link ElasticsearchArgumentsProvider} for {@link org.junit.jupiter.params.ParameterizedTest}
 *
 * @author akeefer
 */
public class EmbeddedElasticsearchExtension implements TestInstancePostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedElasticsearchExtension.class);
    private static final Namespace NAMESPACE = Namespace.create(ExtensionContext.class);
    private static final Set<String> SUPPORTED_ES_VERSIONS = new HashSet<>(Arrays.asList(
            "6.5.4",
            "6.4.3",
            "6.3.2",
            "6.2.4",
            "6.1.4",
            "6.0.1",
            "5.6.14"
    ));

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        SUPPORTED_ES_VERSIONS.parallelStream()
                .forEach(esVersion -> getStore(context)
                        .getOrComputeIfAbsent(esVersion, EmbeddedElasticsearchExtension::createEmbeddedElastic, EmbeddedElastic.class));
    }

    private static RestHighLevelClient createRestHighLevelClient(String esVersion, EmbeddedElastic embeddedElastic) {
        HttpHost host = new HttpHost("localhost", embeddedElastic.getHttpPort(), "http");
        logger.debug("create RestHighLevelClient for ES {} at {}", esVersion, host);
        RestClientBuilder builder = RestClient.builder(host);
        return new RestHighLevelClient(builder);
    }

    private static EmbeddedElastic createEmbeddedElastic(String esVersion) {
        logger.info("creating EmbeddedElastic {} ...", esVersion);
        EmbeddedElastic embeddedElastic = EmbeddedElastic.builder()
                .withElasticVersion(esVersion)
                .withStartTimeout(5, TimeUnit.MINUTES)
                .withSetting(PopularProperties.CLUSTER_NAME, esVersion)
                .withEsJavaOpts("-Xms128m -Xmx128m")
                .build();
        start(embeddedElastic, esVersion);
        logger.info("EmbeddedElastic {} started with HttpPort={} and TransportTcpPort={}!",
                esVersion,
                embeddedElastic.getHttpPort(),
                embeddedElastic.getTransportTcpPort());
        return embeddedElastic;
    }

    private static void start(EmbeddedElastic embeddedElastic, String esVersion) {
        try {
            logger.debug("starting EmbeddedElastic {}", esVersion);
            embeddedElastic.start();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("could not start embedded elasticsearch " + esVersion, e);
        }
    }

    private static void cleanup(EmbeddedElastic embeddedElastic, String esVersion) {
        logger.debug("cleanup EmbeddedElastic {}", esVersion);
        embeddedElastic.deleteTemplates();
        embeddedElastic.deleteIndices();
        embeddedElastic.refreshIndices();
    }

    private static ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
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
                        EmbeddedElastic embeddedElastic = getStore(context)
                                .getOrComputeIfAbsent(esVersion, EmbeddedElasticsearchExtension::createEmbeddedElastic, EmbeddedElastic.class);
                        start(embeddedElastic, esVersion);
                        cleanup(embeddedElastic, esVersion);
                        return Arguments.of(esVersion, embeddedElastic, createRestHighLevelClient(esVersion, embeddedElastic));
                    });
        }
    }
}
