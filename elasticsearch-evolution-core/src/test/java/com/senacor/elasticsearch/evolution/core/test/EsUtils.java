package com.senacor.elasticsearch.evolution.core.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senacor.elasticsearch.evolution.rest.abstraction.EvolutionRestClient;
import com.senacor.elasticsearch.evolution.rest.abstraction.EvolutionRestResponse;
import com.senacor.elasticsearch.evolution.rest.abstraction.HttpMethod;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Andreas Keefer
 */
@RequiredArgsConstructor
@Getter
public class EsUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @NonNull
    private final EvolutionRestClient<?> evolutionRestClient;
    @NonNull
    private final OpenSearchClient openSearchClient;

    public void refreshIndices() {
        try {
            evolutionRestClient.execute(HttpMethod.GET, "/_refresh");
        } catch (IOException e) {
            throw new IllegalStateException("refreshIndices failed", e);
        }
    }

    public List<String> fetchAllDocuments(String index) {
        try {
            EvolutionRestResponse response = evolutionRestClient.execute(
                    HttpMethod.POST,
                    "/" + index + "/_search",
                    Map.of("Content-Type", "application/json"),
                    null,
                    """
                    {\
                        "query": {\
                            "match_all": {}\
                        }\
                    }\
                    """);
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException("fetchAllDocuments(" + index + ") failed with HTTP status " +
                        statusCode + ": " + response.asString());
            }
            String body = response.body().orElse("");

            return parseDocuments(body)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("fetchAllDocuments(" + index + ") failed", e);
        }
    }

    private Stream<String> parseDocuments(String body) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(body);
            return StreamSupport.stream(jsonNode.get("hits").get("hits").spliterator(), false)
                    .map(hitNode -> hitNode.get("_source"))
                    .map(JsonNode::toString);
        } catch (IOException e) {
            throw new IllegalStateException("parseDocuments failed. body=" + body, e);
        }
    }

    public void indexDocument(String index, String id, HashMap<String, Object> source) {
        try {
            final EvolutionRestResponse res = evolutionRestClient.execute(
                    HttpMethod.PUT,
                    "/" + index + "/_doc/" + id,
                    Map.of("Content-Type", "application/json"),
                    null,
                    OBJECT_MAPPER.writeValueAsString(source));
            if (res.statusCode() != 201) {
                throw new IllegalStateException("indexDocument failed with status code %s: %s".formatted(
                        res.statusCode(),
                        res.statusReasonPhrase().orElse("")));
            }
        } catch (IOException e) {
            throw new IllegalStateException("indexDocument failed", e);
        }
    }

    public Set<String> getAllIndices() {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("ignore_unavailable", "true");
            params.put("allow_no_indices", "true");
            EvolutionRestResponse response = evolutionRestClient.execute(
                    HttpMethod.GET,
                    "/_all",
                    null,
                    params,
                    null);
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException("getAllIndices failed with HTTP status " +
                        statusCode + ": " + response.asString());
            }
            if (response.body().isEmpty()) {
                return Set.of();
            }
            Map<String, Object> indices = OBJECT_MAPPER.readValue(response.body().get(), new TypeReference<>() {
            });
            return indices.keySet();
        } catch (IOException e) {
            throw new IllegalStateException("getAllIndices failed", e);
        }
    }

    public void deleteIndices(Set<String> indices) {
        try {
            if (null == indices || indices.isEmpty()) {
                return;
            }
            String indexNames = String.join(",", indices);
            EvolutionRestResponse response = evolutionRestClient.execute(
                    HttpMethod.DELETE,
                    "/" + indexNames);
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException("deleteIndices failed with HTTP status " +
                        statusCode + ": " + response.asString());
            }
        } catch (IOException e) {
            throw new IllegalStateException("deleteIndices failed", e);
        }
    }

    public EvolutionRestResponse deleteAllTemplates() throws IOException {
        return evolutionRestClient.execute(HttpMethod.DELETE, "/_template/*");
    }
}