package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.test.MockitoExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

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

            InOrder order = Mockito.inOrder(historyRepository);
            order.verify(historyRepository).isLocked();
            order.verifyNoMoreInteractions();
        }

        @Test
        void LockExistsAndGetsReleased() {
            doReturn(true, false).when(historyRepository).isLocked();
            MigrationServiceImpl underTest = new MigrationServiceImpl(historyRepository, 100, 100);

            assertTimeout(Duration.ofMillis(200), underTest::waitUntilUnlocked);

            InOrder order = Mockito.inOrder(historyRepository);
            order.verify(historyRepository, times(2)).isLocked();
            order.verifyNoMoreInteractions();
        }
    }
}