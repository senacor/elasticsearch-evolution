package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Andreas Keefer
 */
class MigrationScriptReaderTest {
    @Nested
    class readResources {
        @Nested
        class fromClassPath {
            @Test
            void normal() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList("classpath:scriptreader"),
                        StandardCharsets.UTF_8,
                        "c",
                        singletonList(".http"));
                List<RawMigrationScript> actual = reader.read();
                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript().setFileName("content.http").setContent("content!"),
                                new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"));
            }

            @Test
            void pom() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList("classpath:META-INF/maven/org.assertj/assertj-core"),
                        StandardCharsets.UTF_8,
                        "pom",
                        singletonList(".properties"));

                List<RawMigrationScript> res = reader.read();

                assertThat(res).hasSize(1);
                assertThat(res.get(0).getFileName()).isEqualTo("pom.properties");
            }

            @Test
            void invalidClasspath() {
                String classpath = "classpath:script reader";
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList(classpath),
                        StandardCharsets.UTF_8,
                        "c",
                        singletonList(".http")) {
                    @Override
                    protected List<RawMigrationScript> readFromLocation(String location) throws URISyntaxException, IOException {
                        throw new URISyntaxException("input", "reason");
                    }
                };

                assertThatThrownBy(reader::read)
                        .isInstanceOf(MigrationException.class)
                        .hasMessage("couldn't read scripts from " + classpath);
            }

            @Test
            void multipleSuffixes() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList("classpath:scriptreader"),
                        StandardCharsets.UTF_8,
                        "c",
                        Arrays.asList(".http", ".other"));
                List<RawMigrationScript> actual = reader.read();
                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript().setFileName("content.http").setContent("content!"),
                                new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"),
                                new RawMigrationScript().setFileName("content.other").setContent("content!"));
            }

            @Test
            void handlingDuplicates() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        Arrays.asList("classpath:scriptreader", "classpath:scriptreader"),
                        StandardCharsets.UTF_8,
                        "c",
                        Arrays.asList(".http", ".other"));
                List<RawMigrationScript> actual = reader.read();
                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript().setFileName("content.http").setContent("content!"),
                                new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"),
                                new RawMigrationScript().setFileName("content.other").setContent("content!"));
            }

            @Test
            void withWrongProtocol() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        Arrays.asList("classpath:scriptreader", "http:scriptreader"),
                        StandardCharsets.UTF_8,
                        "c",
                        Arrays.asList(".http", ".other"));
                assertThatThrownBy(reader::read)
                        .isInstanceOf(MigrationException.class)
                        .hasMessage("could not read location path http:scriptreader, should look like this: " +
                                "classpath:es/migration or this: file:/home/scripts/migration");
            }
        }

        @Nested
        class fromFileSystem {
            @Test
            void normalPath() throws URISyntaxException {
                URL resourceDirectory = MigrationScriptReaderImpl.resolveURL("scriptreader");
                String absolutePathToScriptreader = Paths.get(resourceDirectory.toURI()).toFile().getAbsolutePath();
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList("file:" + absolutePathToScriptreader),
                        StandardCharsets.UTF_8,
                        "c",
                        singletonList(".http"));
                List<RawMigrationScript> actual = reader.read();
                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript().setFileName("content.http").setContent("content!"),
                                new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"));
            }


            @Test
            void invalidPath() {
                assertThatThrownBy(() ->
                        new MigrationScriptReaderImpl(
                                singletonList("file:X:/snc/scripts"),
                                StandardCharsets.UTF_8,
                                "c",
                                singletonList(".http")).read())
                        .isInstanceOf(MigrationException.class)
                        .hasMessage("The location file:X:/snc/scripts is not a valid location!");
            }


            @Test
            void validAndInvalidPath() throws URISyntaxException {
                URL resourceDirectory = MigrationScriptReaderImpl.resolveURL("scriptreader");
                String absolutePathToScriptreader = Paths.get(resourceDirectory.toURI()).toFile().getAbsolutePath();
                assertThatThrownBy(() ->
                        new MigrationScriptReaderImpl(
                                Arrays.asList("file:X:/snc/scripts", "file:" + absolutePathToScriptreader),
                                StandardCharsets.UTF_8,
                                "c",
                                singletonList(".http")).read())
                        .isInstanceOf(MigrationException.class)
                        .hasMessage("The location file:X:/snc/scripts is not a valid location!");
            }

            @Test
            void validPathButNoFiles() throws URISyntaxException {
                URL resourceDirectory = MigrationScriptReaderImpl.resolveURL("scriptreader");
                String absolutePathToScriptreader = Paths.get(resourceDirectory.toURI()).toFile().getAbsolutePath();
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList("file:" + absolutePathToScriptreader),
                        StandardCharsets.UTF_8,
                        "d",
                        singletonList(".http"));
                List<RawMigrationScript> actual = reader.read();
                assertThat(actual).isEmpty();
            }
        }
    }
}