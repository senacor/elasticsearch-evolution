package com.senacor.elasticsearch.evolution.rest.abstraction.os.genericclient;

import com.senacor.elasticsearch.evolution.rest.abstraction.EvolutionRestResponse;
import com.senacor.elasticsearch.evolution.rest.abstraction.HttpMethod;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.opensearch.client.opensearch.generic.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvolutionOpenSearchGenericClientTest {

    @Mock
    private OpenSearchGenericClient openSearchGenericClient;

    private EvolutionOpenSearchGenericClient underTest;

    @BeforeEach
    void setUp() {
        underTest = new EvolutionOpenSearchGenericClient(openSearchGenericClient);
    }

    @Test
    void createFrom_OpenSearchClient(@Mock OpenSearchClient openSearchClient) {

        underTest = new EvolutionOpenSearchGenericClient(openSearchClient);

        final InOrder order = inOrder(openSearchClient, openSearchGenericClient);
        order.verify(openSearchClient)._transport();
        order.verify(openSearchClient)._transportOptions();
        order.verifyNoMoreInteractions();
    }

    @Test
    void execute_should_call_ESRestClient_withFullResponse(@Mock(answer = Answers.RETURNS_DEEP_STUBS) Response response) throws IOException {
        when(openSearchGenericClient.execute(any()))
                .thenReturn(response);
        when(response.getStatus())
                .thenReturn(200);
        when(response.getReason())
                .thenReturn("OK");
        when(response.getBody())
                .thenReturn(Optional.of(Body.from(
                        "my body".getBytes(StandardCharsets.UTF_8),
                        ContentType.TEXT_PLAIN.getMimeType()
                )));


        final EvolutionRestResponse res = underTest.execute(HttpMethod.POST,
                "/_search",
                Map.of("Content-Type", "application/json"),
                Map.of("refresh", "true"),
                """
                        {"query": "bar"}""");


        assertThat(res)
                .as("response")
                .isNotNull();
        assertThat(res.statusCode())
                .as("status code")
                .isEqualTo(200);
        assertThat(res.statusReasonPhrase())
                .as("status reason phrase")
                .contains("OK");
        assertThat(res.body())
                .as("body")
                .contains("my body");

        final InOrder order = inOrder(openSearchGenericClient);
        order.verify(openSearchGenericClient).execute(argThat(argument -> {
            assertSoftly(softly -> {
                softly.assertThat(argument.getMethod())
                        .as("HTTP method")
                        .isEqualTo("POST");
                softly.assertThat(argument.getEndpoint())
                        .as("endpoint")
                        .isEqualTo("/_search");
                softly.assertThat(argument.getParameters())
                        .as("URL parameters")
                        .containsExactlyInAnyOrderEntriesOf(Map.of("refresh", "true"));
                softly.assertThat(argument.getHeaders())
                        .as("headers")
                        .anySatisfy(header -> {
                            softly.assertThat(header.getKey())
                                    .as("header name")
                                    .isEqualTo("Content-Type");
                            softly.assertThat(header.getValue())
                                    .as("header value")
                                    .isEqualTo("application/json");
                        });
                softly.assertThat(argument.getBody().map(Body::bodyAsString))
                        .as("body")
                        .contains("""
                                {"query": "bar"}""");
            });
            return true;
        }));
        order.verifyNoMoreInteractions();
    }

    @Test
    void execute_should_call_ESRestClient_withMinimumResponse(@Mock Response response) throws IOException {
        when(openSearchGenericClient.execute(any()))
                .thenReturn(response);
        when(response.getStatus())
                .thenReturn(200);
        when(response.getReason())
                .thenReturn(null);
        when(response.getBody())
                .thenReturn(Optional.empty());


        final EvolutionRestResponse res = underTest.execute(HttpMethod.POST,
                "/_search");


        assertThat(res)
                .as("response")
                .isNotNull();
        assertThat(res.statusCode())
                .as("status code")
                .isEqualTo(200);
        assertThat(res.statusReasonPhrase())
                .as("status reason phrase")
                .isEmpty();
        assertThat(res.body())
                .as("body")
                .isEmpty();

        final InOrder order = inOrder(openSearchGenericClient);
        order.verify(openSearchGenericClient).execute(argThat(argument -> {
            assertSoftly(softly -> {
                softly.assertThat(argument.getMethod())
                        .as("HTTP method")
                        .isEqualTo("POST");
                softly.assertThat(argument.getEndpoint())
                        .as("endpoint")
                        .isEqualTo("/_search");
                softly.assertThat(argument.getParameters())
                        .as("URL parameters")
                        .isEmpty();
                softly.assertThat(argument.getHeaders())
                        .as("headers")
                        .isEmpty();
                softly.assertThat(argument.getBody())
                        .as("body")
                        .isEmpty();
            });
            return true;
        }));
        order.verifyNoMoreInteractions();
    }

    @Test
    void getUnderlyingClient_should_returnTheInternalOpenSearchGenericClient() {
        assertThat(underTest.getUnderlyingClient())
                .isSameAs(openSearchGenericClient);
    }
}