package com.senacor.elasticsearch.evolution.core;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationScriptParser;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationScriptReader;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationService;
import com.senacor.elasticsearch.evolution.core.internal.migration.execution.HistoryRepositoryImpl;
import com.senacor.elasticsearch.evolution.core.internal.migration.execution.MigrationServiceImpl;
import com.senacor.elasticsearch.evolution.core.internal.migration.input.MigrationScriptParserImpl;
import com.senacor.elasticsearch.evolution.core.internal.migration.input.MigrationScriptReaderImpl;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigrationScript;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

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

    private final MigrationScriptReader migrationScriptReader;
    private final MigrationScriptParser migrationScriptParser;
    private final MigrationService migrationService;

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

        this.migrationScriptReader = createMigrationScriptReader();
        this.migrationScriptParser = createMigrationScriptParser();
        this.migrationService = createMigrationService();

        logger.info("Created ElasticsearchEvolution with config='{}' and client='{}'",
                this.getConfig(), this.getRestHighLevelClient().getLowLevelClient().getNodes());
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
        Collection<RawMigrationScript> rawMigrationScripts = migrationScriptReader.read();
        logger.info("parsing migration scripts...");
        Collection<ParsedMigrationScript> parsedMigrationScripts = migrationScriptParser.parse(rawMigrationScripts);
        logger.info("execute migration scripts...");
        List<MigrationScriptProtocol> executedScripts = migrationService.executePendingScripts(parsedMigrationScripts);
        return executedScripts.size();
    }

    protected ElasticsearchEvolutionConfig getConfig() {
        return config;
    }

    protected RestHighLevelClient getRestHighLevelClient() {
        return restHighLevelClient;
    }

    protected MigrationScriptParser createMigrationScriptParser() {
        return new MigrationScriptParserImpl(
                getConfig().getEsMigrationPrefix(),
                getConfig().getEsMigrationSuffixes(),
                getConfig().getPlaceholders(),
                getConfig().getPlaceholderPrefix(),
                getConfig().getPlaceholderSuffix(),
                getConfig().isPlaceholderReplacement()
        );
    }

    protected MigrationScriptReader createMigrationScriptReader() {
        return new MigrationScriptReaderImpl(
                getConfig().getLocations(),
                getConfig().getEncoding(),
                getConfig().getEsMigrationPrefix(),
                getConfig().getEsMigrationSuffixes());
    }

    protected HistoryRepository createHistoryRepository() {
        return new HistoryRepositoryImpl(getRestHighLevelClient());
    }

    protected MigrationService createMigrationService() {
        return new MigrationServiceImpl(createHistoryRepository(), 1_000, 10_000);
    }
}
