package com.senacor.elasticsearch.evolution.testspringboot2.esresthighlevelclient760;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
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
        try {
            restClient.performRequest(new Request("POST", "/_refresh"));
        } catch (IOException e) {
            throw new IllegalStateException("refreshIndices failed", e);
        }
    }

    public List<String> fetchAllDocuments(String index) {
        try {
            Request post = new Request("POST", "/" + index + "/_search");
            post.setJsonEntity("{" +
                    "    \"query\": {" +
                    "        \"match_all\": {}" +
                    "    }" +
                    "}");
            Response response = restClient.performRequest(post);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException("fetchAllDocuments(" + index + ") failed with HTTP status " +
                        statusCode + ": " + response.toString());
            }
            String body = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

            return parseDocuments(body)
                    .collect(Collectors.toList());
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
}

