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
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Andreas Keefer
 */
class MigrationScriptReaderImplTest {
    @Nested
    class readResources {
        @Nested
        class fromClassPath {
            @Test
            void nonJarFile() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList("classpath:scriptreader"),
                        StandardCharsets.UTF_8,
                        "c",
                        singletonList(".http"), "\n");
                List<RawMigrationScript> actual = reader.read();
                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript().setFileName("content.http").setContent("content!"),
                                new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"));
            }

            @Test
            void inJarFile() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList("classpath:META-INF"),
                        StandardCharsets.UTF_8,
                        "MANIFEST",
                        singletonList(".MF"), "\n");

                List<RawMigrationScript> res = reader.read();

                assertThat(res).hasSize(1);
                assertThat(res.get(0).getFileName()).isEqualTo("MANIFEST.MF");
            }

            @Test
            void invalidClasspath() {
                String classpath = "classpath:script reader";
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList(classpath),
                        StandardCharsets.UTF_8,
                        "c",
                        singletonList(".http"), "\n") {
                    @Override
                    protected Stream<RawMigrationScript> readFromLocation(String location) throws URISyntaxException, IOException {
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
                        Arrays.asList(".http", ".other"), "\n");
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
                        Arrays.asList(".http", ".other"), "\n");
                List<RawMigrationScript> actual = reader.read();
                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript().setFileName("content.http").setContent("content!"),
                                new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"),
                                new RawMigrationScript().setFileName("content.other").setContent("content!"));
            }

            @Test
            void exclude_locations_with_suffix() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList("classpath:scriptreader/issue36/location"),
                        StandardCharsets.UTF_8,
                        "c",
                        singletonList(".http"), "\n");

                List<RawMigrationScript> actual = reader.read();

                // should not contain anything from "classpath:scriptreader/issue36/location_with_suffix"
                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript().setFileName("content.http").setContent("content!"));
            }

            @Test
            void handle_locations_with_suffix() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        Arrays.asList("classpath:scriptreader/issue36/location",
                                "classpath:scriptreader/issue36/location_with_suffix"),
                        StandardCharsets.UTF_8,
                        "c",
                        singletonList(".http"), "\n");

                List<RawMigrationScript> actual = reader.read();

                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript().setFileName("content.http").setContent("content!"),
                                new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"));
            }

            @Test
            void withWrongProtocol() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        Arrays.asList("classpath:scriptreader", "http:scriptreader"),
                        StandardCharsets.UTF_8,
                        "c",
                        Arrays.asList(".http", ".other"), "\n");
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
                URL resourceDirectory = resolveURL("scriptreader");
                String absolutePathToScriptreader = Paths.get(resourceDirectory.toURI()).toFile().getAbsolutePath();
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList("file:" + absolutePathToScriptreader),
                        StandardCharsets.UTF_8,
                        "c",
                        singletonList(".http"), "\n");
                List<RawMigrationScript> actual = reader.read();
                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript().setFileName("content.http").setContent("content!"),
                                new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"));
            }

            @Test
            void exclude_locations_with_suffix() throws URISyntaxException {
                URL resourceDirectory = resolveURL("scriptreader");
                String absolutePathToScriptreader = Paths.get(resourceDirectory.toURI()).toFile().getAbsolutePath();
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList("file:"+absolutePathToScriptreader+"/issue36/location"),
                        StandardCharsets.UTF_8,
                        "c",
                        singletonList(".http"), "\n");

                List<RawMigrationScript> actual = reader.read();

                // should not contain anything from "classpath:scriptreader/issue36/location_with_suffix"
                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript().setFileName("content.http").setContent("content!"));
            }

            @Test
            void handle_locations_with_suffix() throws URISyntaxException {
                URL resourceDirectory = resolveURL("scriptreader");
                String absolutePathToScriptreader = Paths.get(resourceDirectory.toURI()).toFile().getAbsolutePath();
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        Arrays.asList("file:"+absolutePathToScriptreader+"/issue36/location",
                                "file:"+absolutePathToScriptreader+"/issue36/location_with_suffix"),
                        StandardCharsets.UTF_8,
                        "c",
                        singletonList(".http"), "\n");

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
                                singletonList(".http"), "\n").read())
                        .isInstanceOf(MigrationException.class)
                        .hasMessage("couldn't read scripts from file:X:/snc/scripts");
            }


            @Test
            void validAndInvalidPath() throws URISyntaxException {
                URL resourceDirectory = resolveURL("scriptreader");
                String absolutePathToScriptreader = Paths.get(resourceDirectory.toURI()).toFile().getAbsolutePath();
                assertThatThrownBy(() ->
                        new MigrationScriptReaderImpl(
                                Arrays.asList("file:X:/snc/scripts", "file:" + absolutePathToScriptreader),
                                StandardCharsets.UTF_8,
                                "c",
                                singletonList(".http"), "\n").read())
                        .isInstanceOf(MigrationException.class)
                        .hasMessage("couldn't read scripts from file:X:/snc/scripts");
            }

            @Test
            void validPathButNoFiles() throws URISyntaxException {
                URL resourceDirectory = resolveURL("scriptreader");
                String absolutePathToScriptreader = Paths.get(resourceDirectory.toURI()).toFile().getAbsolutePath();
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList("file:" + absolutePathToScriptreader),
                        StandardCharsets.UTF_8,
                        "d",
                        singletonList(".http"), "\n");
                List<RawMigrationScript> actual = reader.read();
                assertThat(actual).isEmpty();
            }
        }
    }

    private URL resolveURL(String path) {
        ClassLoader classLoader = MigrationScriptReaderImpl.getDefaultClassLoader();
        if (classLoader != null) {
            return classLoader.getResource(path);
        } else {
            return ClassLoader.getSystemResource(path);
        }
    }
}