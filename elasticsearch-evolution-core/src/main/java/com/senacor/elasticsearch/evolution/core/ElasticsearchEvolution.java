package com.senacor.elasticsearch.evolution.core;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import com.senacor.elasticsearch.evolution.core.internal.migration.input.MigrationScriptParser;
import com.senacor.elasticsearch.evolution.core.internal.migration.input.MigrationScriptReader;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigrationScript;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

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

    private final ElasticsearchEvolutionConfig config;
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
        this.config = requireNonNull(elasticsearchEvolutionConfig, "elasticsearchEvolutionConfig must not be null")
                .validate();
        this.restHighLevelClient = requireNonNull(restHighLevelClient, "restHighLevelClient must not be null");

        logger.info("Created ElasticsearchEvolution with config='{}' and client='{}'",
                this.config, this.restHighLevelClient.getLowLevelClient().getNodes());
    }

    /**
     * <p>Starts the migration. All pending migrations will be applied in order.
     * Calling migrate on an up-to-date database has no effect.</p>
     *
     * @return The number of successfully applied migrations.
     * @throws MigrationException when the migration failed.
     */
    public int migrate() throws MigrationException {
        logger.info("start elasticsearch migration...");
        logger.info("reading migration scripts...");
        Collection<RawMigrationScript> rawMigrationScripts = readMigrationScripts();
        logger.info("parsing migration scripts...");
        Collection<ParsedMigrationScript> parsedMigrationScripts = parseMigrationScripts(rawMigrationScripts);

        // TODO (ak) impl
        return 0;
    }

    protected Collection<ParsedMigrationScript> parseMigrationScripts(Collection<RawMigrationScript> rawMigrationScripts) {
        return new MigrationScriptParser(
                config.getEsMigrationPrefix(),
                config.getEsMigrationSuffixes(),
                config.getPlaceholders(),
                config.getPlaceholderPrefix(),
                config.getPlaceholderSuffix(),
                config.isPlaceholderReplacement()
        )
                .parse(rawMigrationScripts);
    }

    protected Collection<RawMigrationScript> readMigrationScripts() {
        return new MigrationScriptReader(
                config.getLocations(),
                config.getEncoding(),
                config.getEsMigrationPrefix(),
                config.getEsMigrationSuffixes()
        )
                .read();
    }

    protected ElasticsearchEvolutionConfig getConfig() {
        return config;
    }

    protected RestHighLevelClient getRestHighLevelClient() {
        return restHighLevelClient;
    }
}
