package com.senacor.elasticsearch.evolution.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.ValidateException;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationScriptParser;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationScriptReader;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationService;
import com.senacor.elasticsearch.evolution.core.internal.migration.execution.HistoryRepositoryImpl;
import com.senacor.elasticsearch.evolution.core.internal.migration.execution.MigrationScriptProtocolMapper;
import com.senacor.elasticsearch.evolution.core.internal.migration.execution.MigrationServiceImpl;
import com.senacor.elasticsearch.evolution.core.internal.migration.input.MigrationScriptParserImpl;
import com.senacor.elasticsearch.evolution.core.internal.migration.input.MigrationScriptReaderImpl;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigrationScript;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;
import com.senacor.elasticsearch.evolution.rest.abstracion.EvolutionRestClient;
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
 *   .load(RestClient.builder(HttpHost.create(esUrl)).build());
 * esEvolution.migrate();
 * </pre>
 * <p>
 *
 * @author Andreas Keefer
 */
public class ElasticsearchEvolution {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchEvolution.class);

    private final ElasticsearchEvolutionConfig config;
    private final EvolutionRestClient restClient;

    private final MigrationScriptReader migrationScriptReader;
    private final MigrationScriptParser migrationScriptParser;
    private final MigrationService migrationService;

    /**
     * This is your starting point. This creates a configuration which can be customized to your needs before being
     * loaded into a new ElasticsearchEvolution instance using the load() method.
     * <p>In its simplest form, this is how you configure Flyway with all defaults to get started:</p>
     * <pre>
     * ElasticsearchEvolution esEvolution = ElasticsearchEvolution.configure()
     *     .load(RestClient.builder(HttpHost.create(esUrl)).build());
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
     * @param restClient                   REST client to interact with Elasticsearch
     */
    public ElasticsearchEvolution(ElasticsearchEvolutionConfig elasticsearchEvolutionConfig,
                                  EvolutionRestClient restClient) {
        this.config = requireNonNull(elasticsearchEvolutionConfig, "elasticsearchEvolutionConfig must not be null")
                .validate();
        this.restClient = requireNonNull(restClient, "restClient must not be null");

        this.migrationScriptReader = createMigrationScriptReader();
        this.migrationScriptParser = createMigrationScriptParser();
        this.migrationService = createMigrationService();

        if (logger.isInfoEnabled()) {
            logger.info("Created ElasticsearchEvolution with config='{}' and client='{}'",
                    this.getConfig(), this.getRestClient().info());
        }
    }

    /**
     * <p>Starts the migration. All pending migrations will be applied in order.
     * Calling migrate on an up-to-date database has no effect.</p>
     *
     * @return The number of successfully applied migrations.
     * @throws MigrationException when the migration failed.
     */
    public int migrate() throws MigrationException {
        if (getConfig().isEnabled()) {
            logger.info("start migration...");
            logger.info("reading migration scripts...");
            Collection<RawMigrationScript> rawMigrationScripts = migrationScriptReader.read();
            if (rawMigrationScripts.size() > getConfig().getHistoryMaxQuerySize()) {
                throw new MigrationException("configured historyMaxQuerySize of '%s' is too low for the number of migration scripts of '%s'".formatted(
                        getConfig().getHistoryMaxQuerySize(), rawMigrationScripts.size()));
            }

            logger.info("parsing migration scripts...");
            Collection<ParsedMigrationScript> parsedMigrationScripts = migrationScriptParser.parse(rawMigrationScripts);
            logger.info("execute migration scripts...");
            List<MigrationScriptProtocol> executedScripts = migrationService.executePendingScripts(parsedMigrationScripts);
            return (int) executedScripts.stream()
                    .filter(MigrationScriptProtocol::isSuccess)
                    .count();
        } else {
            logger.debug("elasticsearch-evolution is not enabled");
            return 0;
        }
    }

    /**
     * Validate applied migrations against resolved ones (on the filesystem or classpath)
     * to detect accidental changes that may prevent the schema(s) from being recreated exactly.
     * Validation fails if:
     * <ul>
     * <li>a previously applied migration has been modified after it was applied (if enabled in configuration {@link ElasticsearchEvolutionConfig#isValidateOnMigrate()}</li>
     * <li>versions have been resolved that haven't been applied yet</li>
     * </ul>
     *
     * @throws ValidateException when the validation failed.
     */
    public void validate() throws ValidateException {
        if (getConfig().isEnabled()) {
            logger.info("start validate...");
            logger.info("reading migration scripts...");
            Collection<RawMigrationScript> rawMigrationScripts = migrationScriptReader.read();
            if (rawMigrationScripts.size() > getConfig().getHistoryMaxQuerySize()) {
                throw new ValidateException("configured historyMaxQuerySize of '%s' is too low for the number of migration scripts of '%s'".formatted(
                        getConfig().getHistoryMaxQuerySize(), rawMigrationScripts.size()));
            }

            logger.info("parsing migration scripts...");
            Collection<ParsedMigrationScript> parsedMigrationScripts = migrationScriptParser.parse(rawMigrationScripts);
            logger.info("validating migration scripts...");
            try {
                List<ParsedMigrationScript> pendingScriptsToBeExecuted = migrationService.getPendingScriptsToBeExecuted(parsedMigrationScripts);

                if (!pendingScriptsToBeExecuted.isEmpty()) {
                    throw new ValidateException(pendingScriptsToBeExecuted.stream()
                            .map(ParsedMigrationScript::getFileNameInfo)
                            .toList());
                }
            } catch (MigrationException migrationException) {
                throw new ValidateException("Validation failed: " + migrationException.getMessage(), migrationException);
            }
            logger.info("validate succeeded");
        } else {
            logger.debug("elasticsearch-evolution is not enabled");
        }
    }

    protected ElasticsearchEvolutionConfig getConfig() {
        return config;
    }

    protected EvolutionRestClient getRestClient() {
        return restClient;
    }

    protected ObjectMapper createObjectMapper(){
        return new ObjectMapper()
                // not all search response properties are mapped, so they must be ignored
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected MigrationScriptParser createMigrationScriptParser() {
        return new MigrationScriptParserImpl(
                getConfig().getEsMigrationPrefix(),
                getConfig().getEsMigrationSuffixes(),
                getConfig().getPlaceholders(),
                getConfig().getPlaceholderPrefix(),
                getConfig().getPlaceholderSuffix(),
                getConfig().isPlaceholderReplacement(),
                getConfig().getLineSeparator()
        );
    }

    protected MigrationScriptReader createMigrationScriptReader() {
        return new MigrationScriptReaderImpl(
                getConfig().getLocations(),
                getConfig().getEncoding(),
                getConfig().getEsMigrationPrefix(),
                getConfig().getEsMigrationSuffixes(),
                getConfig().getLineSeparator(),
                getConfig().isTrimTrailingNewlineInMigrations());
    }

    protected HistoryRepository createHistoryRepository() {
        return new HistoryRepositoryImpl(
                getRestClient(),
                getConfig().getHistoryIndex(),
                new MigrationScriptProtocolMapper(),
                getConfig().getHistoryMaxQuerySize(),
                createObjectMapper());
    }

    protected MigrationService createMigrationService() {
        return new MigrationServiceImpl(
                createHistoryRepository(),
                1_000,
                10_000,
                getRestClient(),
                getConfig().getDefaultContentType(),
                getConfig().getEncoding(),
                getConfig().isValidateOnMigrate(),
                getConfig().getBaselineVersion(),
                getConfig().isOutOfOrder());
    }
}
