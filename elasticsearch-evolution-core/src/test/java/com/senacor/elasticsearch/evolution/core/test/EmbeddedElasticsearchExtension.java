package com.senacor.elasticsearch.evolution.core.test;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.senacor.elasticsearch.evolution.rest.abstraction.EvolutionRestClient;
import com.senacor.elasticsearch.evolution.rest.abstraction.os.restclient.EvolutionOpenSearchRestClient;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.http.HttpHost;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.TestSocketUtils;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension.SearchContainer.ofElasticsearch;
import static com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension.SearchContainer.ofOpensearch;
import static java.time.Duration.ofMinutes;

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
            ofOpensearch("3.5.0"),
            ofOpensearch("3.4.0"),
            ofOpensearch("2.19.4"),

            ofElasticsearch("9.3.0"),
            ofElasticsearch("9.2.5"),
            ofElasticsearch("8.19.11")
    )));

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        SUPPORTED_SEARCH_VERSIONS.parallelStream()
                .forEach(searchContainer -> getStore(context)
                        .getOrComputeIfAbsent(searchContainer, EmbeddedElasticsearchExtension::createElasticsearchContainer, ElasticsearchContainer.class));
    }

    private static RestClient createRestClient(String versionInfo, ElasticsearchContainer elasticsearchContainer) {
        HttpHost host = HttpHost.create(elasticsearchContainer.getHttpHostAddress());
        logger.debug("create RestClient for {} at {}", versionInfo, host);
        return RestClient.builder(host)
                .build();
    }

    private static OpenSearchClient createOpenSearchClient(RestClient restClient) {
        OpenSearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());
        return new OpenSearchClient(transport);
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
        int httpPort = TestSocketUtils.findAvailableTcpPort();
        int transportPort = TestSocketUtils.findAvailableTcpPort();
        container.setPortBindings(Arrays.asList(httpPort + ":9200", transportPort + ":" + searchContainer.transportPort));
//        container.setWaitStrategy(new HttpWaitStrategy()
//                .forPort(9200)
//                .forStatusCodeMatching(response -> response == HTTP_OK || response == HTTP_UNAUTHORIZED)
//                .withStartupTimeout(ofMinutes(5)));
        container.setWaitStrategy(new LogMessageWaitStrategy()
                .withRegEx(".*(\"message\":\\s?\"started[\\s?|\"].*|] started\n$)")
                .withStartupTimeout(ofMinutes(15)));
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

    private static void cleanup(EsUtils esUtils,
                                String versionInfo,
                                OpenSearchClient openSearchClient,
                                RestClient restClient) {
        logger.debug("cleanup ElasticsearchContainer {}", versionInfo);
        try {
            // get all indices
            final GetIndexResponse allIndices = openSearchClient.indices().get(get -> get
                    .index("_all")
                    .ignoreUnavailable(true)
                    .allowNoIndices(true));
            if (!allIndices.result().isEmpty()) {
                logger.debug("delete indices {}", allIndices.result().keySet());
                openSearchClient.indices().delete(builder -> builder.index(List.copyOf((allIndices.result().keySet()))));
            }
            Response deleteRes = restClient.performRequest(new Request("DELETE", "/_template/*"));
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
     */
    public static class ElasticsearchArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return SUPPORTED_SEARCH_VERSIONS.stream()
                    .map(searchContainer -> {
                        ElasticsearchContainer elasticsearchContainer = getStore(context)
                                .getOrComputeIfAbsent(searchContainer, EmbeddedElasticsearchExtension::createElasticsearchContainer, ElasticsearchContainer.class);
                        start(elasticsearchContainer, searchContainer.getInfo());
                        final RestClient restClient = createRestClient(searchContainer.getInfo(), elasticsearchContainer);
                        final OpenSearchClient openSearchClient = createOpenSearchClient(restClient);
                        final EvolutionRestClient<?> evolutionRestClient = new EvolutionOpenSearchRestClient(restClient);
                        final EsUtils esUtils = new EsUtils(restClient, evolutionRestClient, openSearchClient);
                        cleanup(esUtils, searchContainer.getInfo(), openSearchClient, restClient);
                        return Arguments.of(searchContainer.getShortInfo(), esUtils);
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
                    // dockerhub and public.ecr.aws can run into rate limit, so stay with quay.io
                    .containerImage("quay.io/xtermi2/opensearch")
                    .version(version)
                    .env(ImmutableMap.of(
                            // since opensearch 3.x 128MB is not enough
                            "OPENSEARCH_JAVA_OPTS", "-Xms196m -Xmx196m",
                            // disable security / https for testing
                            "plugins.security.disabled", "true",
                            "DISABLE_INSTALL_DEMO_CONFIG", "true",
                            "discovery.type", "single-node"
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
