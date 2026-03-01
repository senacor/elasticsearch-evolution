package com.senacor.elasticsearch.evolution.springboot40;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Andreas Keefer
 */
public class EsUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;

    public EsUtils(RestClient restClient) {
        this.restClient = restClient;
    }

    public void refreshIndices() {
        restClient.get().uri("/_refresh").retrieve();
    }

    public List<String> fetchAllDocuments(String index) {
        String body = restClient.post()
                .uri("/" + index + "/_search")
                .body("""
                        {\
                            "query": {\
                                "match_all": {}\
                            }\
                        }\
                        """)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
                    throw new IllegalStateException("fetchAllDocuments(" + index + ") failed with HTTP status " +
                            response.getStatusCode().value() + ": " + IOUtils.toString(response.getBody(), StandardCharsets.UTF_8));
                })
                .body(String.class);

        return parseDocuments(body)
                .toList();
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
}

