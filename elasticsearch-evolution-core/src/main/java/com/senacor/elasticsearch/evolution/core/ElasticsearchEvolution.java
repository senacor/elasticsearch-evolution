package com.senacor.elasticsearch.evolution.core;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * This is the centre point of Elasticsearch-Evolution, and for most users, the only class they will ever have to deal with.
 * <p>
 * It is THE public API from which all important Elasticsearch-Evolution functions such as migrate can be called.
 * </p>
 * <p>To get started all you need to do is</p>
 * <pre>
 * ElasticsearchEvolution esEvolution = ElasticsearchEvolution.configure()
 *   .load(new RestHighLevelClient(RestClient.builder(HttpHost.create(esUrl))));
 * esEvolution.migrate();
 * </pre>
 * <p>
 *
 * @author Andreas Keefer
 */
public class ElasticsearchEvolution {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchEvolution.class);

    private final ElasticsearchEvolutionConfig elasticsearchEvolutionConfig;
    private final RestHighLevelClient restHighLevelClient;

    /**
     * This is your starting point. This creates a configuration which can be customized to your needs before being
     * loaded into a new ElasticsearchEvolution instance using the load() method.
     * <p>In its simplest form, this is how you configure Flyway with all defaults to get started:</p>
     * <pre>
     * ElasticsearchEvolution esEvolution = ElasticsearchEvolution.configure()
     *     .load(new RestHighLevelClient(RestClient.builder(HttpHost.create(esUrl))));
     *  </pre>
     * <p>After that you have a fully-configured ElasticsearchEvolution instance at your disposal which can be used to
     * invoke ElasticsearchEvolution functionality such as migrate().</p>
     *
     * @return A new configuration from which ElasticsearchEvolution can be loaded.
     */
    public static ElasticsearchEvolutionConfig configure() {
        return new ElasticsearchEvolutionConfig();
    }

    /**
     * Create ElasticsearchEvolution
     *
     * @param elasticsearchEvolutionConfig configuration
     * @param restHighLevelClient          REST client to interact with Elasticsearch
     */
    public ElasticsearchEvolution(ElasticsearchEvolutionConfig elasticsearchEvolutionConfig,
                                  RestHighLevelClient restHighLevelClient) {
        this.elasticsearchEvolutionConfig = requireNonNull(elasticsearchEvolutionConfig, "elasticsearchEvolutionConfig must not be null")
                .validate();
        this.restHighLevelClient = requireNonNull(restHighLevelClient, "restHighLevelClient must not be null");

        logger.info("Created ElasticsearchEvolution with config='{}' and client='{}'",
                this.elasticsearchEvolutionConfig, this.restHighLevelClient.getLowLevelClient().getNodes());
    }

    /**
     * <p>Starts the migration. All pending migrations will be applied in order.
     * Calling migrate on an up-to-date database has no effect.</p>
     *
     * @return The number of successfully applied migrations.
     * @throws MigrationException when the migration failed.
     */
    public int migrate() throws MigrationException {
        System.out.println("migrate...");
        // TODO (ak) impl
        return 0;
    }
}
