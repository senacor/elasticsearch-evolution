package com.senacor.elasticsearch.evolution.core.test;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SocketUtils;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension.SearchContainer.ofElasticsearch;
import static com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension.SearchContainer.ofOpensearch;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.time.Duration.ofMinutes;
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
    private static final SortedSet<SearchContainer> SUPPORTED_SEARCH_VERSIONS = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(
            ofOpensearch("1.3.1"),
            ofOpensearch("1.2.4"),
            ofOpensearch("1.1.0"),
            ofOpensearch("1.0.1"),

            ofElasticsearch("8.1.2"),
            ofElasticsearch("8.0.1"),
            ofElasticsearch("7.17.2"),
            ofElasticsearch("7.16.3"),
            ofElasticsearch("7.15.2"),
            ofElasticsearch("7.14.2"),
            ofElasticsearch("7.13.4"),
            ofElasticsearch("7.12.1"),
            ofElasticsearch("7.11.2"),
            ofElasticsearch("7.10.2"),
            ofElasticsearch("7.9.3"),
            ofElasticsearch("7.8.1"),
            ofElasticsearch("7.7.1"),
            ofElasticsearch("7.6.2"),
            ofElasticsearch("7.5.2")
    )));

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        SUPPORTED_SEARCH_VERSIONS.parallelStream()
                .forEach(searchContainer -> getStore(context)
                        .getOrComputeIfAbsent(searchContainer, EmbeddedElasticsearchExtension::createElasticsearchContainer, ElasticsearchContainer.class));
    }

    private static RestHighLevelClient createRestHighLevelClient(String versionInfo, ElasticsearchContainer elasticsearchContainer) {
        HttpHost host = HttpHost.create(elasticsearchContainer.getHttpHostAddress());
        logger.debug("create RestClient for {} at {}", versionInfo, host);
        RestClientBuilder builder = RestClient.builder(host);
        return new RestHighLevelClient(builder);
    }

    private static ElasticsearchContainer createElasticsearchContainer(SearchContainer searchContainer) {
        logger.info("creating ElasticsearchContainer for {} ...", searchContainer.getInfo());
        ElasticsearchContainer container = new ElasticsearchContainer(DockerImageName.parse(searchContainer.getContainerImage())
                .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch")
                .withTag(searchContainer.getVersion())) {
            @Override
            protected void containerIsStarted(InspectContainerResponse containerInfo) {
                // since testcontainers 1.17 it detects if ES 8.x is running and copies a certificate in this case
                // but we don't want security
            }
        }
                .withEnv(searchContainer.getEnv())
                .withEnv("cluster.routing.allocation.disk.watermark.low", "97%")
                .withEnv("cluster.routing.allocation.disk.watermark.high", "98%")
                .withEnv("cluster.routing.allocation.disk.watermark.flood_stage", "99%");
        int httpPort = SocketUtils.findAvailableTcpPort(5000, 30000);
        int transportPort = SocketUtils.findAvailableTcpPort(30001, 65535);
        container.setPortBindings(Arrays.asList(httpPort + ":9200", transportPort + ":" + searchContainer.transportPort));
        container.setWaitStrategy(new HttpWaitStrategy()
                .forPort(9200)
                .forStatusCodeMatching(response -> response == HTTP_OK || response == HTTP_UNAUTHORIZED)
                .withStartupTimeout(ofMinutes(5)));
        start(container, searchContainer.getInfo());
        logger.info("ElasticsearchContainer {} started with HttpPort={} and TransportTcpPort={}!",
                searchContainer.getInfo(),
                httpPort,
                transportPort);
        return container;
    }

    private static void start(ElasticsearchContainer elasticsearchContainer, String versionInfo) {
        logger.debug("starting ElasticsearchContainer {}", versionInfo);
        elasticsearchContainer.start();
    }

    private static void cleanup(EsUtils esUtils, String versionInfo, RestHighLevelClient restHighLevelClient) {
        logger.debug("cleanup ElasticsearchContainer {}", versionInfo);
        try {
            // get all indices
            final GetIndexResponse allIndices = restHighLevelClient.indices().get(new GetIndexRequest("_all")
                    .indicesOptions(IndicesOptions.lenientExpandOpen()), DEFAULT);
            if (allIndices.getIndices().length > 0) {
                logger.debug("delete indices {}", Arrays.toString(allIndices.getIndices()));
                restHighLevelClient.indices().delete(new DeleteIndexRequest(allIndices.getIndices()), DEFAULT);
            }
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
     * provides in this order:
     * - Short Version Info
     * - EsUtils
     * - RestHighLevelClient
     */
    public static class ElasticsearchArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return SUPPORTED_SEARCH_VERSIONS.stream()
                    .map(searchContainer -> {
                        ElasticsearchContainer elasticsearchContainer = getStore(context)
                                .getOrComputeIfAbsent(searchContainer, EmbeddedElasticsearchExtension::createElasticsearchContainer, ElasticsearchContainer.class);
                        start(elasticsearchContainer, searchContainer.getInfo());
                        RestHighLevelClient restHighLevelClient = createRestHighLevelClient(searchContainer.getInfo(), elasticsearchContainer);
                        EsUtils esUtils = new EsUtils(restHighLevelClient.getLowLevelClient());
                        cleanup(esUtils, searchContainer.getInfo(), restHighLevelClient);
                        return Arguments.of(searchContainer.getShortInfo(), esUtils, restHighLevelClient);
                    });
        }
    }

    @Value
    @Builder
    public static class SearchContainer implements Comparable<SearchContainer> {
        @NonNull
        String vendor;
        @NonNull
        String vendorShort;
        @NonNull
        String version;
        @NonNull
        String containerImage;
        @NonNull
        Map<String, String> env;
        int transportPort;

        public static SearchContainer ofElasticsearch(String version) {
            return SearchContainer.builder()
                    .vendor("Elasticsearch")
                    .vendorShort("ES")
                    .containerImage("docker.elastic.co/elasticsearch/elasticsearch")
                    .version(version)
                    .env(ImmutableMap.of(
                            "ES_JAVA_OPTS", "-Xms128m -Xmx128m",
                            // since elasticsearch 8 security / https is enabled per default - but for testing it should be disabled
                            "xpack.security.enabled", "false"
                    ))
                    .transportPort(9300)
                    .build();
        }

        public static SearchContainer ofOpensearch(String version) {
            return SearchContainer.builder()
                    .vendor("Opensearch")
                    .vendorShort("OS")
                    .containerImage("quay.io/xtermi2/opensearch")
                    .version(version)
                    .env(ImmutableMap.of(
                            "OPENSEARCH_JAVA_OPTS", "-Xms128m -Xmx128m",
                            // disable security / https for testing
                            "plugins.security.disabled", "true",
                            "DISABLE_INSTALL_DEMO_CONFIG", "true"
                    ))
                    .transportPort(9600)
                    .build();
        }

        public String getInfo() {
            return vendor + " " + version;
        }

        public String getShortInfo() {
            return vendorShort + " " + version;
        }

        @Override
        public int compareTo(@NotNull SearchContainer other) {
            return version.compareTo(other.version);
        }
    }
}
