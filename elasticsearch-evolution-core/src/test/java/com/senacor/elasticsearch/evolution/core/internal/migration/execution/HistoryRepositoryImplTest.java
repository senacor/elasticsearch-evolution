package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.test.ArgumentProviders;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestClient;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestResponse;
import com.senacor.elasticsearch.evolution.rest.abstracion.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andreas Keefer
 */
@ExtendWith(MockitoExtension.class)
class HistoryRepositoryImplTest {

    private static final String INDEX = "es_evolution";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EvolutionRestClient evolutionRestClient;
    private HistoryRepositoryImpl underTest;

    @BeforeEach
    void setUp() {
        final ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        underTest = new HistoryRepositoryImpl(evolutionRestClient, INDEX, new MigrationScriptProtocolMapper(), 1000, objectMapper);
    }

    @Nested
    class findAll {
        @Test
        void failed() throws IOException {
            when(evolutionRestClient.execute(any(), anyString(), anyMap(), anyMap(), anyString()))
                    .thenThrow(new IOException("test error"));

            assertThatThrownBy(() -> underTest.findAll())
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("findAll failed!");
        }
    }

    @Nested
    class saveOrUpdate {
        @Test
        void failed() throws IOException {
            when(evolutionRestClient.execute(any(), anyString(), anyMap(), isNull(), anyString()))
                    .thenThrow(new IOException("test error"));
            MigrationScriptProtocol protocol = new MigrationScriptProtocol().setVersion("1");

            assertThatThrownBy(() -> underTest.saveOrUpdate(protocol))
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("saveOrUpdate of '%s' failed!", protocol);
        }
    }

    @Nested
    class isLocked {
        @Test
        void failed() throws IOException {
            when(evolutionRestClient.execute(any(), anyString(), nullable(Map.class), anyMap(), nullable(String.class)).statusCode())
                    .thenReturn(200);
            when(evolutionRestClient.execute(eq(HttpMethod.POST), endsWith("/_count"), anyMap(), anyMap(), anyString()))
                    .thenThrow(new IOException("test error"));

            assertThatThrownBy(() -> underTest.isLocked())
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("isLocked check failed!");
        }
    }

    @Nested
    class lock {
        @Test
        void failed() throws IOException {
            when(evolutionRestClient.execute(any(), anyString(), nullable(Map.class), anyMap(), nullable(String.class)))
                    .thenThrow(new IOException("test error"));

            assertThat(underTest.lock()).isFalse();
        }
    }

    @Nested
    class unlock {
        @Test
        void failed() throws IOException {
            final EvolutionRestResponse responseMock = mock(EvolutionRestResponse.class);
            when(evolutionRestClient.execute(any(), anyString(), nullable(Map.class), anyMap(), nullable(String.class)))
                    // first call is refresh, which must succeed
                    .thenReturn(responseMock)
                    // second call is updateByQuery which should fail
                    .thenThrow(new IOException("test error"));
            when(responseMock.statusCode())
                    .thenReturn(200);

            assertThat(underTest.unlock()).isFalse();
        }
    }

    @Nested
    class createIndexIfAbsent {
        @Test
        void failedCheckingIndex() throws IOException {
            when(evolutionRestClient.execute(HttpMethod.HEAD, "/" + INDEX))
                    .thenThrow(new IOException("test error"));

            assertThatThrownBy(() -> underTest.createIndexIfAbsent())
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("createIndexIfAbsent failed!");
        }

    }

    @Nested
    class validateStatus2xxOK {
        @ParameterizedTest
        @ArgumentsSource(ArgumentProviders.SuccessHttpCodesProvider.class)
        void isOK(int httpStatusCode) {
            assertThatCode(() -> underTest.validateHttpStatusIs2xx(httpStatusCode, "isOK"))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ArgumentsSource(ArgumentProviders.FailingHttpCodesProvider.class)
        void failed(int httpStatusCode) {
            String description = "failed";
            assertThatThrownBy(() -> underTest.validateHttpStatusIs2xx(httpStatusCode, description))
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("%s - response status is not OK: %s", description, httpStatusCode);
        }
    }

    @Nested
    class refresh {
        @Test
        void allIndices_failed() throws IOException {
            when(evolutionRestClient.execute(any(), anyString(), isNull(), anyMap(), isNull()))
                    .thenThrow(new IOException("foo"));

            assertThatThrownBy(() -> underTest.refresh())
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("refresh failed!");
        }
    }
}