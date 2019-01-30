package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;

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

            Assertions.assertThat(res).hasSize(9)
                    .containsEntry(MigrationScriptProtocolMapper.CHECKSUM_FIELD_NAME, 0)
                    .containsEntry(MigrationScriptProtocolMapper.DESCRIPTION_FIELD_NAME, null)
                    .containsEntry(MigrationScriptProtocolMapper.EXECUTION_RUNTIME_IN_MILLIS_FIELD_NAME, 0)
                    .containsEntry(MigrationScriptProtocolMapper.EXECUTION_TIMESTAMP_FIELD_NAME, null)
                    .containsEntry(MigrationScriptProtocolMapper.INDEX_NAME_FIELD_NAME, null)
                    .containsEntry(MigrationScriptProtocolMapper.SCRIPT_NAME_FIELD_NAME, null)
                    .containsEntry(MigrationScriptProtocolMapper.LOCKED_FIELD_NAME, true)
                    .containsEntry(MigrationScriptProtocolMapper.SUCCESS_FIELD_NAME, false)
                    .containsEntry(MigrationScriptProtocolMapper.VERSION_FIELD_NAME, null);
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

            Assertions.assertThat(res).hasSize(9)
                    .containsEntry(MigrationScriptProtocolMapper.CHECKSUM_FIELD_NAME, 1)
                    .containsEntry(MigrationScriptProtocolMapper.DESCRIPTION_FIELD_NAME, "des")
                    .containsEntry(MigrationScriptProtocolMapper.EXECUTION_RUNTIME_IN_MILLIS_FIELD_NAME, 2)
                    .containsEntry(MigrationScriptProtocolMapper.EXECUTION_TIMESTAMP_FIELD_NAME, "2019-01-01T00:00:00Z")
                    .containsEntry(MigrationScriptProtocolMapper.INDEX_NAME_FIELD_NAME, "index")
                    .containsEntry(MigrationScriptProtocolMapper.SCRIPT_NAME_FIELD_NAME, "foo.http")
                    .containsEntry(MigrationScriptProtocolMapper.LOCKED_FIELD_NAME, false)
                    .containsEntry(MigrationScriptProtocolMapper.SUCCESS_FIELD_NAME, true)
                    .containsEntry(MigrationScriptProtocolMapper.VERSION_FIELD_NAME, "1");
        }
    }
}