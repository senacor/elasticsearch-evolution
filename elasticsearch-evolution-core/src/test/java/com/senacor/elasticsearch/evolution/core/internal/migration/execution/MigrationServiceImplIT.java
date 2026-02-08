package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfigImpl;
import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.FileNameInfoImpl;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.MigrationScriptRequest;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigration;
import com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension;
import com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension.ElasticsearchArgumentsProvider;
import com.senacor.elasticsearch.evolution.core.test.EsUtils;
import com.senacor.elasticsearch.evolution.rest.abstraction.EvolutionRestClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.senacor.elasticsearch.evolution.core.api.migration.MigrationVersion.fromVersion;
import static com.senacor.elasticsearch.evolution.rest.abstraction.HttpMethod.PUT;
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

    private final Charset encoding = StandardCharsets.UTF_8;
    private final String defaultContentType = EvolutionRestClient.APPLICATION_JSON_UTF8;

    @Nested
    class executeScript {

        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(ElasticsearchArgumentsProvider.class)
        void OK_indexDocumentIsWrittenToElasticsearch(String versionInfo, EsUtils esUtils) {
            String index = "myindex";
            ParsedMigration<MigrationScriptRequest> script = createParsedMigrationScript("1.1", index);

            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepositoryMock,
                    0, 0, esUtils.getEvolutionRestClient(),
                    new ElasticsearchEvolutionConfigImpl()
                            .setDefaultContentType(defaultContentType)
                            .setEncoding(encoding)
                            .setValidateOnMigrate(true)
                            .setBaselineVersion("1.0")
                            .setOutOfOrder(false)
            );

            MigrationScriptProtocol res = underTest.executeMigration(script).getProtocol();

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
                        .isBetween(1, 8000);
                softly.assertThat(res.getExecutionTimestamp())
                        .isBetween(afterExecution.minus(8000, ChronoUnit.MILLIS), afterExecution);
            });

            // wait until all documents are indexed
            esUtils.refreshIndices();

            List<String> allDocuments = esUtils.fetchAllDocuments(index);
            assertThat(allDocuments)
                    .hasSize(1)
                    .contains(script.getMigrationRequest().getBody());
        }
    }

    private ParsedMigration<MigrationScriptRequest> createParsedMigrationScript(String version, String index) {
        return new ParsedMigration<MigrationScriptRequest>()
                .setFileNameInfo(
                        new FileNameInfoImpl(fromVersion(version), version, createDefaultScriptName(version)))
                .setChecksum(1)
                .setMigrationRequest(new MigrationScriptRequest()
                        .setHttpMethod(PUT)
                        .setPath(index + "/" + HistoryRepositoryImpl.INDEX_TYPE_DOC + "/1")
                        .setBody("{\"user\":\"kimchy\",\"post_date\":\"2009-11-15T14:12:12\"}"));
    }

    private String createDefaultScriptName(String version) {
        return "V" + version + "__" + version + ".http";
    }
}