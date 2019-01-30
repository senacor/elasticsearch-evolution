package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.FileNameInfoImpl;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.MigrationScriptRequest;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigrationScript;
import com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension;
import com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension.ElasticsearchArgumentsProvider;
import com.senacor.elasticsearch.evolution.core.test.MockitoExtension;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mock;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.senacor.elasticsearch.evolution.core.internal.model.MigrationVersion.fromVersion;
import static com.senacor.elasticsearch.evolution.core.internal.model.migration.MigrationScriptRequest.HttpMethod.PUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Integration Test with embedded elasticsearch
 *
 * @author Andreas Keefer
 */
@ExtendWith({
        MockitoExtension.class,
        EmbeddedElasticsearchExtension.class
})
class MigrationServiceImplIT {

    @Mock
    private HistoryRepository historyRepositoryMock;

    private Charset encoding = StandardCharsets.UTF_8;
    private ContentType defaultContentType = ContentType.APPLICATION_JSON;

    @Nested
    class executeScript {

        @ParameterizedTest
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void OK_indexDocumentIsWrittenToElasticsearch(String esVersion, EmbeddedElastic embeddedElastic, RestHighLevelClient restHighLevelClient) throws IOException {
            String index = "myindex";
            ParsedMigrationScript script = createParsedMigrationScript("1.1", index);

            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepositoryMock,
                    0, 0, restHighLevelClient.getLowLevelClient(),
                    defaultContentType, encoding);

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
                        .isBetween(1, 2000);
                softly.assertThat(res.getExecutionTimestamp())
                        .isBetween(afterExecution.minus(2000, ChronoUnit.MILLIS), afterExecution);
            });

            // wait until all documents are indexed
            embeddedElastic.refreshIndices();

            List<String> allDocuments = embeddedElastic.fetchAllDocuments(index);
            assertThat(allDocuments)
                    .hasSize(1)
                    .contains(script.getMigrationScriptRequest().getBody());
        }
    }

    private ParsedMigrationScript createParsedMigrationScript(String version, String index) {
        return new ParsedMigrationScript()
                .setFileNameInfo(
                        new FileNameInfoImpl(fromVersion(version), version, createDefaultScriptName(version)))
                .setChecksum(1)
                .setMigrationScriptRequest(new MigrationScriptRequest()
                        .setHttpMethod(PUT)
                        .setPath(index + "/" + HistoryRepositoryImpl.INDEX_TYPE_DOC + "/1")
                        .setBody("{\"user\":\"kimchy\",\"post_date\":\"2009-11-15T14:12:12\"}"));
    }

    private String createDefaultScriptName(String version) {
        return "V" + version + "__" + version + ".http";
    }
}