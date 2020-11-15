package com.senacor.elasticsearch.evolution.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.internal.migration.execution.HistoryRepositoryImpl;
import com.senacor.elasticsearch.evolution.core.internal.migration.execution.MigrationScriptProtocolMapper;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension;
import com.senacor.elasticsearch.evolution.core.test.EmbeddedElasticsearchExtension.ElasticsearchArgumentsProvider;
import com.senacor.elasticsearch.evolution.core.test.EsUtils;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.NavigableSet;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

/**
 * @author Andreas Keefer
 */
@ExtendWith(EmbeddedElasticsearchExtension.class)
class ElasticsearchEvolutionIT {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchEvolutionIT.class);

    private HistoryRepository historyRepository;

    @ParameterizedTest(name = "esVersion: {0}")
    @ArgumentsSource(ElasticsearchArgumentsProvider.class)
    void migrate_OK(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) throws IOException {

        ElasticsearchEvolutionConfig elasticsearchEvolutionConfig = ElasticsearchEvolution.configure()
                .setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/migrate_OK"));
        String historyIndex = elasticsearchEvolutionConfig.getHistoryIndex();
        historyRepository = new HistoryRepositoryImpl(restHighLevelClient, historyIndex, new MigrationScriptProtocolMapper(), 1000);
        ElasticsearchEvolution underTest = elasticsearchEvolutionConfig
                .load(restHighLevelClient);

        assertSoftly(softly -> {
            softly.assertThat(underTest.migrate())
                    .as("# of successful executed scripts ")
                    .isEqualTo(7);
            softly.assertThat(historyRepository.findAll())
                    .as("# of historyIndex entries and all are successful")
                    .hasSize(7)
                    .allMatch(MigrationScriptProtocol::isSuccess);
        });
        esUtils.refreshIndices();

        String testIndex = "test_*";
        Response getIndexResponse = restHighLevelClient.getLowLevelClient().performRequest(new Request("GET", "/" + testIndex));
        String getIndexResponseBody = IOUtils.toString(getIndexResponse.getEntity().getContent(), StandardCharsets.UTF_8);
        logger.debug("GetIndexResponse: {}; BODY={}", getIndexResponse, getIndexResponseBody);
        Map<String, Map<String, Map>> bodyParsed = new ObjectMapper().readValue(getIndexResponseBody,
                new TypeReference<Map<String, Map<String, Map>>>() {
                });
        assertSoftly(softly -> {
            softly.assertThat(bodyParsed)
                    .hasSize(2)
                    .containsKeys("test_1", "test_2");
            softly.assertThat(bodyParsed.get("test_1").get("mappings"))
                    .as("'test_1' and 'test_2' mapping should be equals '%s'", bodyParsed)
                    .isNotNull()
                    .isNotEmpty()
                    .isEqualTo(bodyParsed.get("test_2").get("mappings"));
        });

        // search for indexed documents:
        SearchResponse search = restHighLevelClient.search(
                new SearchRequest(testIndex)
                        .source(new SearchSourceBuilder()
                                .query(QueryBuilders.boolQuery()
                                        .must(QueryBuilders.termsQuery("searchable.version", "1", "2")))),
                DEFAULT);
        assertThat(search.getHits().getTotalHits().value)
                .as("search res by version: %s", search)
                .isEqualTo(3);

        search = restHighLevelClient.search(
                new SearchRequest(testIndex)
                        .source(new SearchSourceBuilder()
                                .query(QueryBuilders.boolQuery()
                                        .must(QueryBuilders.termsQuery("searchable.version.text", "1", "2")))),
                DEFAULT);
        assertThat(search.getHits().getTotalHits().value)
                .as("search res by version.text: %s", search)
                .isEqualTo(3);

        search = restHighLevelClient.search(
                new SearchRequest(testIndex)
                        .source(new SearchSourceBuilder()
                                .query(QueryBuilders.boolQuery()
                                        .must(QueryBuilders.termsQuery("searchable.version.bm25", "1", "2")))),
                DEFAULT);
        assertThat(search.getHits().getTotalHits().value)
                .as("search res by version.bm25: %s", search)
                .isEqualTo(3);
    }

    @ParameterizedTest(name = "esVersion: {0}")
    @ArgumentsSource(ElasticsearchArgumentsProvider.class)
    void migrate_failed_then_fixed_script_and_re_execute(String esVersion, EsUtils esUtils, RestHighLevelClient restHighLevelClient) {

        ElasticsearchEvolutionConfig elasticsearchEvolutionConfig = ElasticsearchEvolution.configure()
                .setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/migrate_failed_step1"));
        historyRepository = new HistoryRepositoryImpl(restHighLevelClient, elasticsearchEvolutionConfig.getHistoryIndex(), new MigrationScriptProtocolMapper(), 1000);

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> elasticsearchEvolutionConfig.load(restHighLevelClient).migrate())
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("execution of script 'FileNameInfoImpl{version=1.1, description='addDocument', scriptName='V001.01__addDocument.http'}' failed");
            NavigableSet<MigrationScriptProtocol> protocols = historyRepository.findAll();
            softly.assertThat(protocols)
                    .as("# of historyIndex entries (%s)", protocols)
                    .hasSize(2);
            softly.assertThat(protocols.first().isSuccess())
                    .as("first protocol.isSuccess")
                    .isTrue();
            softly.assertThat(protocols.last().isSuccess())
                    .as("last protocol.isSuccess")
                    .isFalse();
        });

        elasticsearchEvolutionConfig.setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/migrate_failed_step2_fixed"));

        assertSoftly(softly -> {
            softly.assertThat(elasticsearchEvolutionConfig.load(restHighLevelClient).migrate())
                    .as("# of successful executed scripts")
                    .isEqualTo(1);
            NavigableSet<MigrationScriptProtocol> protocols = historyRepository.findAll();
            softly.assertThat(protocols)
                    .as("# of historyIndex entries and all are successful")
                    .hasSize(2)
                    .allMatch(MigrationScriptProtocol::isSuccess);
        });
    }
}