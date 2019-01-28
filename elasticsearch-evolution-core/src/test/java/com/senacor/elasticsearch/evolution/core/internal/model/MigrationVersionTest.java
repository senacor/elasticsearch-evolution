package com.senacor.elasticsearch.evolution.core.internal.model;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Andreas Keefer
 */
class MigrationVersionTest {

    @Nested
    class isAtLeast {
        @Test
        void smaller() {
            assertThat(MigrationVersion.fromVersion("1.2").isAtLeast("1.3"))
                    .isFalse();
        }

        @Test
        void equals() {
            assertThat(MigrationVersion.fromVersion("1.2").isAtLeast("1.2"))
                    .isTrue();
        }

        @Test
        void bigger() {
            assertThat(MigrationVersion.fromVersion("1.3").isAtLeast("1.2"))
                    .isTrue();
        }
    }

    @Nested
    class isNewerThan {
        @Test
        void smaller() {
            assertThat(MigrationVersion.fromVersion("1.2").isNewerThan("1.3"))
                    .isFalse();
        }

        @Test
        void equals() {
            assertThat(MigrationVersion.fromVersion("1.2").isNewerThan("1.2"))
                    .isFalse();
        }

        @Test
        void bigger() {
            assertThat(MigrationVersion.fromVersion("1.3").isNewerThan("1.2"))
                    .isTrue();
        }
    }

    @Nested
    class isMajorNewerThan {
        @Test
        void onlyMinorSmaller() {
            assertThat(MigrationVersion.fromVersion("1.2").isMajorNewerThan("1.3"))
                    .isFalse();
        }

        @Test
        void equals() {
            assertThat(MigrationVersion.fromVersion("1.2").isMajorNewerThan("1.2"))
                    .isFalse();
        }

        @Test
        void onlyMinorBigger() {
            assertThat(MigrationVersion.fromVersion("1.3").isMajorNewerThan("1.2"))
                    .isFalse();
        }

        @Test
        void smaller() {
            assertThat(MigrationVersion.fromVersion("1.2").isMajorNewerThan("2.3"))
                    .isFalse();
        }

        @Test
        void bigger() {
            assertThat(MigrationVersion.fromVersion("2.3").isMajorNewerThan("1.2"))
                    .isTrue();
        }
    }

    @Nested
    class compareTo {
        @Test
        void everythingIsBiggerThanNull() {
            assertThat(MigrationVersion.fromVersion("2.3").compareTo(null))
                    .isGreaterThan(0);
        }
    }

    @Nested
    class getMajorAsString {
        @Test
        void returnsMajorString() {
            assertThat(MigrationVersion.fromVersion("2.3").getMajorAsString())
                    .isEqualTo("2");
        }
    }

    @Nested
    class getMinorAsString {
        @Test
        void returnsMinorString() {
            assertThat(MigrationVersion.fromVersion("2.3").getMinorAsString())
                    .isEqualTo("3");
        }

        @Test
        void returnsMinorStringEvenIfNoMinorExists() {
            assertThat(MigrationVersion.fromVersion("2").getMinorAsString())
                    .isEqualTo("0");
        }
    }

    @Nested
    class getMajor {
        @Test
        void returnsMajorInt() {
            assertThat(MigrationVersion.fromVersion("2.3").getMajor())
                    .isEqualTo(2);
        }
    }

    @Nested
    class toString {
        @Test
        void returnsStringRepresentation() {
            assertThat(MigrationVersion.fromVersion("2.3").toString())
                    .isEqualTo("2.3");
        }

        @Test
        void returnsSameValueAsGetVersion() {
            MigrationVersion version = MigrationVersion.fromVersion("2.3");
            assertThat(version.toString())
                    .isEqualTo(version.getVersion());
        }
    }

    @Nested
    class creat {
        @Test
        void canHandleUnderscoreAsSeparator() {
            assertThat(MigrationVersion.fromVersion("2_3").toString())
                    .isEqualTo("2.3");
        }

        @Test
        void removeUnnecessaryVersionParts_removeEndingZero() {
            MigrationVersion migrationVersion = MigrationVersion.fromVersion("2.0");

            assertThat(migrationVersion.hashCode())
                    .isEqualTo(Arrays.asList(2).hashCode());
            assertThat(migrationVersion.toString())
                    .isEqualTo("2");
        }

        @Test
        void removeUnnecessaryVersionParts_middleZeroIsNotRemoved() {
            MigrationVersion migrationVersion = MigrationVersion.fromVersion("2.0.1.0");

            assertThat(migrationVersion.hashCode())
                    .isEqualTo(Arrays.asList(2, 0, 1).hashCode());
            assertThat(migrationVersion.toString())
                    .isEqualTo("2.0.1");
        }

        @Test
        void ignoreLeadingZeros() {
            assertThat(MigrationVersion.fromVersion("02.01").equals(MigrationVersion.fromVersion("2.1")))
                    .isTrue();
        }

        @Test
        void blankVersion() {
            assertThatThrownBy(() -> MigrationVersion.fromVersion(" "))
                    .hasMessage("version must not be blank")
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void invalidVersion() {
            assertThatThrownBy(() -> MigrationVersion.fromVersion("1-2"))
                    .hasMessage("Invalid version containing non-numeric characters. Only 0..9 and . are allowed. Invalid version: 1-2")
                    .isInstanceOf(MigrationException.class);
        }
    }

    @Nested
    class hashCode {
        @Test
        void hashCodeIsCalculatedFromVersionParts() {
            MigrationVersion version = MigrationVersion.fromVersion("2.3");

            assertThat(version.hashCode())
                    .isEqualTo(Arrays.asList(2, 3).hashCode());
        }
    }

    @Nested
    class equals {
        @Test
        void notEqualsToNull() {
            MigrationVersion version = MigrationVersion.fromVersion("2.3");

            assertThat(version.equals(null))
                    .isFalse();
        }

        @Test
        void notEqualsToOtherVersion() {
            MigrationVersion version = MigrationVersion.fromVersion("2.3");

            assertThat(version.equals(MigrationVersion.fromVersion("2.4")))
                    .isFalse();
        }

        @Test
        void notEqualsToOtherClass() {
            MigrationVersion version = MigrationVersion.fromVersion("2.3");

            assertThat(version.equals("2.4"))
                    .isFalse();
        }

        @Test
        void equalsToSameInstance() {
            MigrationVersion version = MigrationVersion.fromVersion("2.3");

            assertThat(version.equals(version))
                    .isTrue();
        }

        @Test
        void equalsToOtherInstance() {
            MigrationVersion version = MigrationVersion.fromVersion("2.3");

            assertThat(version.equals(MigrationVersion.fromVersion("2.3")))
                    .isTrue();
        }
    }
}