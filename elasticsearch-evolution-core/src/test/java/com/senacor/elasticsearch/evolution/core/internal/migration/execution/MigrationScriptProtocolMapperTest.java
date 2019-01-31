package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.internal.model.MigrationVersion;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;

import static com.senacor.elasticsearch.evolution.core.internal.migration.execution.MigrationScriptProtocolMapper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author Andreas Keefer
 */
class MigrationScriptProtocolMapperTest {

    private MigrationScriptProtocolMapper underTest = new MigrationScriptProtocolMapper();

    @Nested
    class mapToMap {
        @Test
        void emptyProtocol() {
            MigrationScriptProtocol protocol = new MigrationScriptProtocol();

            HashMap<String, Object> res = underTest.mapToMap(protocol);

            assertThat(res).hasSize(9)
                    .containsEntry(CHECKSUM_FIELD_NAME, 0)
                    .containsEntry(DESCRIPTION_FIELD_NAME, null)
                    .containsEntry(EXECUTION_RUNTIME_IN_MILLIS_FIELD_NAME, 0)
                    .containsEntry(EXECUTION_TIMESTAMP_FIELD_NAME, null)
                    .containsEntry(INDEX_NAME_FIELD_NAME, null)
                    .containsEntry(SCRIPT_NAME_FIELD_NAME, null)
                    .containsEntry(LOCKED_FIELD_NAME, true)
                    .containsEntry(SUCCESS_FIELD_NAME, false)
                    .containsEntry(VERSION_FIELD_NAME, null);
        }

        @Test
        void fullProtocol() {
            MigrationScriptProtocol protocol = new MigrationScriptProtocol()
                    .setVersion("1")
                    .setChecksum(1)
                    .setDescription("des")
                    .setLocked(false)
                    .setSuccess(true)
                    .setExecutionRuntimeInMillis(2)
                    .setExecutionTimestamp(OffsetDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                    .setIndexName("index")
                    .setScriptName("foo.http");

            HashMap<String, Object> res = underTest.mapToMap(protocol);

            assertThat(res).hasSize(9)
                    .containsEntry(CHECKSUM_FIELD_NAME, 1)
                    .containsEntry(DESCRIPTION_FIELD_NAME, "des")
                    .containsEntry(EXECUTION_RUNTIME_IN_MILLIS_FIELD_NAME, 2)
                    .containsEntry(EXECUTION_TIMESTAMP_FIELD_NAME, "2019-01-01T00:00:00Z")
                    .containsEntry(INDEX_NAME_FIELD_NAME, "index")
                    .containsEntry(SCRIPT_NAME_FIELD_NAME, "foo.http")
                    .containsEntry(LOCKED_FIELD_NAME, false)
                    .containsEntry(SUCCESS_FIELD_NAME, true)
                    .containsEntry(VERSION_FIELD_NAME, "1");
        }
    }

    @Nested
    class mapFromMap {
        @Test
        void emptyProtocol() {
            HashMap<String, Object> mapData = new HashMap<>();

            MigrationScriptProtocol protocol = underTest.mapFromMap(mapData);

            assertSoftly(softly -> {
                softly.assertThat(protocol.getChecksum()).isEqualTo(0);
                softly.assertThat(protocol.getDescription()).isNull();
                softly.assertThat(protocol.getExecutionRuntimeInMillis()).isEqualTo(0);
                softly.assertThat(protocol.getExecutionTimestamp()).isNull();
                softly.assertThat(protocol.getIndexName()).isNull();
                softly.assertThat(protocol.getScriptName()).isNull();
                softly.assertThat(protocol.isLocked()).isEqualTo(true);
                softly.assertThat(protocol.isSuccess()).isEqualTo(false);
                softly.assertThat(protocol.getVersion()).isNull();
            });
        }

        @Test
        void fullProtocol() {
            HashMap<String, Object> mapData = new HashMap<>();
            mapData.put(CHECKSUM_FIELD_NAME, 1);
            mapData.put(DESCRIPTION_FIELD_NAME, "des");
            mapData.put(EXECUTION_RUNTIME_IN_MILLIS_FIELD_NAME, 2);
            mapData.put(EXECUTION_TIMESTAMP_FIELD_NAME, "2019-01-01T00:00:00Z");
            mapData.put(INDEX_NAME_FIELD_NAME, "index");
            mapData.put(SCRIPT_NAME_FIELD_NAME, "foo.http");
            mapData.put(LOCKED_FIELD_NAME, false);
            mapData.put(SUCCESS_FIELD_NAME, true);
            mapData.put(VERSION_FIELD_NAME, "1");

            MigrationScriptProtocol protocol = underTest.mapFromMap(mapData);

            assertSoftly(softly -> {
                softly.assertThat(protocol.getChecksum()).isEqualTo(1);
                softly.assertThat(protocol.getDescription()).isEqualTo("des");
                softly.assertThat(protocol.getExecutionRuntimeInMillis()).isEqualTo(2);
                softly.assertThat(protocol.getExecutionTimestamp()).isEqualTo(OffsetDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
                softly.assertThat(protocol.getIndexName()).isEqualTo("index");
                softly.assertThat(protocol.getScriptName()).isEqualTo("foo.http");
                softly.assertThat(protocol.isLocked()).isEqualTo(false);
                softly.assertThat(protocol.isSuccess()).isEqualTo(true);
                softly.assertThat(protocol.getVersion()).isEqualTo(MigrationVersion.fromVersion("1"));
            });
        }
    }
}