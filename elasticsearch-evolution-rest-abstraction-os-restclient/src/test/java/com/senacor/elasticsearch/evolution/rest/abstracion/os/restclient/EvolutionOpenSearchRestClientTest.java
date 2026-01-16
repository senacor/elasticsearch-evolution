package com.senacor.elasticsearch.evolution.rest.abstracion.os.restclient;

import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestResponse;
import com.senacor.elasticsearch.evolution.rest.abstracion.HttpMethod;
import org.apache.http.HttpHost;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.Node;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvolutionOpenSearchRestClientTest {

    @Mock
    private RestClient restClient;

    private EvolutionOpenSearchRestClient underTest;

    @BeforeEach
    void setUp() {
        underTest = new EvolutionOpenSearchRestClient(restClient);
    }

    @Test
    void info() {
        when(restClient.getNodes())
                .thenReturn(List.of(new Node(HttpHost.create("http://localhost:9200"))));

        assertThat(underTest.info())
                .isEqualTo(underTest.getClass().getName() + " (nodes=[[host=http://localhost:9200]])");
    }

    @Test
    void execute_should_call_ESRestClient_withFullResponse(@Mock(answer = Answers.RETURNS_DEEP_STUBS) Response response) throws IOException {
        when(restClient.performRequest(any()))
                .thenReturn(response);
        when(response.getStatusLine().getStatusCode())
                .thenReturn(200);
        when(response.getStatusLine().getReasonPhrase())
                .thenReturn("OK");
        when(response.getEntity())
                .thenReturn(new NStringEntity("my body", ContentType.TEXT_PLAIN));


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

        final InOrder order = inOrder(restClient);
        order.verify(restClient).performRequest(argThat(argument -> {
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
                softly.assertThat(argument.getOptions().getHeaders())
                        .as("headers")
                        .anySatisfy(header -> {
                            softly.assertThat(header.getName())
                                    .as("header name")
                                    .isEqualTo("Content-Type");
                            softly.assertThat(header.getValue())
                                    .as("header value")
                                    .isEqualTo("application/json");
                        });
                try {
                    String body = EntityUtils.toString(argument.getEntity());
                    softly.assertThat(body)
                            .as("body")
                            .isEqualTo("""
                                    {"query": "bar"}""");
                } catch (IOException | ParseException e) {
                    softly.fail("failed to validate body", e);
                }
            });
            return true;
        }));
        order.verifyNoMoreInteractions();
    }

    @Test
    void execute_should_call_ESRestClient_withMinimumResponse(@Mock(answer = Answers.RETURNS_DEEP_STUBS) Response response) throws IOException {
        when(restClient.performRequest(any()))
                .thenReturn(response);
        when(response.getStatusLine().getStatusCode())
                .thenReturn(200);
        when(response.getStatusLine().getReasonPhrase())
                .thenReturn(null);
        when(response.getEntity())
                .thenReturn(null);


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

        final InOrder order = inOrder(restClient);
        order.verify(restClient).performRequest(argThat(argument -> {
            assertSoftly(softly -> {
                softly.assertThat(argument.getMethod())
                        .as("HTTP method")
                        .isEqualTo("POST");
                softly.assertThat(argument.getEndpoint())
                        .as("endpoint")
                        .isEqualTo("/_search");
                softly.assertThat(argument.getParameters())
                        .as("URL parameters")
                        .isNullOrEmpty();
                softly.assertThat(argument.getOptions().getHeaders())
                        .as("headers")
                        .isEmpty();
                softly.assertThat(argument.getEntity())
                        .as("body")
                        .isNull();
            });
            return true;
        }));
        order.verifyNoMoreInteractions();
    }
}