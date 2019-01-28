package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Andreas Keefer
 */
class MigrationScriptReaderTest {
    @Nested
    class readResources {

        @Test
        void fromClassPathResourcesDircetory() {
            MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(Arrays.asList("classpath:scriptreader"),
                    StandardCharsets.UTF_8,
                    "c",
                    Arrays.asList(".http"));
            List<RawMigrationScript> actual = reader.read();
            assertThat(actual).containsExactlyInAnyOrder(new RawMigrationScript().setFileName("content.http").setContent("content!"),
                    new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"));
        }

        @Test
        void fromClassPathResourcesDirectoryAndMultipleSuffixes() {
            MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(Arrays.asList("classpath:scriptreader"),
                    StandardCharsets.UTF_8,
                    "c",
                    Arrays.asList(".http", ".other"));
            List<RawMigrationScript> actual = reader.read();
            assertThat(actual).containsExactlyInAnyOrder(new RawMigrationScript().setFileName("content.http").setContent("content!"),
                    new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"),
                    new RawMigrationScript().setFileName("content.other").setContent("content!"));
        }

        @Test
        void fromClasspathResourcesDirectoryWithClassPathPrepended() {
            MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(Arrays.asList("classpath:scriptreader"),
                    StandardCharsets.UTF_8,
                    "c",
                    Arrays.asList(".http", ".other"));
            List<RawMigrationScript> actual = reader.read();
            assertThat(actual).containsExactlyInAnyOrder(new RawMigrationScript().setFileName("content.http").setContent("content!"),
                    new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"),
                    new RawMigrationScript().setFileName("content.other").setContent("content!"));
        }

        @Test
        void fromClasspathResourcesDirectoryWithSameLocation() {
            MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(Arrays.asList("classpath:scriptreader", "classpath:scriptreader"),
                    StandardCharsets.UTF_8,
                    "c",
                    Arrays.asList(".http", ".other"));
            List<RawMigrationScript> actual = reader.read();
            assertThat(actual).containsExactlyInAnyOrder(new RawMigrationScript().setFileName("content.http").setContent("content!"),
                    new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"),
                    new RawMigrationScript().setFileName("content.other").setContent("content!"));
            assertThat(actual).hasSize(3);
        }

        @Test()
        void fromClasspathResourcesDirectoryWithWrongProtocol() {
            // http is not a valid prefix
            assertThrows(MigrationException.class, () -> {
                new MigrationScriptReaderImpl(Arrays.asList("classpath:scriptreader", "http:scriptreader"),
                        StandardCharsets.UTF_8,
                        "c",
                        Arrays.asList(".http", ".other")).read();
            });

        }

        @Test
        void fromFileSystemPath() {
            MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(Arrays.asList("file:C:/Users/jjuppe/Documents/VSMS/test_data/scriptreader"),
                    StandardCharsets.UTF_8,
                    "c",
                    Arrays.asList(".http"));
            List<RawMigrationScript> actual = reader.read();
            assertThat(actual).containsExactlyInAnyOrder(new RawMigrationScript().setFileName("content.http").setContent("content!"),
                    new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"));
        }

        @Test
        void fromInvalidFileSystemPath() {
            MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(Arrays.asList("file:X:/snc/scripts"),
                    StandardCharsets.UTF_8,
                    "c",
                    Arrays.asList(".http"));
            List<RawMigrationScript> actual = reader.read();
            assertThat(actual).isEmpty();
        }

        @Test
        void fromInvalidAndValidFileSystemPath() {
            MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(Arrays.asList("file:X:/snc/scripts",
                    "classpath:scriptreader"),
                    StandardCharsets.UTF_8,
                    "c",
                    Arrays.asList(".http"));
            List<RawMigrationScript> actual = reader.read();
            assertThat(actual).containsExactlyInAnyOrder(new RawMigrationScript().setFileName("content.http").setContent("content!"),
                    new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"));
        }
    }
}