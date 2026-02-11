package com.senacor.elasticsearch.evolution.core;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.ValidateException;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfig;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationService;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationVersion;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.FileNameInfoImpl;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigration;
import com.senacor.elasticsearch.evolution.rest.abstraction.EvolutionRestClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Andreas Keefer
 */
@ExtendWith(MockitoExtension.class)
class ElasticsearchEvolutionTest {

    @Mock
    private EvolutionRestClient<?> evolutionRestClient;

    @Nested
    class MigrateShould {

        @Test
        void throw_MigrationException_when_historyMaxQuerySizeTooLow() {
            String indexName = "es_evolution";
            int historyMaxQuerySize = 6;
            ElasticsearchEvolution underTest = ElasticsearchEvolution.configure()
                    .setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/migrate_OK"))
                    .setHistoryIndex(indexName)
                    .setHistoryMaxQuerySize(historyMaxQuerySize)
                    .load(evolutionRestClient);

            assertThatThrownBy(underTest::migrate)
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("configured historyMaxQuerySize of '%s' is too low for the number of migration scripts of '%s'", historyMaxQuerySize, 10);

            InOrder order = inOrder(evolutionRestClient);
            order.verify(evolutionRestClient).info();
            order.verifyNoMoreInteractions();
        }

        @Test
        void do_nothing_on_empty_location() {
            ElasticsearchEvolution underTest = ElasticsearchEvolution.configure()
                    .setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/empty_location"))
                    .load(evolutionRestClient);

            assertThatCode(underTest::migrate)
                    .doesNotThrowAnyException();
        }

        @Test
        void do_nothing_on_non_existing_location() {
            ElasticsearchEvolution underTest = ElasticsearchEvolution.configure()
                    .setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/does_not_exist"))
                    .load(evolutionRestClient);

            assertThatCode(underTest::migrate)
                    .doesNotThrowAnyException();
        }

        @Test
        void return_zero_when_elasticsearchEvolution_IsNotEnabled() {
            int migrations = ElasticsearchEvolution.configure()
                    .setEnabled(false)
                    .load(evolutionRestClient)
                    .migrate();

            assertThat(migrations).isZero();

            InOrder order = inOrder(evolutionRestClient);
            order.verifyNoMoreInteractions();
        }
    }

    @Nested
    class ValidateShould {

        @Test
        void throw_ValidateException_when_historyMaxQuerySizeTooLow() {
            String indexName = "es_evolution";
            int historyMaxQuerySize = 6;
            ElasticsearchEvolution underTest = ElasticsearchEvolution.configure()
                    .setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/migrate_OK"))
                    .setHistoryIndex(indexName)
                    .setHistoryMaxQuerySize(historyMaxQuerySize)
                    .load(evolutionRestClient);

            assertThatThrownBy(underTest::validate)
                    .isInstanceOf(ValidateException.class)
                    .hasMessage("configured historyMaxQuerySize of '%s' is too low for the number of migration scripts of '%s'", historyMaxQuerySize, 10);

            InOrder order = inOrder(evolutionRestClient);
            order.verify(evolutionRestClient).info();
            order.verifyNoMoreInteractions();
        }

        @Test
        void beValid_on_empty_location() {
            ElasticsearchEvolution underTest = ElasticsearchEvolution.configure()
                    .setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/empty_location"))
                    .load(evolutionRestClient);

            assertThatCode(underTest::validate)
                    .doesNotThrowAnyException();
        }

        @Test
        void beValid_on_non_existing_location() {
            ElasticsearchEvolution underTest = ElasticsearchEvolution.configure()
                    .setLocations(singletonList("classpath:es/ElasticsearchEvolutionTest/does_not_exist"))
                    .load(evolutionRestClient);

            assertThatCode(underTest::validate)
                    .doesNotThrowAnyException();
        }

        @Test
        void isValid_when_elasticsearchEvolution_IsNotEnabled() {
            final ElasticsearchEvolution underTest = ElasticsearchEvolution.configure()
                    .setEnabled(false)
                    .load(evolutionRestClient);

            assertThatCode(underTest::validate)
                    .doesNotThrowAnyException();

            InOrder order = inOrder(evolutionRestClient);
            order.verifyNoMoreInteractions();
        }

        @Test
        void fail_when_pending_migrations_exist(@Mock MigrationService migrationService) {
            final ElasticsearchEvolutionConfig config = ElasticsearchEvolution.configure();
            final ElasticsearchEvolution underTest = new ElasticsearchEvolution(config, evolutionRestClient) {
                @Override
                protected MigrationService createMigrationService() {
                    return migrationService;
                }
            };
            final ParsedMigration<?> pendingMigration = new ParsedMigration<>();
            pendingMigration.setFileNameInfo(new FileNameInfoImpl(MigrationVersion.fromVersion("1.0"), "des", "scriptName"));
            when(migrationService.getPendingScriptsToBeExecuted(anyCollection()))
                    .thenReturn(List.of(pendingMigration));

            assertThatThrownBy(underTest::validate)
                    .isInstanceOf(ValidateException.class)
                    .hasMessage("There are pending migrations to be executed: [FileNameInfoImpl(version=1, description=des, scriptName=scriptName)]");
        }

        @Test
        void fail_when_MigrationService_throws_MigrationException(@Mock MigrationService migrationService) {
            final ElasticsearchEvolutionConfig config = ElasticsearchEvolution.configure();
            final ElasticsearchEvolution underTest = new ElasticsearchEvolution(config, evolutionRestClient) {
                @Override
                protected MigrationService createMigrationService() {
                    return migrationService;
                }
            };
            final ParsedMigration<?> pendingMigration = new ParsedMigration<>();
            pendingMigration.setFileNameInfo(new FileNameInfoImpl(MigrationVersion.fromVersion("1.0"), "des", "scriptName"));
            when(migrationService.getPendingScriptsToBeExecuted(anyCollection()))
                    .thenThrow(new MigrationException("MigrationService failure"));

            assertThatThrownBy(underTest::validate)
                    .isInstanceOf(ValidateException.class)
                    .hasMessage("Validation failed: MigrationService failure")
                    .hasCauseInstanceOf(MigrationException.class)
                    .hasRootCauseMessage("MigrationService failure");
        }
    }
}