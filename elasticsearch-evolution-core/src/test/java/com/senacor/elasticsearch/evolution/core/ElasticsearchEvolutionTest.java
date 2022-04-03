package com.senacor.elasticsearch.evolution.core;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.test.MockitoExtension;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.*;

/**
 * @author Andreas Keefer
 */
@ExtendWith(MockitoExtension.class)
class ElasticsearchEvolutionTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private RestHighLevelClient restHighLevelClient;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = restHighLevelClient.getLowLevelClient();
        when(restClient.getNodes())
                .thenReturn(singletonList(new Node(HttpHost.create("http://localhost:9200"))));
    }

    @Test
    void migrate_historyMaxQuerySizeToLow() throws IOException {
        String indexName = "es_evolution";
        int historyMaxQuerySize = 6;
        ElasticsearchEvolution underTest = ElasticsearchEvolution.configure()
                .setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/migrate_OK"))
                .setHistoryIndex(indexName)
                .setHistoryMaxQuerySize(historyMaxQuerySize)
                .load(restHighLevelClient.getLowLevelClient());

        assertThatThrownBy(underTest::migrate)
                .isInstanceOf(MigrationException.class)
                .hasMessage("configured historyMaxQuerySize of '%s' is to low for the number of migration scripts of '%s'", historyMaxQuerySize, 7);

        InOrder order = inOrder(restHighLevelClient, restClient);
        order.verify(restHighLevelClient, times(2)).getLowLevelClient();
        order.verify(restClient).getNodes();
        order.verifyNoMoreInteractions();
    }

    @Test
    void migrate_empty_location() {
        ElasticsearchEvolution underTest = ElasticsearchEvolution.configure()
                .setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/empty_location"))
                .load(restHighLevelClient.getLowLevelClient());

        assertThatCode(underTest::migrate)
                .doesNotThrowAnyException();
    }

    @Test
    void migrate_non_existing_location() {
        ElasticsearchEvolution underTest = ElasticsearchEvolution.configure()
                .setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/does_not_exist"))
                .load(restHighLevelClient.getLowLevelClient());

        assertThatCode(underTest::migrate)
                .doesNotThrowAnyException();
    }

    @Test
    void elasticsearchEvolutionIsNotEnabled() {
        int migrations = ElasticsearchEvolution.configure()
                .setEnabled(false)
                .load(restHighLevelClient.getLowLevelClient())
                .migrate();

        assertThat(migrations).isZero();

        InOrder order = inOrder(restHighLevelClient, restClient);
        order.verify(restHighLevelClient, times(2)).getLowLevelClient();
        order.verify(restClient).getNodes();
        order.verifyNoMoreInteractions();
    }

}