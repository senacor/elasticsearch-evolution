package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS;

/**
 * @author Andreas Keefer
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {EmbeddedElasticsearchConfiguration.class, SpringBootTestApplication.class})
@DirtiesContext(classMode = BEFORE_CLASS)
class ElasticsearchEvolutionAutoConfigurationIT {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private EmbeddedElastic embeddedElastic;

    @Test
    void migrateOnApplicationStartViaInitializer() throws IOException {
        embeddedElastic.refreshIndices();
        SearchResponse searchResponse = restHighLevelClient.search(
                new SearchRequest("test_*")
                        .source(new SearchSourceBuilder()
                                .query(QueryBuilders.termQuery("searchable.version", "1"))),
                DEFAULT);

        assertThat(searchResponse.getHits().totalHits)
                .as("searchResponse: %s", searchResponse)
                .isEqualTo(1);
    }

}