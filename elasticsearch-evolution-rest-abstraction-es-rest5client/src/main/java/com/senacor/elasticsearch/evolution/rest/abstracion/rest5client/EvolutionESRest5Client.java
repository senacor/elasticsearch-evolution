package com.senacor.elasticsearch.evolution.rest.abstracion.rest5client;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.RequestOptions;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestClient;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestResponse;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestResponseImpl;
import com.senacor.elasticsearch.evolution.rest.abstracion.HttpMethod;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class EvolutionESRest5Client implements EvolutionRestClient {

    @NonNull
    private final Rest5Client restClient;

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
            request.setEntity(new StringEntity(body, contentType));
        }
        if (null != headers) {
            RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
            headers.forEach(builder::addHeader);
            request.setOptions(builder);
        }

        final Response response = restClient.performRequest(request);

        final HttpEntity entity = response.getEntity();
        final Optional<String> responseBody;
        try {
            responseBody = null == entity
                    ? Optional.empty()
                    : Optional.ofNullable(EntityUtils.toString(entity));
        } catch (ParseException e) {
            throw new IOException("failed to parse body content", e);
        }
        return new EvolutionRestResponseImpl(response.getStatusCode(),
                Optional.empty(),
                responseBody);
    }
}
