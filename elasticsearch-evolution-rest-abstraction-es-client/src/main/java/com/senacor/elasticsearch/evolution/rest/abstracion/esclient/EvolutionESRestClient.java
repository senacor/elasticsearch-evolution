package com.senacor.elasticsearch.evolution.rest.abstracion.esclient;

import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestClient;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestResponse;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestResponseImpl;
import com.senacor.elasticsearch.evolution.rest.abstracion.HttpMethod;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class EvolutionESRestClient implements EvolutionRestClient {

    @NonNull
    private final RestClient restClient;

    @Override
    public String info() {
        return "%s (nodes=%s)".formatted(EvolutionRestClient.super.info(), restClient.getNodes());
    }

    @Override
    public EvolutionRestResponse execute(@NonNull HttpMethod method,
                                         @NonNull String endpoint,
                                         Map<String, String> headers,
                                         Map<String, String> urlParams,
                                         String body) throws IOException {
        final Request request = new Request(method.name(), endpoint);
        if (null != urlParams) {
            request.addParameters(urlParams);
        }
        if (null != body
                && !body.trim().isEmpty()) {
            ContentType contentType = getContentType(headers)
                    .map(ContentType::parse)
                    .orElse(null);
            request.setEntity(new NStringEntity(body, contentType));
        }
        if (null != headers) {
            RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
            headers.forEach(builder::addHeader);
            request.setOptions(builder);
        }

        final Response response = restClient.performRequest(request);

        final HttpEntity entity = response.getEntity();
        Optional<String> responseBody = null == entity
                ? Optional.empty()
                : Optional.ofNullable(EntityUtils.toString(entity));
        return new EvolutionRestResponseImpl(response.getStatusLine().getStatusCode(),
                Optional.ofNullable(response.getStatusLine().getReasonPhrase()),
                responseBody);
    }
}
