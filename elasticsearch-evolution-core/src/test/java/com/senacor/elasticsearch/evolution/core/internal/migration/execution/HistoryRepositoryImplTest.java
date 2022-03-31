package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.test.ArgumentProviders;
import com.senacor.elasticsearch.evolution.core.test.MockitoExtension;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Answers;
import org.mockito.Mock;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Andreas Keefer
 */
@ExtendWith(MockitoExtension.class)
class HistoryRepositoryImplTest {

    private static final String INDEX = "es_evolution";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestHighLevelClient restHighLevelClient;
    private HistoryRepositoryImpl underTest;

    @BeforeEach
    void setUp() {
        underTest = new HistoryRepositoryImpl(restHighLevelClient, INDEX, new MigrationScriptProtocolMapper(), 1000);
    }

    @Nested
    class findAll {
        @Test
        void failed() throws IOException {
            when(restHighLevelClient.search(any(), eq(RequestOptions.DEFAULT)))
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
            when(restHighLevelClient.index(any(), eq(RequestOptions.DEFAULT)))
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
            when(
                    restHighLevelClient.getLowLevelClient().performRequest(any()).getStatusLine().getStatusCode()
            ).thenReturn(200);
            when(restHighLevelClient.indices().refresh(any(), eq(RequestOptions.DEFAULT)).getStatus())
                    .thenReturn(RestStatus.OK);
            when(restHighLevelClient.count(any(), eq(RequestOptions.DEFAULT)))
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
            when(restHighLevelClient.count(any(), eq(RequestOptions.DEFAULT)))
                    .thenThrow(new IOException("test error"));

            assertThat(underTest.lock()).isFalse();
        }
    }

    @Nested
    class unlock {
        @Test
        void failed() throws IOException {
            when(
                    restHighLevelClient.getLowLevelClient().performRequest(any()).getStatusLine().getStatusCode()
            ).thenReturn(200);

            when(restHighLevelClient.updateByQuery(any(), eq(RequestOptions.DEFAULT)))
                    .thenThrow(new IOException("test error"));

            assertThat(underTest.unlock()).isFalse();
        }
    }

    @Nested
    class createIndexIfAbsent {
        @Test
        void failedCheckingIndex() throws IOException {
            when(restHighLevelClient.getLowLevelClient().performRequest(new Request("HEAD", "/" + INDEX)))
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
        void isOK(RestStatus status) {
            underTest.validateHttpStatusIs2xx(status, "isOK");
        }

        @ParameterizedTest
        @ArgumentsSource(ArgumentProviders.FailingHttpCodesProvider.class)
        void failed(RestStatus status) {
            String description = "failed";
            assertThatThrownBy(() -> underTest.validateHttpStatusIs2xx(status, description))
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("%s - response status is not OK: %s", description, status.getStatus());
        }
    }

    @Nested
    class refresh {
        @Test
        void allIndices_failed() throws IOException {
            when(
                    restHighLevelClient.getLowLevelClient().performRequest(any())
            ).thenThrow(new IOException("foo"));

            assertThatThrownBy(() -> underTest.refresh())
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("refresh failed!");
        }
    }
}