package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension;
import com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension.ElasticsearchArgumentsProvider;
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
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

import static com.senacor.elasticsearch.evolution.core.internal.migration.execution.HistoryRepositoryImpl.INDEX_TYPE_DOC;
import static com.senacor.elasticsearch.evolution.core.internal.migration.execution.MigrationScriptProtocolMapper.*;
import static org.assertj.core.api.Assertions.assertThat;
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
        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void findAll(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            // TODO
        }
    }

    @Nested
    class saveOrUpdate {
        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void saveFullDocument(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) throws IOException {
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

            embeddedElastic.refreshIndices();
            List<String> res = embeddedElastic.fetchAllDocuments(INDEX);
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

        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void save2DocumentsWithDifferentVersions(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) throws IOException {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            underTest.saveOrUpdate(new MigrationScriptProtocol()
                    .setVersion("1.1"));
            underTest.saveOrUpdate(new MigrationScriptProtocol()
                    .setVersion("1.2"));

            embeddedElastic.refreshIndices();
            List<String> res = embeddedElastic.fetchAllDocuments(INDEX);
            assertThat(res).hasSize(2);
        }

        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void UpdateDocument(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) throws IOException {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            OffsetDateTime now = OffsetDateTime.now();
            underTest.saveOrUpdate(new MigrationScriptProtocol()
                    .setVersion("1.1")
                    .setDescription("d"));
            underTest.saveOrUpdate(new MigrationScriptProtocol()
                    .setVersion("1.2"));
            underTest.saveOrUpdate(new MigrationScriptProtocol()
                    .setVersion("1.1")
                    .setDescription("new"));

            embeddedElastic.refreshIndices();
            List<String> res = embeddedElastic.fetchAllDocuments(INDEX);
            assertThat(res).hasSize(2);
            GetResponse getRes = restHighLevelClient.get(new GetRequest(INDEX).id("1.1"), DEFAULT);
            assertThat(getRes.getSourceAsMap())
                    .containsEntry(DESCRIPTION_FIELD_NAME, "new")
                    .containsEntry(VERSION_FIELD_NAME, "1.1");
        }
    }

    @Nested
    class isLocked {
        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void emptyIndex_IsNotLocked(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.createIndexIfAbsent();

            embeddedElastic.refreshIndices();
            assertThat(underTest.isLocked())
                    .as("isLocked")
                    .isFalse();
        }

        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void noIndex_IsNotLocked(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            embeddedElastic.refreshIndices();
            assertThat(underTest.isLocked())
                    .as("isLocked")
                    .isFalse();
        }

        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void existingDocuments_IsNotLocked(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) throws Exception {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.createIndexIfAbsent();
            indexDocumentWithLock(false, embeddedElastic, restHighLevelClient);

            embeddedElastic.refreshIndices();
            assertThat(underTest.isLocked())
                    .as("isLocked")
                    .isFalse();
        }

        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void existingDocuments_IsLocked(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) throws Exception {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.createIndexIfAbsent();
            indexDocumentWithLock(true, embeddedElastic, restHighLevelClient);

            embeddedElastic.refreshIndices();
            assertThat(underTest.isLocked())
                    .as("isLocked")
                    .isTrue();
        }
    }

    @Nested
    class lock {

        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void noDocumentsInIndex(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.createIndexIfAbsent();
            embeddedElastic.refreshIndices();

            assertThat(underTest.lock()).isTrue();

            embeddedElastic.refreshIndices();
            assertThat(underTest.isLocked()).isTrue();
        }

        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void indexDoesNotExist(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            assertThat(underTest.lock()).isTrue();

            embeddedElastic.refreshIndices();
            assertThat(underTest.isLocked()).isTrue();
        }

        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void allExistingLockedDocumentsStayLocked(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) throws Exception {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            indexDocumentWithLock(true, embeddedElastic, restHighLevelClient);
            embeddedElastic.refreshIndices();

            assertThat(underTest.lock()).isTrue();

            embeddedElastic.refreshIndices();
            List<String> docs = embeddedElastic.fetchAllDocuments(INDEX);
            assertThat(docs).hasSize(1)
                    .contains("{\"" + LOCKED_FIELD_NAME + "\":true}");
            assertThat(underTest.isLocked()).isTrue();
        }

        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void allExistingUnlockedDocumentsGetsLocked(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) throws Exception {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            indexDocumentWithLock(false, embeddedElastic, restHighLevelClient);
            embeddedElastic.refreshIndices();

            assertThat(underTest.lock()).isTrue();

            embeddedElastic.refreshIndices();
            List<String> docs = embeddedElastic.fetchAllDocuments(INDEX);
            assertThat(docs).hasSize(1)
                    .contains("{\"" + LOCKED_FIELD_NAME + "\":true}");
            assertThat(underTest.isLocked()).isTrue();
        }
    }

    @Nested
    class unlock {
        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void noDocumentsInIndex(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.createIndexIfAbsent();
            embeddedElastic.refreshIndices();

            assertThat(underTest.unlock()).isTrue();

            embeddedElastic.refreshIndices();
            assertThat(underTest.isLocked()).isFalse();
        }

        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void indexDoesNotExist(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            assertThat(underTest.unlock()).isTrue();

            embeddedElastic.refreshIndices();
            assertThat(underTest.isLocked()).isFalse();
        }

        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void allExistingLockedDocumentsGetsUnlocked(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) throws Exception {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            underTest.lock();
            indexDocumentWithLock(true, embeddedElastic, restHighLevelClient);
            embeddedElastic.refreshIndices();
            assertThat(embeddedElastic.fetchAllDocuments(INDEX)).hasSize(2);

            assertThat(underTest.unlock()).isTrue();

            embeddedElastic.refreshIndices();
            List<String> docs = embeddedElastic.fetchAllDocuments(INDEX);
            assertThat(docs).hasSize(1)
                    .contains("{\"" + LOCKED_FIELD_NAME + "\":false}");
            assertThat(underTest.isLocked()).isFalse();
        }

        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void allExistingNonLockedDocumentsStayUnlocked(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) throws Exception {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);
            indexDocumentWithLock(false, embeddedElastic, restHighLevelClient);
            embeddedElastic.refreshIndices();

            assertThat(underTest.unlock()).isTrue();

            embeddedElastic.refreshIndices();
            List<String> docs = embeddedElastic.fetchAllDocuments(INDEX);
            assertThat(docs).hasSize(1)
                    .contains("{\"" + LOCKED_FIELD_NAME + "\":false}");
            assertThat(underTest.isLocked()).isFalse();
        }
    }

    @Nested
    class createIndexIfAbsent {
        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void indexDoesNotExistsYet_indexWillBeCreated(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) {
            HistoryRepositoryImpl underTest = createHistoryRepositoryImpl(restHighLevelClient);

            assertThat(underTest.createIndexIfAbsent()).as("new index created").isTrue();

            embeddedElastic.refreshIndices();

            assertThat(underTest.createIndexIfAbsent()).as("new index created").isFalse();
        }

    }

    private void indexDocumentWithLock(boolean locked, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) throws Exception {
        HashMap<String, Object> source = new HashMap<>();
        source.put(LOCKED_FIELD_NAME, locked);

        restHighLevelClient.index(
                new IndexRequest(INDEX)
                        .type(INDEX_TYPE_DOC)
                        .id(RandomStringUtils.randomNumeric(5))
                        .source(source),
                DEFAULT);

        embeddedElastic.refreshIndices();
        logger.debug("all documents in index '{}': {}", INDEX, embeddedElastic.fetchAllDocuments(INDEX));
    }

    private HistoryRepositoryImpl createHistoryRepositoryImpl(RestHighLevelClient restHighLevelClient) {
        return new HistoryRepositoryImpl(restHighLevelClient, INDEX, new MigrationScriptProtocolMapper());
    }
}