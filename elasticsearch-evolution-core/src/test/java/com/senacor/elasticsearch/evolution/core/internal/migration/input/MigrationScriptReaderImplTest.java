package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
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
                        singletonList(".http"), "\n", false);
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
                        singletonList(".MF"), "\n", false);

                List<RawMigrationScript> res = reader.read();

                assertThat(res).isNotEmpty()
                        .allSatisfy(rawMigrationScript -> assertThat(rawMigrationScript.getFileName())
                                .as("fileName")
                                .isEqualTo("MANIFEST.MF"));
            }

            @Test
            void invalidClasspath() {
                String classpath = "classpath:script reader";
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        singletonList(classpath),
                        StandardCharsets.UTF_8,
                        "c",
                        singletonList(".http"), "\n", false) {
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
                        Arrays.asList(".http", ".other"), "\n", false);
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
                        Arrays.asList(".http", ".other"), "\n", false);
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
                        singletonList(".http"), "\n", false);

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
                        singletonList(".http"), "\n", false);

                List<RawMigrationScript> actual = reader.read();

                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript().setFileName("content.http").setContent("content!"),
                                new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"));
            }

            @Test
            void include_trailing_newlines() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        Arrays.asList("classpath:scriptreader/issue293_trailing_newlines"),
                        StandardCharsets.UTF_8,
                        "w",
                        singletonList(".http"), "\n", false);

                List<RawMigrationScript> actual = reader.read();

                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript().setFileName("with_trailing_newline.http").setContent("content!\n"));
            }

            @Test
            void withWrongProtocol() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                        Arrays.asList("classpath:scriptreader", "http:scriptreader"),
                        StandardCharsets.UTF_8,
                        "c",
                        Arrays.asList(".http", ".other"), "\n", false);
                assertThatThrownBy(reader::read)
                        .isInstanceOf(MigrationException.class)
                        .hasMessage("""
                                could not read location path http:scriptreader, should look like this: \
                                classpath:es/migration or this: file:/home/scripts/migration\
                                """);
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
                        singletonList(".http"), "\n", false);
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
                        singletonList(".http"), "\n", false);

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
                        singletonList(".http"), "\n", false);

                List<RawMigrationScript> actual = reader.read();

                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript().setFileName("content.http").setContent("content!"),
                                new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"));
            }

            @Test
            void invalidPath() {
                final MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(
                        singletonList("file:X:/snc/scripts"),
                        StandardCharsets.UTF_8,
                        "c",
                        singletonList(".http"), "\n", false);
                assertThatThrownBy(() -> underTest.read())
                        .isInstanceOf(MigrationException.class)
                        .hasMessage("couldn't read scripts from file:X:/snc/scripts");
            }


            @Test
            void validAndInvalidPath() throws URISyntaxException {
                URL resourceDirectory = resolveURL("scriptreader");
                String absolutePathToScriptreader = Paths.get(resourceDirectory.toURI()).toFile().getAbsolutePath();
                final MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(
                        Arrays.asList("file:X:/snc/scripts", "file:" + absolutePathToScriptreader),
                        StandardCharsets.UTF_8,
                        "c",
                        singletonList(".http"), "\n", false);
                assertThatThrownBy(() -> underTest.read())
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
                        singletonList(".http"), "\n", false);
                List<RawMigrationScript> actual = reader.read();
                assertThat(actual).isEmpty();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "foo\nbar\n",
            "foo\r\nbar\r\n",
            "foo\rbar\r"
    })
    void read_should_normalize_new_lines_to_defined_line_separator(String input) throws IOException {
        final String lineSeparator = "<my-line-separator>";
        MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                singletonList("ignore"),
                StandardCharsets.UTF_8,
                "ignore",
                singletonList(".ignore"),
                lineSeparator,
                false);

        final Stream<RawMigrationScript> res;
        try (BufferedReader bufferedReader = new BufferedReader(new StringReader(input))) {
            res = reader.read(bufferedReader, "filename");
        }

        assertThat(res)
                .containsExactlyInAnyOrder(new RawMigrationScript().setFileName("filename").setContent("foo" + lineSeparator + "bar" + lineSeparator));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "foo\nbar\n",
            "foo\r\nbar\r\n",
            "foo\rbar\r"
    })
    void read_should_trim_trailing_newlines_if_config_is_set(String input) throws IOException {
        final String lineSeparator = "<my-line-separator>";
        MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(
                singletonList("ignore"),
                StandardCharsets.UTF_8,
                "ignore",
                singletonList(".ignore"),
                lineSeparator,
                true);

        final Stream<RawMigrationScript> res;
        try (BufferedReader bufferedReader = new BufferedReader(new StringReader(input))) {
            res = reader.read(bufferedReader, "filename");
        }

        assertThat(res)
                .containsExactlyInAnyOrder(new RawMigrationScript().setFileName("filename").setContent("foo" + lineSeparator + "bar"));
    }

    private URL resolveURL(String path) {
        ClassLoader classLoader = getDefaultClassLoader();
        if (classLoader != null) {
            return classLoader.getResource(path);
        } else {
            return ClassLoader.getSystemResource(path);
        }
    }

    static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = MigrationScriptReaderImpl.class.getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }
        return cl;
    }
}