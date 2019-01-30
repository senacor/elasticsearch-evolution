package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.internal.model.MigrationVersion;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.FileNameInfoImpl;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.MigrationScriptRequest;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigrationScript;
import com.senacor.elasticsearch.evolution.core.test.ArgumentProviders;
import com.senacor.elasticsearch.evolution.core.test.MockitoExtension;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.rest.RestStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import static com.senacor.elasticsearch.evolution.core.internal.model.MigrationVersion.fromVersion;
import static com.senacor.elasticsearch.evolution.core.internal.model.migration.MigrationScriptRequest.HttpMethod.DELETE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * @author Andreas Keefer
 */
@ExtendWith(MockitoExtension.class)
class MigrationServiceImplTest {

    @Mock
    private HistoryRepository historyRepository;
    @Mock
    private RestClient restClient;

    private Charset encoding = StandardCharsets.UTF_8;
    private ContentType defaultContentType = ContentType.APPLICATION_JSON;

    @Nested
    class waitUntilUnlocked {

        @Test
        void noLockExists() {
            doReturn(false).when(historyRepository).isLocked();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    2000, 2000, restClient, defaultContentType, encoding);

            assertTimeout(Duration.ofSeconds(1), underTest::waitUntilUnlocked);

            InOrder order = inOrder(historyRepository);
            order.verify(historyRepository).isLocked();
            order.verifyNoMoreInteractions();
        }

        @Test
        void LockExistsAndGetsReleased() {
            doReturn(true, false).when(historyRepository).isLocked();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    100, 100, restClient, defaultContentType, encoding);

            assertTimeout(Duration.ofMillis(200), underTest::waitUntilUnlocked);

            InOrder order = inOrder(historyRepository);
            order.verify(historyRepository, times(2)).isLocked();
            order.verifyNoMoreInteractions();
        }
    }

    @Nested
    class getPendingScriptsToBeExecuted {
        @Test
        void emptyHistory_allScriptsHaveToBeReturned() {
            doReturn(new TreeSet<>()).when(historyRepository).findAll();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);
            ParsedMigrationScript parsedMigrationScript1_1 = createParsedMigrationScript("1.1");
            ParsedMigrationScript parsedMigrationScript1_0 = createParsedMigrationScript("1.0");
            List<ParsedMigrationScript> parsedMigrationScripts = asList(
                    parsedMigrationScript1_1,
                    parsedMigrationScript1_0);

            List<ParsedMigrationScript> res = underTest.getPendingScriptsToBeExecuted(parsedMigrationScripts);

            assertThat(res).hasSize(2);
            assertThat(res.get(0).getFileNameInfo().getVersion()).isEqualTo(fromVersion("1.0"));
            assertThat(res.get(1).getFileNameInfo().getVersion()).isEqualTo(fromVersion("1.1"));
            InOrder order = inOrder(historyRepository);
            order.verify(historyRepository).findAll();
            order.verifyNoMoreInteractions();
        }

        @Test
        void scriptsAndHistoryInSync_noScriptsWillBeReturned() {
            doReturn(new TreeSet<>(asList(
                    createMigrationScriptProtocol("1.0", true),
                    createMigrationScriptProtocol("1.1", true)
            ))).when(historyRepository).findAll();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            List<ParsedMigrationScript> parsedMigrationScripts = asList(
                    createParsedMigrationScript("1.1"),
                    createParsedMigrationScript("1.0"));

            List<ParsedMigrationScript> res = underTest.getPendingScriptsToBeExecuted(parsedMigrationScripts);

            assertThat(res).hasSize(0);
            InOrder order = inOrder(historyRepository);
            order.verify(historyRepository).findAll();
            order.verifyNoMoreInteractions();
        }

        @Test
        void lastHistoryVersionWasFailing_AllScriptsInclFailedWillBeReturned() {
            doReturn(new TreeSet<>(asList(
                    createMigrationScriptProtocol("1.0", true),
                    createMigrationScriptProtocol("1.1", false)
            ))).when(historyRepository).findAll();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            ParsedMigrationScript parsedMigrationScript1_0 = createParsedMigrationScript("1.0");
            ParsedMigrationScript parsedMigrationScript1_1 = createParsedMigrationScript("1.1");
            List<ParsedMigrationScript> parsedMigrationScripts = asList(
                    parsedMigrationScript1_1,
                    parsedMigrationScript1_0);

            List<ParsedMigrationScript> res = underTest.getPendingScriptsToBeExecuted(parsedMigrationScripts);

            assertThat(res).hasSize(1);
            assertThat(res.get(0)).isSameAs(parsedMigrationScript1_1);
            InOrder order = inOrder(historyRepository);
            order.verify(historyRepository).findAll();
            order.verifyNoMoreInteractions();
        }

        @Test
        void moreHistoryVersionsThanScripts_warningIsShownAnNoScriptsWillBeReturned() {
            doReturn(new TreeSet<>(asList(
                    createMigrationScriptProtocol("1.0", true),
                    createMigrationScriptProtocol("1.1", true),
                    createMigrationScriptProtocol("1.2", false)
            ))).when(historyRepository).findAll();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            ParsedMigrationScript parsedMigrationScript1_0 = createParsedMigrationScript("1.0");
            ParsedMigrationScript parsedMigrationScript1_1 = createParsedMigrationScript("1.1");
            List<ParsedMigrationScript> parsedMigrationScripts = asList(
                    parsedMigrationScript1_1,
                    parsedMigrationScript1_0);

            List<ParsedMigrationScript> res = underTest.getPendingScriptsToBeExecuted(parsedMigrationScripts);

            assertThat(res).hasSize(0);
            InOrder order = inOrder(historyRepository);
            order.verify(historyRepository).findAll();
            order.verifyNoMoreInteractions();
        }

        @Test
        void outOfOrderExecutionIsNotSupported() {
            doReturn(new TreeSet<>(asList(
                    createMigrationScriptProtocol("1.0", true),
                    createMigrationScriptProtocol("1.1", true)
            ))).when(historyRepository).findAll();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            ParsedMigrationScript parsedMigrationScript1_0 = createParsedMigrationScript("1.0");
            ParsedMigrationScript parsedMigrationScript1_0_1 = createParsedMigrationScript("1.0.1");
            ParsedMigrationScript parsedMigrationScript1_1 = createParsedMigrationScript("1.1");
            List<ParsedMigrationScript> parsedMigrationScripts = asList(
                    parsedMigrationScript1_1,
                    parsedMigrationScript1_0_1,
                    parsedMigrationScript1_0);

            assertThatThrownBy(() -> underTest.getPendingScriptsToBeExecuted(parsedMigrationScripts))
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("The logged execution in the Elasticsearch-Evolution history index at position 1 is version 1.1 and in the same position in the given migration scripts is version 1.0.1! Out of order execution is not supported. Or maybe you have added new migration scripts in between or have to cleanup the Elasticsearch-Evolution history index manually");
        }
    }

    @Nested
    class executeScript {
        @Test
        void OK_resultIsSetCorrect() throws IOException {
            ParsedMigrationScript script = createParsedMigrationScript("1.1");
            Response responseMock = createResponseMock(200);
            doReturn(responseMock).when(restClient).performRequest(any());

            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            MigrationScriptProtocol res = underTest.executeScript(script);

            OffsetDateTime afterExecution = OffsetDateTime.now();
            assertSoftly(softly -> {
                softly.assertThat(res.getVersion())
                        .isEqualTo(script.getFileNameInfo().getVersion())
                        .isNotNull();
                softly.assertThat(res.getChecksum())
                        .isEqualTo(script.getChecksum())
                        .isNotNull();
                softly.assertThat(res.getDescription())
                        .isEqualTo(script.getFileNameInfo().getDescription())
                        .isNotNull();
                softly.assertThat(res.getScriptName())
                        .isEqualTo(script.getFileNameInfo().getScriptName())
                        .isNotNull();
                softly.assertThat(res.isSuccess())
                        .isTrue();
                softly.assertThat(res.isLocked())
                        .isTrue();
                softly.assertThat(res.getExecutionRuntimeInMillis())
                        .isBetween(0, 200);
                softly.assertThat(res.getExecutionTimestamp())
                        .isBetween(afterExecution.minus(200, ChronoUnit.MILLIS), afterExecution);
            });

            InOrder order = inOrder(historyRepository, restClient);
            order.verify(restClient).performRequest(argThat(argument -> {
                assertSoftly(softly -> {
                    softly.assertThat(argument.getMethod())
                            .isEqualToIgnoringCase(script.getMigrationScriptRequest().getHttpMethod().name())
                            .isNotNull();
                    softly.assertThat(argument.getEndpoint())
                            .isEqualTo(script.getMigrationScriptRequest().getPath())
                            .isNotNull();
                    softly.assertThat(argument.getEntity())
                            .isNull();
                    softly.assertThat(argument.getOptions().getHeaders())
                            .hasSize(script.getMigrationScriptRequest().getHttpHeader().size())
                            .isEmpty();
                });
                return true;
            }));
            order.verifyNoMoreInteractions();
        }

        @Test
        void OK_requestWithBody() throws IOException {
            ParsedMigrationScript script = createParsedMigrationScript("1.1");
            script.getMigrationScriptRequest().setBody("my-body");
            Response responseMock = createResponseMock(200);
            doReturn(responseMock).when(restClient).performRequest(any());

            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            MigrationScriptProtocol res = underTest.executeScript(script);

            assertThat(res.isSuccess()).isTrue();

            InOrder order = inOrder(historyRepository, restClient);
            order.verify(restClient).performRequest(argThat(argument -> {
                assertSoftly(softly -> {
                    softly.assertThat(argument.getMethod())
                            .isEqualToIgnoringCase(script.getMigrationScriptRequest().getHttpMethod().name())
                            .isNotNull();
                    softly.assertThat(argument.getEndpoint())
                            .isEqualTo(script.getMigrationScriptRequest().getPath())
                            .isNotNull();
                    softly.assertThat(argument.getEntity().getContentLength())
                            .isEqualTo(script.getMigrationScriptRequest().getBody().length());
                    softly.assertThat(argument.getEntity().getContentType().getValue())
                            .isEqualTo(defaultContentType.toString());
                    softly.assertThat(argument.getEntity().getContentEncoding())
                            .isNull();
                    softly.assertThat(argument.getOptions().getHeaders())
                            .hasSize(script.getMigrationScriptRequest().getHttpHeader().size())
                            .isEmpty();
                });
                return true;
            }));
            order.verifyNoMoreInteractions();
        }

        @Test
        void OK_requestWithCustomContentTypeAndDefaultCharset() throws IOException {
            ParsedMigrationScript script = createParsedMigrationScript("1.1");
            String contentType = "text/plain";
            script.getMigrationScriptRequest().setBody("my-body")
                    .addHttpHeader("content-type", contentType);
            Response responseMock = createResponseMock(200);
            doReturn(responseMock).when(restClient).performRequest(any());

            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            MigrationScriptProtocol res = underTest.executeScript(script);

            assertThat(res.isSuccess()).isTrue();

            InOrder order = inOrder(historyRepository, restClient);
            order.verify(restClient).performRequest(argThat(argument -> {
                assertSoftly(softly -> {
                    softly.assertThat(argument.getMethod())
                            .isEqualToIgnoringCase(script.getMigrationScriptRequest().getHttpMethod().name())
                            .isNotNull();
                    softly.assertThat(argument.getEndpoint())
                            .isEqualTo(script.getMigrationScriptRequest().getPath())
                            .isNotNull();
                    softly.assertThat(argument.getEntity().getContentLength())
                            .isEqualTo(script.getMigrationScriptRequest().getBody().length());
                    softly.assertThat(argument.getEntity().getContentType().getValue())
                            .isEqualTo(contentType + "; charset=" + encoding);
                    softly.assertThat(argument.getEntity().getContentEncoding())
                            .isNull();
                    softly.assertThat(argument.getOptions().getHeaders())
                            .hasSize(script.getMigrationScriptRequest().getHttpHeader().size());
                });
                return true;
            }));
            order.verifyNoMoreInteractions();
        }

        @Test
        void OK_requestWithCustomContentTypeAndCustomCharset() throws IOException {
            ParsedMigrationScript script = createParsedMigrationScript("1.1");
            String contentType = "text/plain; charset=" + StandardCharsets.ISO_8859_1;
            script.getMigrationScriptRequest().setBody("my-body")
                    .addHttpHeader("content-type", contentType);
            Response responseMock = createResponseMock(200);
            doReturn(responseMock).when(restClient).performRequest(any());

            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            MigrationScriptProtocol res = underTest.executeScript(script);

            assertThat(res.isSuccess()).isTrue();

            InOrder order = inOrder(historyRepository, restClient);
            order.verify(restClient).performRequest(argThat(argument -> {
                assertSoftly(softly -> {
                    softly.assertThat(argument.getMethod())
                            .isEqualToIgnoringCase(script.getMigrationScriptRequest().getHttpMethod().name())
                            .isNotNull();
                    softly.assertThat(argument.getEndpoint())
                            .isEqualTo(script.getMigrationScriptRequest().getPath())
                            .isNotNull();
                    softly.assertThat(argument.getEntity().getContentLength())
                            .isEqualTo(script.getMigrationScriptRequest().getBody().length());
                    softly.assertThat(argument.getEntity().getContentType().getValue())
                            .isEqualTo(contentType);
                    softly.assertThat(argument.getEntity().getContentEncoding())
                            .isNull();
                    softly.assertThat(argument.getOptions().getHeaders())
                            .hasSize(script.getMigrationScriptRequest().getHttpHeader().size());
                });
                return true;
            }));
            order.verifyNoMoreInteractions();
        }

        @Test
        void OK_requestWithCustomHeader() throws IOException {
            ParsedMigrationScript script = createParsedMigrationScript("1.1");
            String headerKey = "X-Custom-header";
            String headerValue = "custom-value";
            script.getMigrationScriptRequest()
                    .addHttpHeader(headerKey, headerValue);
            Response responseMock = createResponseMock(200);
            doReturn(responseMock).when(restClient).performRequest(any());

            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            MigrationScriptProtocol res = underTest.executeScript(script);

            assertThat(res.isSuccess()).isTrue();

            InOrder order = inOrder(historyRepository, restClient);
            order.verify(restClient).performRequest(argThat(argument -> {
                assertSoftly(softly -> {
                    softly.assertThat(argument.getMethod())
                            .isEqualToIgnoringCase(script.getMigrationScriptRequest().getHttpMethod().name())
                            .isNotNull();
                    softly.assertThat(argument.getEndpoint())
                            .isEqualTo(script.getMigrationScriptRequest().getPath())
                            .isNotNull();
                    softly.assertThat(argument.getEntity())
                            .isNull();
                    softly.assertThat(argument.getOptions().getHeaders())
                            .hasSize(1);
                    softly.assertThat(argument.getOptions().getHeaders().get(0).getName())
                            .isEqualTo(headerKey);
                    softly.assertThat(argument.getOptions().getHeaders().get(0).getValue())
                            .isEqualTo(headerValue);
                });
                return true;
            }));
            order.verifyNoMoreInteractions();
        }

        @ParameterizedTest
        @ArgumentsSource(HandledErrorsProvider.class)
        void executeScript_failed_status(Exception handledError) throws IOException {
            ParsedMigrationScript script = createParsedMigrationScript("1.1");
            doThrow(handledError).when(restClient).performRequest(any());

            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            MigrationScriptProtocol res = underTest.executeScript(script);

            assertThat(res.isSuccess()).isFalse();
        }

        @ParameterizedTest
        @ArgumentsSource(ArgumentProviders.FailingHttpCodesProvider.class)
        void executeScript_failed_status(RestStatus status) throws IOException {
            ParsedMigrationScript script = createParsedMigrationScript("1.1");
            Response responseMock = createResponseMock(status.getStatus());
            doReturn(responseMock).when(restClient).performRequest(any());

            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            MigrationScriptProtocol res = underTest.executeScript(script);

            assertThat(res.isSuccess()).isFalse();
        }

        @ParameterizedTest
        @ArgumentsSource(ArgumentProviders.SuccessHttpCodesProvider.class)
        void executeScript_OK_status(RestStatus status) throws IOException {
            ParsedMigrationScript script = createParsedMigrationScript("1.1");
            Response responseMock = createResponseMock(status.getStatus());
            doReturn(responseMock).when(restClient).performRequest(any());

            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            MigrationScriptProtocol res = underTest.executeScript(script);

            assertThat(res.isSuccess()).isTrue();
        }
    }

    @Nested
    class executePendingScripts {
        @Test
        void allOK() throws IOException {
            List<ParsedMigrationScript> scripts = asList(
                    createParsedMigrationScript("1.0"),
                    createParsedMigrationScript("1.1"));
            doReturn(false).when(historyRepository).isLocked();
            doReturn(true).when(historyRepository).lock();
            doReturn(true).when(historyRepository).unlock();
            doReturn(new TreeSet<>()).when(historyRepository).findAll();

            Response responseMock = createResponseMock(200);
            doReturn(responseMock).when(restClient).performRequest(any());
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            List<MigrationScriptProtocol> res = underTest.executePendingScripts(scripts);

            assertThat(res).hasSize(2)
                    .allMatch(MigrationScriptProtocol::isSuccess);
            InOrder order = inOrder(historyRepository, restClient);
            order.verify(historyRepository).createIndexIfAbsent();
            order.verify(historyRepository).isLocked();
            order.verify(historyRepository).lock();
            order.verify(historyRepository).findAll();
            order.verify(restClient).performRequest(any());
            order.verify(historyRepository).saveOrUpdate(any());
            order.verify(restClient).performRequest(any());
            order.verify(historyRepository).saveOrUpdate(any());
            order.verify(historyRepository).unlock();
            order.verifyNoMoreInteractions();
        }

        @Test
        void firstExecutionFailed() throws IOException {
            List<ParsedMigrationScript> scripts = asList(
                    createParsedMigrationScript("1.0"),
                    createParsedMigrationScript("1.1"));
            doReturn(false).when(historyRepository).isLocked();
            doReturn(true).when(historyRepository).lock();
            doReturn(true).when(historyRepository).unlock();
            doReturn(new TreeSet<>()).when(historyRepository).findAll();

            Response responseMock = createResponseMock(500);
            doReturn(responseMock).when(restClient).performRequest(any());
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            List<MigrationScriptProtocol> res = underTest.executePendingScripts(scripts);

            assertThat(res).hasSize(1)
                    .allMatch(migrationScriptProtocol -> !migrationScriptProtocol.isSuccess());
            assertThat(res.get(0).getVersion()).isEqualTo(MigrationVersion.fromVersion("1.0"));
            InOrder order = inOrder(historyRepository, restClient);
            order.verify(historyRepository).createIndexIfAbsent();
            order.verify(historyRepository).isLocked();
            order.verify(historyRepository).lock();
            order.verify(historyRepository).findAll();
            order.verify(restClient).performRequest(any());
            order.verify(historyRepository).saveOrUpdate(any());
            order.verify(historyRepository).unlock();
            order.verifyNoMoreInteractions();
        }

        @Test
        void error_unlockWasNotSuccessful() throws IOException {
            List<ParsedMigrationScript> scripts = asList(
                    createParsedMigrationScript("1.0"),
                    createParsedMigrationScript("1.1"));
            doReturn(false).when(historyRepository).isLocked();
            doReturn(true).when(historyRepository).lock();
            doReturn(false).when(historyRepository).unlock();
            doReturn(new TreeSet<>()).when(historyRepository).findAll();

            Response responseMock = createResponseMock(200);
            doReturn(responseMock).when(restClient).performRequest(any());
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            assertThatThrownBy(() -> underTest.executePendingScripts(scripts))
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("could not release the elasticsearch-evolution history index lock! Maybe you have to release it manually.");

            InOrder order = inOrder(historyRepository, restClient);
            order.verify(historyRepository).createIndexIfAbsent();
            order.verify(historyRepository).isLocked();
            order.verify(historyRepository).lock();
            order.verify(historyRepository).findAll();
            order.verify(restClient).performRequest(any());
            order.verify(historyRepository).saveOrUpdate(any());
            order.verify(restClient).performRequest(any());
            order.verify(historyRepository).saveOrUpdate(any());
            order.verify(historyRepository).unlock();
            order.verifyNoMoreInteractions();
        }

        @Test
        void error_lockWasNotSuccessful() {
            List<ParsedMigrationScript> scripts = asList(
                    createParsedMigrationScript("1.0"),
                    createParsedMigrationScript("1.1"));
            doReturn(false).when(historyRepository).isLocked();
            doReturn(false).when(historyRepository).lock();
            doReturn(true).when(historyRepository).unlock();

            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            assertThatThrownBy(() -> underTest.executePendingScripts(scripts))
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("could not lock the elasticsearch-evolution history index");


            InOrder order = inOrder(historyRepository, restClient);
            order.verify(historyRepository).createIndexIfAbsent();
            order.verify(historyRepository).isLocked();
            order.verify(historyRepository).lock();
            order.verify(historyRepository).unlock();
            order.verifyNoMoreInteractions();
        }

        @Test
        void emptyScriptsCollection() {
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository,
                    0, 0, restClient, defaultContentType, encoding);

            List<MigrationScriptProtocol> res = underTest.executePendingScripts(emptyList());

            assertThat(res).hasSize(0);
            InOrder order = inOrder(historyRepository, restClient);
            order.verifyNoMoreInteractions();
        }
    }

    private Response createResponseMock(int statusCode) {
        Response restResponse = mock(Response.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        StatusLine statusLine = restResponse.getStatusLine();
        doReturn(statusCode).when(statusLine).getStatusCode();
        return restResponse;
    }

    private MigrationScriptProtocol createMigrationScriptProtocol(String version, boolean success) {
        return new MigrationScriptProtocol()
                .setVersion(version)
                .setChecksum(1)
                .setSuccess(success)
                .setLocked(true)
                .setDescription(version)
                .setScriptName(createDefaultScriptName(version));
    }

    private ParsedMigrationScript createParsedMigrationScript(String version) {
        return new ParsedMigrationScript()
                .setFileNameInfo(
                        new FileNameInfoImpl(fromVersion(version), version, createDefaultScriptName(version)))
                .setChecksum(1)
                .setMigrationScriptRequest(new MigrationScriptRequest()
                        .setHttpMethod(DELETE)
                        .setPath("/"));
    }

    private String createDefaultScriptName(String version) {
        return "V" + version + "__" + version + ".http";
    }

    static class HandledErrorsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(Arguments.of(new NullPointerException("test-error")),
                    Arguments.of(new IOException("test-error")));
        }
    }

}