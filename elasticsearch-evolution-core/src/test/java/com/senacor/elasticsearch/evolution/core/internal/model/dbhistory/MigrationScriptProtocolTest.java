package com.senacor.elasticsearch.evolution.core.internal.model.dbhistory;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andreas Keefer
 */
class MigrationScriptProtocolTest {

    @Nested
    class hashcode {

        @Test
        void onlyVersionIsUsed() {
            String version = "1.1";
            MigrationScriptProtocol migrationScriptProtocol = new MigrationScriptProtocol()
                    .setVersion(version);
            MigrationScriptProtocol other = new MigrationScriptProtocol()
                    .setVersion(version)
                    .setChecksum(1)
                    .setDescription("a")
                    .setExecutionRuntimeInMillis(1)
                    .setIndexName("asd")
                    .setScriptName("asd")
                    .setExecutionTimestamp(ZonedDateTime.now())
                    .setSuccess(true)
                    .setLocked(false);

            assertThat(migrationScriptProtocol.hashCode()).isEqualTo(other.hashCode());
            assertThat(migrationScriptProtocol.hashCode()).isNotEqualTo(other.setVersion("1.2").hashCode());
        }
    }


    @Nested
    class equals {
        @Test
        void onlyVersionIsUsed() {
            String version = "1.1";
            MigrationScriptProtocol migrationScriptProtocol = new MigrationScriptProtocol()
                    .setVersion(version);
            MigrationScriptProtocol other = new MigrationScriptProtocol()
                    .setVersion(version)
                    .setChecksum(1)
                    .setDescription("a")
                    .setExecutionRuntimeInMillis(1)
                    .setIndexName("asd")
                    .setScriptName("asd")
                    .setExecutionTimestamp(ZonedDateTime.now())
                    .setSuccess(true)
                    .setLocked(false);

            assertThat(migrationScriptProtocol.equals(other)).isTrue();
            assertThat(migrationScriptProtocol.equals(other.setVersion("1.2"))).isFalse();
        }
    }

    @Nested
    class compareTo {

        @Test
        void equals() {
            String version = "1.1";
            assertThat(
                    new MigrationScriptProtocol()
                            .setVersion(version)
                            .compareTo(new MigrationScriptProtocol()
                                    .setVersion(version)))
                    .isEqualTo(0);
        }

        @Test
        void bigger() {
            assertThat(
                    new MigrationScriptProtocol()
                            .setVersion("1.1")
                            .compareTo(new MigrationScriptProtocol()
                                    .setVersion("1.0")))
                    .isGreaterThan(0);
        }

        @Test
        void biggerThanNull() {
            assertThat(
                    new MigrationScriptProtocol()
                            .setVersion("1.1")
                            .compareTo(null))
                    .isGreaterThan(0);
        }

        @Test
        void smaller() {
            assertThat(
                    new MigrationScriptProtocol()
                            .setVersion("1.1")
                            .compareTo(new MigrationScriptProtocol()
                                    .setVersion("1.2")))
                    .isLessThan(0);
        }
    }
}