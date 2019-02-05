package com.senacor.elasticsearch.evolution.core;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.test.MockitoExtension;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
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
                .setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/migrate_OK"))
                .load(restHighLevelClient);
        when(restHighLevelClient.indices().refresh(any(), eq(DEFAULT)).getStatus())
                .thenReturn(RestStatus.OK);
        when(restHighLevelClient.count(any(), eq(DEFAULT)).status())
                .thenReturn(RestStatus.OK);
        when(restHighLevelClient.index(any(), eq(DEFAULT)).status())
                .thenReturn(RestStatus.OK);
        SearchResponse searchResponse = restHighLevelClient.search(any(), eq(DEFAULT));
        when(searchResponse.getHits().getHits())
                .thenReturn(new SearchHit[0]);
        when(searchResponse.status())
                .thenReturn(RestStatus.OK);

        assertThatThrownBy(underTest::migrate)
                .isInstanceOf(MigrationException.class)
                .hasMessage("execution of script 'FileNameInfoImpl{version=1, description='createTemplateWithIndexMapping', scriptName='V001.00__createTemplateWithIndexMapping.http'}' failed with HTTP status 0: null");

        InOrder order = inOrder(restHighLevelClient, restClient);
        order.verify(restHighLevelClient, times(3)).getLowLevelClient();
        order.verify(restClient).getNodes();
        order.verify(restClient).performRequest(any());
        order.verify(restHighLevelClient).index(any(), eq(DEFAULT));
        order.verify(restHighLevelClient).indices();
        order.verify(restHighLevelClient, times(2)).updateByQuery(any(), eq(DEFAULT));
        order.verifyNoMoreInteractions();
    }

    @Test
    void elasticsearchEvolutionIsNotEnabled() {
        int migrations = ElasticsearchEvolution.configure()
                .setEnabled(false)
                .load(restHighLevelClient)
                .migrate();

        assertThat(migrations).isEqualTo(0);

        InOrder order = inOrder(restHighLevelClient, restClient);
        order.verify(restHighLevelClient, times(3)).getLowLevelClient();
        order.verify(restClient).getNodes();
        order.verifyNoMoreInteractions();
    }

}