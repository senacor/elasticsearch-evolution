package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.elasticsearch.client.RestClient;
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
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private EsUtils esUtils;

    @BeforeEach
    void setUp() {
        elasticsearchClient = new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
    }

    @Test
    void migrateOnApplicationStartViaInitializer() throws IOException {
        esUtils.refreshIndices();
        SearchResponse searchResponse = elasticsearchClient.search(search -> search
                        .index("test_*")
                        .query(q -> q.term(t -> t
                                .field("searchable.version")
                                .value("1"))));

        assertThat(searchResponse.hits().total().value())
                .as("searchResponse: %s", searchResponse)
                .as("Documents created by migration files")
                .isOne();
    }

}