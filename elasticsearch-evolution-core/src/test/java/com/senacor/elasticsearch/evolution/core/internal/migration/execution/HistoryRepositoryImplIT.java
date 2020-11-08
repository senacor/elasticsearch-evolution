package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.internal.model.MigrationVersion;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension;
import com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension.ElasticsearchArgumentsProvider;
import com.senacor.elasticsearch.evolution.core.test.EsUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.NavigableSet;

import static com.senacor.elasticsearch.evolution.core.internal.migration.execution.HistoryRepositoryImpl.INDEX_TYPE_DOC;
import static com.senacor.elasticsearch.evolution.core.internal.migration.execution.MigrationScriptProtocolMapper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

/**
 * @author Andreas Keefer
 */
@ExtendWith(EmbeddedElasticsearchExtension.class)
class HistoryRepositoryImplIT {

    private static final Logger logger = LoggerFactory.getLogger(HistoryRepositoryImplIT.class);
    private static final String INDEX = "es_evolution";

    @Nested
    class findAll {
        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void doesNotReturnProtocolsWithMajorVersion0(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("0.1"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.0"));
            underTest.refresh(INDEX);

            NavigableSet<MigrationScriptProtocol> all = underTest.findAll();

            assertThat(all).hasSize(1);
            assertThat(all.first().getVersion()).isEqualTo(MigrationVersion.fromVersion("1.0"));
        }

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void returnsProtocolsInVersionOrder(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.1"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("2.0"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.0"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.2"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.3"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.4"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.5"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.6"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.7"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.8"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.9"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.10"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.11"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.12"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.13"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.14"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.15"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.16"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.17"));
            underTest.saveOrUpdate(new MigrationScriptProtocol().setVersion("1.18"));
            underTest.refresh(INDEX);

            NavigableSet<MigrationScriptProtocol> all = underTest.findAll();

            assertThat(all).hasSize(20);
            assertThat(all.first().getVersion()).isEqualTo(MigrationVersion.fromVersion("1.0"));
            assertThat(all.last().getVersion()).isEqualTo(MigrationVersion.fromVersion("2.0"));
        }

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void returnsFullProtocol(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            OffsetDateTime executionTimestamp = OffsetDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            MigrationScriptProtocol protocol = new MigrationScriptProtocol()
                    .setVersion("1")
                    .setChecksum(1)
                    .setDescription("des")
                    .setLocked(false)
                    .setSuccess(true)
                    .setExecutionRuntimeInMillis(2)
                    .setExecutionTimestamp(executionTimestamp)
                    .setIndexName(INDEX)
                    .setScriptName("foo.http");
            underTest.saveOrUpdate(protocol);
            underTest.refresh(INDEX);

            NavigableSet<MigrationScriptProtocol> all = underTest.findAll();

            assertSoftly(softly -> {
                softly.assertThat(all).hasSize(1);
                softly.assertThat(all.first().getChecksum()).isEqualTo(1);
                softly.assertThat(all.first().getDescription()).isEqualTo("des");
                softly.assertThat(all.first().getExecutionRuntimeInMillis()).isEqualTo(2);
                softly.assertThat(all.first().getExecutionTimestamp()).isEqualTo(executionTimestamp);
                softly.assertThat(all.first().getIndexName()).isEqualTo(INDEX);
                softly.assertThat(all.first().getScriptName()).isEqualTo("foo.http");
                softly.assertThat(all.first().isLocked()).isEqualTo(false);
                softly.assertThat(all.first().isSuccess()).isEqualTo(true);
                softly.assertThat(all.first().getVersion()).isEqualTo(MigrationVersion.fromVersion("1"));
            });
        }
    }

    @Nested
    class saveOrUpdate {
        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void saveFullDocument(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) throws IOException {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            OffsetDateTime now = OffsetDateTime.now();
            String version = "1.1";
            underTest.saveOrUpdate(new MigrationScriptProtocol()
                    .setVersion(version)
                    .setScriptName("s")
                    .setDescription("d")
                    .setExecutionRuntimeInMillis(1)
                    .setSuccess(true)
                    .setChecksum(2)
                    .setExecutionTimestamp(now)
                    .setIndexName(INDEX)
                    .setLocked(true));

            esUtils.refreshIndices();
            List<String> res = esUtils.fetchAllDocuments(INDEX);
            String executionTimestamp = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            assertThat(res).hasSize(1)
                    .contains("{" +
                            "\"success\":true," +
                            "\"indexName\":\"" + INDEX + "\"," +
                            "\"checksum\":2," +
                            "\"description\":\"d\"," +
                            "\"executionRuntimeInMillis\":1," +
                            "\"scriptName\":\"s\"," +
                            "\"locked\":true," +
                            "\"version\":\"1.1\"," +
                            "\"executionTimestamp\":\"" + executionTimestamp + "\"" +
                            "}");
            GetResponse getRes = restHighLevelClient.get(new GetRequest(INDEX).id(version), DEFAULT);
            assertThat(getRes.getSourceAsMap())
                    .hasSize(9)
                    .containsEntry(INDEX_NAME_FIELD_NAME, INDEX)
                    .containsEntry(CHECKSUM_FIELD_NAME, 2)
                    .containsEntry(DESCRIPTION_FIELD_NAME, "d")
                    .containsEntry(SCRIPT_NAME_FIELD_NAME, "s")
                    .containsEntry(EXECUTION_RUNTIME_IN_MILLIS_FIELD_NAME, 1)
                    .containsEntry(LOCKED_FIELD_NAME, true)
                    .containsEntry(VERSION_FIELD_NAME, version)
                    .containsEntry(EXECUTION_TIMESTAMP_FIELD_NAME, executionTimestamp)
                    .containsEntry(SUCCESS_FIELD_NAME, true);
        }

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void save2DocumentsWithDifferentVersions(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) throws IOException {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            underTest.saveOrUpdate(new MigrationScriptProtocol()
                    .setVersion("1.1"));
            underTest.saveOrUpdate(new MigrationScriptProtocol()
                    .setVersion("1.2"));

            esUtils.refreshIndices();
            List<String> res = esUtils.fetchAllDocuments(INDEX);
            assertThat(res).hasSize(2);
        }

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void updateDocument(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) throws IOException {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            underTest.saveOrUpdate(new MigrationScriptProtocol()
                    .setVersion("1.1")
                    .setDescription("d"));
            underTest.saveOrUpdate(new MigrationScriptProtocol()
                    .setVersion("1.2"));
            underTest.saveOrUpdate(new MigrationScriptProtocol()
                    .setVersion("1.1")
                    .setDescription("new"));

            esUtils.refreshIndices();
            List<String> res = esUtils.fetchAllDocuments(INDEX);
            assertThat(res).hasSize(2);
            GetResponse getRes = restHighLevelClient.get(new GetRequest(INDEX).id("1.1"), DEFAULT);
            assertThat(getRes.getSourceAsMap())
                    .containsEntry(DESCRIPTION_FIELD_NAME, "new")
                    .containsEntry(VERSION_FIELD_NAME, "1.1");
        }
    }

    @Nested
    class isLocked {
        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void emptyIndex_IsNotLocked(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.createIndexIfAbsent();

            esUtils.refreshIndices();
            assertThat(underTest.isLocked())
                    .as("isLocked")
                    .isFalse();
        }

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void noIndex_IsNotLocked(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            esUtils.refreshIndices();
            assertThat(underTest.isLocked())
                    .as("isLocked")
                    .isFalse();
        }

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void existingDocuments_IsNotLocked(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) throws Exception {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.createIndexIfAbsent();
            indexDocumentWithLock(false, esUtils, restHighLevelClient);

            esUtils.refreshIndices();
            assertThat(underTest.isLocked())
                    .as("isLocked")
                    .isFalse();
        }

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void existingDocuments_IsLocked(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) throws Exception {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.createIndexIfAbsent();
            indexDocumentWithLock(true, esUtils, restHighLevelClient);

            esUtils.refreshIndices();
            assertThat(underTest.isLocked())
                    .as("isLocked")
                    .isTrue();
        }
    }

    @Nested
    class lock {

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void noDocumentsInIndex(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.createIndexIfAbsent();
            esUtils.refreshIndices();

            assertThat(underTest.lock()).isTrue();

            esUtils.refreshIndices();
            assertThat(underTest.isLocked()).isTrue();
        }

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void indexDoesNotExist(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            assertThat(underTest.lock()).isTrue();

            esUtils.refreshIndices();
            assertThat(underTest.isLocked()).isTrue();
        }

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void allExistingLockedDocumentsStayLocked(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) throws Exception {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            indexDocumentWithLock(true, esUtils, restHighLevelClient);
            esUtils.refreshIndices();

            assertThat(underTest.lock()).isTrue();

            esUtils.refreshIndices();
            List<String> docs = esUtils.fetchAllDocuments(INDEX);
            assertThat(docs).hasSize(1)
                    .contains("{\"" + LOCKED_FIELD_NAME + "\":true}");
            assertThat(underTest.isLocked()).isTrue();
        }

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void allExistingUnlockedDocumentsGetsLocked(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) throws Exception {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            indexDocumentWithLock(false, esUtils, restHighLevelClient);
            esUtils.refreshIndices();

            assertThat(underTest.lock()).isTrue();

            esUtils.refreshIndices();
            List<String> docs = esUtils.fetchAllDocuments(INDEX);
            assertThat(docs).hasSize(1)
                    .contains("{\"" + LOCKED_FIELD_NAME + "\":true}");
            assertThat(underTest.isLocked()).isTrue();
        }
    }

    @Nested
    class unlock {
        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void noDocumentsInIndex(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.createIndexIfAbsent();
            esUtils.refreshIndices();

            assertThat(underTest.unlock()).isTrue();

            esUtils.refreshIndices();
            assertThat(underTest.isLocked()).isFalse();
        }

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void indexDoesNotExist(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            assertThat(underTest.unlock()).isTrue();

            esUtils.refreshIndices();
            assertThat(underTest.isLocked()).isFalse();
        }

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void allExistingLockedDocumentsGetsUnlocked(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) throws Exception {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.lock();
            indexDocumentWithLock(true, esUtils, restHighLevelClient);
            esUtils.refreshIndices();
            assertThat(esUtils.fetchAllDocuments(INDEX)).hasSize(2);

            assertThat(underTest.unlock()).isTrue();

            esUtils.refreshIndices();
            List<String> docs = esUtils.fetchAllDocuments(INDEX);
            assertThat(docs).hasSize(1)
                    .contains("{\"" + LOCKED_FIELD_NAME + "\":false}");
            assertThat(underTest.isLocked()).isFalse();
        }

        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void allExistingNonLockedDocumentsStayUnlocked(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) throws Exception {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            indexDocumentWithLock(false, esUtils, restHighLevelClient);
            esUtils.refreshIndices();

            assertThat(underTest.unlock()).isTrue();

            esUtils.refreshIndices();
            List<String> docs = esUtils.fetchAllDocuments(INDEX);
            assertThat(docs).hasSize(1)
                    .contains("{\"" + LOCKED_FIELD_NAME + "\":false}");
            assertThat(underTest.isLocked()).isFalse();
        }
    }

    @Nested
    class createIndexIfAbsent {
        @ParameterizedTest(name = "esVersion: {0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void indexDoesNotExistsYet_indexWillBeCreated(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            assertThat(underTest.createIndexIfAbsent()).as("new index created").isTrue();

            esUtils.refreshIndices();

            assertThat(underTest.createIndexIfAbsent()).as("new index created").isFalse();
        }

    }

    private void indexDocumentWithLock(boolean locked, EsUtils esUtils, RestHighLevelClient restHighLevelClient) throws Exception {
        HashMap<String, Object> source = new HashMap<>();
        source.put(LOCKED_FIELD_NAME, locked);

        restHighLevelClient.index(
                new IndexRequest(INDEX)
                        .id(RandomStringUtils.randomNumeric(5))
                        .source(source),
                DEFAULT);

        esUtils.refreshIndices();
        logger.debug("all documents in index '{}': {}", INDEX, esUtils.fetchAllDocuments(INDEX));
    }

    private HistoryRepositoryImpl createHistoryRepositoryImpl(RestHighLevelClient restHighLevelClient) {
        return new HistoryRepositoryImpl(restHighLevelClient, INDEX, new MigrationScriptProtocolMapper(), 1000);
    }
}