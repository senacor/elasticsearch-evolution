package com.senacor.elasticsearch.evolution.rest.abstracion.os.genericclient;

import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestClient;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestResponse;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestResponseImpl;
import com.senacor.elasticsearch.evolution.rest.abstracion.HttpMethod;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.generic.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public class EvolutionOpenSearchGenericClient implements EvolutionRestClient {

    @NonNull
    private final OpenSearchGenericClient openSearchGenericClient;

    public EvolutionOpenSearchGenericClient(@NonNull OpenSearchClient openSearchClient) {
        this(new OpenSearchGenericClient(openSearchClient._transport(), openSearchClient._transportOptions()));
    }

    @Override
    public EvolutionRestResponse execute(@NonNull HttpMethod method,
                                         @NonNull String endpoint,
                                         Map<String, String> headers,
                                         Map<String, String> urlParams,
                                         String body) throws IOException {
        Collection<Map.Entry<String, String>> requestHeaders = Optional.ofNullable(headers)
                .map(Map::entrySet)
                .orElse(Set.of());
        Map<String, String> parameters = null == urlParams
                ? Map.of()
                : urlParams;
        Body genericBody = null == body
                ? null
                : Body.from(body.getBytes(StandardCharsets.UTF_8), getContentType(headers).orElse(null));

        final Request request = Requests.create(method.name(), endpoint, requestHeaders, parameters, genericBody);

        try (final Response response = openSearchGenericClient.execute(request)) {
            return new EvolutionRestResponseImpl(response.getStatus(),
                    Optional.ofNullable(response.getReason()),
                    response.getBody()
                            .map(Body::bodyAsString));
        }
    }
}
