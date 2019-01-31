package com.senacor.elasticsearch.evolution.core;

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
import static org.assertj.core.api.Assertions.assertThat;
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
    void migrate_Failed() throws IOException {
        ElasticsearchEvolution underTest = ElasticsearchEvolution.configure()
                .setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/migrate"))
                .load(restHighLevelClient);

        assertThat(underTest.migrate())
                .isEqualTo(1);

        InOrder order = inOrder(restHighLevelClient, restClient);
        order.verify(restHighLevelClient, times(3)).getLowLevelClient();
        order.verify(restClient).getNodes();
        order.verify(restClient).performRequest(any());
        order.verifyNoMoreInteractions();
    }
}