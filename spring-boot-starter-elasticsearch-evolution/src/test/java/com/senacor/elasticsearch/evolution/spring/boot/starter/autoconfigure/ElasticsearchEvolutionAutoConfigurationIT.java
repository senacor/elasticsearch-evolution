package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS;

/**
 * @author Andreas Keefer
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.elasticsearch.evolution.es.rest5client.enabled=false",
                "spring.elasticsearch.evolution.os.restclient.enabled=false",
                "spring.elasticsearch.evolution.os.genericclient.enabled=false",
        })
@ContextConfiguration(classes = {EmbeddedElasticsearchConfiguration.class, SpringBootTestApplication.class})
@DirtiesContext(classMode = BEFORE_CLASS)
class ElasticsearchEvolutionAutoConfigurationIT {

    @Autowired
    private RestClient restClient;
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private EsUtils esUtils;

    @BeforeEach
    void setUp() {
        restHighLevelClient = new RestHighLevelClient(RestClient.builder(restClient.getNodes().toArray(new Node[0])));
    }

    @Test
    void migrateOnApplicationStartViaInitializer() throws IOException {
        esUtils.refreshIndices();
        SearchResponse searchResponse = restHighLevelClient.search(
                new SearchRequest("test_*")
                        .source(new SearchSourceBuilder()
                                .query(QueryBuilders.termQuery("searchable.version", "1"))),
                DEFAULT);

        assertThat(searchResponse.getHits().getTotalHits().value)
                .as("searchResponse: %s", searchResponse)
                .as("Documents created by migration files")
                .isOne();
    }

}