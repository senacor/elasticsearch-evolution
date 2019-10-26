package com.senacor.elasticsearch.evolution.core.test;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.*;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SocketUtils;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.elasticsearch.client.RequestOptions.DEFAULT;

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
            "7.4.1",
            "7.3.2",
            "7.2.1",
            "7.1.1",
            "7.0.1",
            "6.8.3",
            "6.7.2",
            "6.6.2"
    ));

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        SUPPORTED_ES_VERSIONS.parallelStream()
                .forEach(esVersion -> getStore(context)
                        .getOrComputeIfAbsent(esVersion, EmbeddedElasticsearchExtension::createElasticsearchContainer, ElasticsearchContainer.class));
    }

    private static RestHighLevelClient createRestHighLevelClient(String esVersion, ElasticsearchContainer elasticsearchContainer) {
        HttpHost host = HttpHost.create(elasticsearchContainer.getHttpHostAddress());
        logger.debug("create RestHighLevelClient for ES {} at {}", esVersion, host);
        RestClientBuilder builder = RestClient.builder(host);
        return new RestHighLevelClient(builder);
    }

    private static ElasticsearchContainer createElasticsearchContainer(String esVersion) {
        logger.info("creating ElasticsearchContainer {} ...", esVersion);
        ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:" + esVersion)
                .withEnv("ES_JAVA_OPTS", "-Xms128m -Xmx128m");
        int esHttpPort = SocketUtils.findAvailableTcpPort(5000, 30000);
        int esTransportPort = SocketUtils.findAvailableTcpPort(30001, 65535);
        container.setPortBindings(Arrays.asList(esHttpPort + ":9200", esTransportPort + ":9300"));
        start(container, esVersion);
        logger.info("ElasticsearchContainer {} started with HttpPort={} and TransportTcpPort={}!",
                esVersion,
                esHttpPort,
                esTransportPort);
        return container;
    }

    private static void start(ElasticsearchContainer elasticsearchContainer, String esVersion) {
        logger.debug("starting ElasticsearchContainer {}", esVersion);
        elasticsearchContainer.start();
    }

    private static void cleanup(EsUtils esUtils, String esVersion, RestHighLevelClient restHighLevelClient) {
        logger.debug("cleanup ElasticsearchContainer {}", esVersion);
        try {
            restHighLevelClient.indices().delete(new DeleteIndexRequest("*"), DEFAULT);
            Response deleteRes = restHighLevelClient.getLowLevelClient().performRequest(new Request("DELETE", "/_template/*"));
            logger.debug("deleted all templates: {}", deleteRes);
        } catch (IOException e) {
            throw new IllegalStateException("ElasticsearchContainer cleanup failed", e);
        }
        esUtils.refreshIndices();
    }

    private static ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }

    /**
     * provides
     * - ElasticsearchContainer
     * - RestHighLevelClient
     */
    public static class ElasticsearchArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            Optional<String> versionFilterPattern = context.getTestMethod()
                    .map(method -> method.getDeclaredAnnotation(IgnoreEsVersion.class))
                    .map(IgnoreEsVersion::value);


            return SUPPORTED_ES_VERSIONS.stream()
                    .filter(version -> versionFilterPattern
                            .map(filterPattern -> !version.matches(filterPattern))
                            .orElse(true)
                    )
                    .sorted()
                    .map(esVersion -> {
                        ElasticsearchContainer elasticsearchContainer = getStore(context)
                                .getOrComputeIfAbsent(esVersion, EmbeddedElasticsearchExtension::createElasticsearchContainer, ElasticsearchContainer.class);
                        start(elasticsearchContainer, esVersion);
                        RestHighLevelClient restHighLevelClient = createRestHighLevelClient(esVersion, elasticsearchContainer);
                        EsUtils esUtils = new EsUtils(restHighLevelClient.getLowLevelClient());
                        cleanup(esUtils, esVersion, restHighLevelClient);
                        return Arguments.of(esVersion, esUtils, restHighLevelClient);
                    });
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface IgnoreEsVersion {

        /**
         * The version pattern (regex) to ignore
         */
        String value();
    }
}
