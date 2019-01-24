package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.FileNameInfoImpl;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.MigrationScriptRequest;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigrationScript;
import com.senacor.elasticsearch.evolution.core.test.MockitoExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import static com.senacor.elasticsearch.evolution.core.internal.model.MigrationVersion.fromVersion;
import static com.senacor.elasticsearch.evolution.core.internal.model.migration.MigrationScriptRequest.HttpMethod.DELETE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.*;

/**
 * @author Andreas Keefer
 */
@ExtendWith(MockitoExtension.class)
class MigrationServiceImplTest {

    @Mock
    private HistoryRepository historyRepository;

    @Nested
    class waitUntilUnlocked {

        @Test
        void noLockExists() {
            doReturn(false).when(historyRepository).isLocked();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository, 2000, 2000);

            assertTimeout(Duration.ofSeconds(1), underTest::waitUntilUnlocked);

            InOrder order = inOrder(historyRepository);
            order.verify(historyRepository).isLocked();
            order.verifyNoMoreInteractions();
        }

        @Test
        void LockExistsAndGetsReleased() {
            doReturn(true, false).when(historyRepository).isLocked();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository, 100, 100);

            assertTimeout(Duration.ofMillis(200), underTest::waitUntilUnlocked);

            InOrder order = inOrder(historyRepository);
            order.verify(historyRepository, times(2)).isLocked();
            order.verifyNoMoreInteractions();
        }
    }

    @Nested
    class getPendingScriptsToBeExecuted {
        @Test
        void emptyHistory_allScriptsHaveToBeReturned() {
            doReturn(new TreeSet<>()).when(historyRepository).findAll();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository, 0, 0);
            ParsedMigrationScript parsedMigrationScript1_1 = createParsedMigrationScript("1.1");
            ParsedMigrationScript parsedMigrationScript1_0 = createParsedMigrationScript("1.0");
            List<ParsedMigrationScript> parsedMigrationScripts = Arrays.asList(
                    parsedMigrationScript1_1,
                    parsedMigrationScript1_0);

            List<ParsedMigrationScript> res = underTest.getPendingScriptsToBeExecuted(parsedMigrationScripts);

            assertThat(res).hasSize(2);
            assertThat(res.get(0).getFileNameInfo().getVersion()).isEqualTo(fromVersion("1.0"));
            assertThat(res.get(1).getFileNameInfo().getVersion()).isEqualTo(fromVersion("1.1"));
            InOrder order = inOrder(historyRepository);
            order.verify(historyRepository).findAll();
            order.verifyNoMoreInteractions();
        }

        @Test
        void scriptsAndHistoryInSync_noScriptsWillBeReturned() {
            doReturn(new TreeSet<>(Arrays.asList(
                    createMigrationScriptProtocol("1.0", true),
                    createMigrationScriptProtocol("1.1", true)
            ))).when(historyRepository).findAll();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository, 0, 0);

            List<ParsedMigrationScript> parsedMigrationScripts = Arrays.asList(
                    createParsedMigrationScript("1.1"),
                    createParsedMigrationScript("1.0"));

            List<ParsedMigrationScript> res = underTest.getPendingScriptsToBeExecuted(parsedMigrationScripts);

            assertThat(res).hasSize(0);
            InOrder order = inOrder(historyRepository);
            order.verify(historyRepository).findAll();
            order.verifyNoMoreInteractions();
        }

        @Test
        void lastHistoryVersionWasFailing_AllScriptsInclFailedWillBeReturned() {
            doReturn(new TreeSet<>(Arrays.asList(
                    createMigrationScriptProtocol("1.0", true),
                    createMigrationScriptProtocol("1.1", false)
            ))).when(historyRepository).findAll();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository, 0, 0);

            ParsedMigrationScript parsedMigrationScript1_0 = createParsedMigrationScript("1.0");
            ParsedMigrationScript parsedMigrationScript1_1 = createParsedMigrationScript("1.1");
            List<ParsedMigrationScript> parsedMigrationScripts = Arrays.asList(
                    parsedMigrationScript1_1,
                    parsedMigrationScript1_0);

            List<ParsedMigrationScript> res = underTest.getPendingScriptsToBeExecuted(parsedMigrationScripts);

            assertThat(res).hasSize(1);
            assertThat(res.get(0)).isSameAs(parsedMigrationScript1_1);
            InOrder order = inOrder(historyRepository);
            order.verify(historyRepository).findAll();
            order.verifyNoMoreInteractions();
        }

        @Test
        void moreHistoryVersionsThanScripts_warningIsShownAnNoScriptsWillBeReturned() {
            doReturn(new TreeSet<>(Arrays.asList(
                    createMigrationScriptProtocol("1.0", true),
                    createMigrationScriptProtocol("1.1", true),
                    createMigrationScriptProtocol("1.2", false)
            ))).when(historyRepository).findAll();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository, 0, 0);

            ParsedMigrationScript parsedMigrationScript1_0 = createParsedMigrationScript("1.0");
            ParsedMigrationScript parsedMigrationScript1_1 = createParsedMigrationScript("1.1");
            List<ParsedMigrationScript> parsedMigrationScripts = Arrays.asList(
                    parsedMigrationScript1_1,
                    parsedMigrationScript1_0);

            List<ParsedMigrationScript> res = underTest.getPendingScriptsToBeExecuted(parsedMigrationScripts);

            assertThat(res).hasSize(0);
            InOrder order = inOrder(historyRepository);
            order.verify(historyRepository).findAll();
            order.verifyNoMoreInteractions();
        }

        @Test
        void outOfOrderExecutionIsNotSupported() {
            doReturn(new TreeSet<>(Arrays.asList(
                    createMigrationScriptProtocol("1.0", true),
                    createMigrationScriptProtocol("1.1", true)
            ))).when(historyRepository).findAll();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository, 0, 0);

            ParsedMigrationScript parsedMigrationScript1_0 = createParsedMigrationScript("1.0");
            ParsedMigrationScript parsedMigrationScript1_0_1 = createParsedMigrationScript("1.0.1");
            ParsedMigrationScript parsedMigrationScript1_1 = createParsedMigrationScript("1.1");
            List<ParsedMigrationScript> parsedMigrationScripts = Arrays.asList(
                    parsedMigrationScript1_1,
                    parsedMigrationScript1_0_1,
                    parsedMigrationScript1_0);

            assertThatThrownBy(() -> underTest.getPendingScriptsToBeExecuted(parsedMigrationScripts))
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("The logged execution in the Elasticsearch-Evolution history index at position 1 is version 1.1 and in the same position in the given migration scripts is version 1.0.1! Out of order execution is not supported. Or maybe you have added new migration scripts in between or have to cleanup the Elasticsearch-Evolution history index manually");
        }
    }

    private MigrationScriptProtocol createMigrationScriptProtocol(String version, boolean success) {
        return new MigrationScriptProtocol()
                .setVersion(version)
                .setChecksum(1)
                .setSuccess(success)
                .setLocked(true)
                .setDescription(version)
                .setScriptName(createDefaultScriptName(version));
    }

    private ParsedMigrationScript createParsedMigrationScript(String version) {
        return new ParsedMigrationScript()
                .setFileNameInfo(
                        new FileNameInfoImpl(fromVersion(version), version, createDefaultScriptName(version)))
                .setChecksum(1)
                .setMigrationScriptRequest(new MigrationScriptRequest()
                        .setHttpMethod(DELETE)
                        .setPath("/"));
    }

    private String createDefaultScriptName(String version) {
        return "V" + version + "__" + version + ".http";
    }
}