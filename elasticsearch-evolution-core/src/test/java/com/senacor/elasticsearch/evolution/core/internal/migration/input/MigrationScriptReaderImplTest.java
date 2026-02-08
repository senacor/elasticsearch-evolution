package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.config.ElasticsearchEvolutionConfigImpl;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationVersion;
import com.senacor.elasticsearch.evolution.core.api.migration.java.JavaMigrationMetadata;
import com.senacor.elasticsearch.evolution.core.internal.migration.input.testjavamigrations.direct.V1_2__Valid_Filename;
import com.senacor.elasticsearch.evolution.core.internal.migration.input.testjavamigrations.nodefaultconstructor.V1_2__NoDefaultConstructor;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.JavaMigrationRequestContent;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ScriptMigrationContent;
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
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author Andreas Keefer
 */
class MigrationScriptReaderImplTest {

    private final ElasticsearchEvolutionConfigImpl config = new ElasticsearchEvolutionConfigImpl();

    @Nested
    class ReadJavaMigrationsFromClasspathShould {

        @Test
        void return_JavaMigrationWithValidFilename_DirectInLocation() {
            MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(config
                    .setLocations(singletonList("classpath:com/senacor/elasticsearch/evolution/core/internal/migration/input/testjavamigrations/direct")));

            final List<RawMigrationScript<?>> res = underTest.read();

            assertThat(res)
                    .hasSize(1)
                    .anySatisfy(rawMigrationScript -> {
                        assertSoftly(softly -> {
                            softly.assertThat(rawMigrationScript.getFileName())
                                    .as("fileName")
                                    .isEqualTo("V1_2__Valid_Filename");
                            softly.assertThat(rawMigrationScript.getContent())
                                    .as("content")
                                    .isInstanceOf(JavaMigrationRequestContent.class);
                            JavaMigrationRequestContent content = (JavaMigrationRequestContent) rawMigrationScript.getContent();
                            softly.assertThat(content.javaMigration().getChecksum())
                                    .as("checksum")
                                    .isZero();
                            softly.assertThat(content.javaMigration().getMetadata())
                                    .as("metadata")
                                    .isNull();
                        });

                    });
        }

        @Test
        void return_JavaMigrationWithProvidedMetadata_DirectInLocation() {
            MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(config
                    .setLocations(singletonList("classpath:com/senacor/elasticsearch/evolution/core/internal/migration/input/testjavamigrations/metadata")));

            final List<RawMigrationScript<?>> res = underTest.read();

            assertThat(res)
                    .hasSize(1)
                    .anySatisfy(rawMigrationScript -> {
                        assertSoftly(softly -> {
                            softly.assertThat(rawMigrationScript.getFileName())
                                    .as("fileName")
                                    .isEqualTo("InvalidFilenameWithMetadata");
                            softly.assertThat(rawMigrationScript.getContent())
                                    .as("content")
                                    .isInstanceOf(JavaMigrationRequestContent.class);
                            JavaMigrationRequestContent content = (JavaMigrationRequestContent) rawMigrationScript.getContent();
                            softly.assertThat(content.javaMigration().getChecksum())
                                    .as("checksum")
                                    .isEqualTo(42);
                            softly.assertThat(content.javaMigration().getMetadata())
                                    .as("metadata")
                                    .isEqualTo(new JavaMigrationMetadata(MigrationVersion.fromVersion("1.3"), "Invalid Filename With Metadata"));
                        });

                    });
        }

        @Test
        void return_JavaMigrationWithValidFilename_SubdirInLocation() {
            MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(config
                    .setLocations(singletonList("classpath:com/senacor/elasticsearch/evolution/core/internal/migration/input/testjavamigrations/withsubdir")));

            final List<RawMigrationScript<?>> res = underTest.read();

            assertThat(res)
                    .hasSize(1)
                    .anySatisfy(rawMigrationScript -> {
                        assertSoftly(softly -> {
                            softly.assertThat(rawMigrationScript.getFileName())
                                    .as("fileName")
                                    .isEqualTo("V1_4__Valid_Filename_in_subdir");
                            softly.assertThat(rawMigrationScript.getContent())
                                    .as("content")
                                    .isInstanceOf(JavaMigrationRequestContent.class);
                            JavaMigrationRequestContent content = (JavaMigrationRequestContent) rawMigrationScript.getContent();
                            softly.assertThat(content.javaMigration().getChecksum())
                                    .as("checksum")
                                    .isZero();
                            softly.assertThat(content.javaMigration().getMetadata())
                                    .as("metadata")
                                    .isNull();
                        });

                    });
        }

        @Test
        void throw_MigrationException_when_NoDefaultConstructorInJavaMigration() {
            MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(config
                    .setLocations(singletonList("classpath:com/senacor/elasticsearch/evolution/core/internal/migration/input/testjavamigrations/nodefaultconstructor")));

            assertThatCode(underTest::read)
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("JavaMigration com.senacor.elasticsearch.evolution.core.internal.migration.input.testjavamigrations.nodefaultconstructor.V1_2__NoDefaultConstructor does not have a public no-args constructor!");
        }

        @Test
        void throw_MigrationException_when_NoPublicDefaultConstructorInJavaMigration() {
            MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(config
                    .setLocations(singletonList("classpath:com/senacor/elasticsearch/evolution/core/internal/migration/input/testjavamigrations/nopublicdefaultconstructor")));

            assertThatCode(underTest::read)
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("JavaMigration com.senacor.elasticsearch.evolution.core.internal.migration.input.testjavamigrations.nopublicdefaultconstructor.V1_2__NoPublicDefaultConstructor does not have a public no-args constructor!");
        }

        @Test
        void throw_MigrationException_when_NameInvalidAndNoMetadata() {
            MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(config
                    .setLocations(singletonList("classpath:com/senacor/elasticsearch/evolution/core/internal/migration/input/testjavamigrations/invalidfilenameandnometadata")));

            assertThatCode(underTest::read)
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("JavaMigration com.senacor.elasticsearch.evolution.core.internal.migration.input.testjavamigrations.invalidfilenameandnometadata.Invalid_Filename_no_metadata does not provide metadata and does not match migration file name pattern!");
        }

        @Test
        void throw_MigrationException_when_InstantiationFailed() {
            MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(config
                    .setLocations(singletonList("classpath:com/senacor/elasticsearch/evolution/core/internal/migration/input/testjavamigrations/instantiationfailed")));

            assertThatCode(underTest::read)
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("Couldn't create instance of JavaMigration: com.senacor.elasticsearch.evolution.core.internal.migration.input.testjavamigrations.instantiationfailed.V1_2__Failing_instantiation");
        }
    }

    @Nested
    class readResources {
        @Nested
        class fromClassPath {
            @Test
            void nonJarFile() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                        .setLocations(singletonList("classpath:scriptreader"))
                        .setEsMigrationPrefix("c"));

                List<RawMigrationScript<?>> actual = reader.read();

                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content.http")
                                        .setContent(new ScriptMigrationContent("content!")),
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content_sub.http")
                                        .setContent(new ScriptMigrationContent("sub content!")));
            }

            @Test
            void inJarFile() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                        .setLocations(singletonList("classpath:META-INF"))
                        .setEsMigrationPrefix("MANIFEST")
                        .setEsMigrationSuffixes(singletonList(".MF")));

                List<RawMigrationScript<?>> res = reader.read();

                assertThat(res).isNotEmpty()
                        .allSatisfy(rawMigrationScript -> assertThat(rawMigrationScript.getFileName())
                                .as("fileName")
                                .isEqualTo("MANIFEST.MF"));
            }

            @Test
            void invalidClasspath() {
                String classpath = "classpath:script reader";
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                        .setLocations(singletonList(classpath))
                        .setEsMigrationPrefix("c")) {
                    @Override
                    protected Stream<RawMigrationScript<?>> readFromLocation(String location) throws URISyntaxException, IOException {
                        throw new URISyntaxException("input", "reason");
                    }
                };

                assertThatThrownBy(reader::read)
                        .isInstanceOf(MigrationException.class)
                        .hasMessage("couldn't read migrations from " + classpath);
            }

            @Test
            void multipleSuffixes() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                        .setLocations(singletonList("classpath:scriptreader"))
                        .setEsMigrationPrefix("c")
                        .setEsMigrationSuffixes(Arrays.asList(".http", ".other")));

                List<RawMigrationScript<?>> actual = reader.read();
                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content.http")
                                        .setContent(new ScriptMigrationContent("content!")),
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content_sub.http")
                                        .setContent(new ScriptMigrationContent("sub content!")),
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content.other")
                                        .setContent(new ScriptMigrationContent("content!")));
            }

            @Test
            void handlingDuplicates() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                        .setLocations(Arrays.asList("classpath:scriptreader", "classpath:scriptreader"))
                        .setEsMigrationPrefix("c")
                        .setEsMigrationSuffixes(Arrays.asList(".http", ".other")));

                List<RawMigrationScript<?>> actual = reader.read();

                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content.http")
                                        .setContent(new ScriptMigrationContent("content!")),
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content_sub.http")
                                        .setContent(new ScriptMigrationContent("sub content!")),
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content.other")
                                        .setContent(new ScriptMigrationContent("content!")));
            }

            @Test
            void exclude_locations_with_suffix() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                        .setLocations(singletonList("classpath:scriptreader/issue36/location"))
                        .setEsMigrationPrefix("c"));

                List<RawMigrationScript<?>> actual = reader.read();

                // should not contain anything from "classpath:scriptreader/issue36/location_with_suffix"
                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content.http")
                                        .setContent(new ScriptMigrationContent("content!")));
            }

            @Test
            void handle_locations_with_suffix() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                        .setLocations(Arrays.asList("classpath:scriptreader/issue36/location",
                                "classpath:scriptreader/issue36/location_with_suffix"))
                        .setEsMigrationPrefix("c"));

                List<RawMigrationScript<?>> actual = reader.read();

                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content.http")
                                        .setContent(new ScriptMigrationContent("content!")),
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content_sub.http")
                                        .setContent(new ScriptMigrationContent("sub content!")));
            }

            @Test
            void include_trailing_newlines() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                        .setLocations(List.of("classpath:scriptreader/issue293_trailing_newlines"))
                        .setEsMigrationPrefix("w"));

                List<RawMigrationScript<?>> actual = reader.read();

                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("with_trailing_newline.http")
                                        .setContent(new ScriptMigrationContent("content!\n")));
            }

            @Test
            void withWrongProtocol() {
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                        .setLocations(Arrays.asList("classpath:scriptreader", "http:scriptreader"))
                        .setEsMigrationPrefix("c")
                        .setEsMigrationSuffixes(Arrays.asList(".http", ".other")));

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
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                        .setLocations(singletonList("file:" + absolutePathToScriptreader))
                        .setEsMigrationPrefix("c"));

                List<RawMigrationScript<?>> actual = reader.read();

                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content.http")
                                        .setContent(new ScriptMigrationContent("content!")),
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content_sub.http")
                                        .setContent(new ScriptMigrationContent("sub content!")));
            }

            @Test
            void exclude_locations_with_suffix() throws URISyntaxException {
                URL resourceDirectory = resolveURL("scriptreader");
                String absolutePathToScriptreader = Paths.get(resourceDirectory.toURI()).toFile().getAbsolutePath();
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                        .setLocations(singletonList("file:" + absolutePathToScriptreader + "/issue36/location"))
                        .setEsMigrationPrefix("c"));

                List<RawMigrationScript<?>> actual = reader.read();

                // should not contain anything from "classpath:scriptreader/issue36/location_with_suffix"
                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content.http")
                                        .setContent(new ScriptMigrationContent("content!")));
            }

            @Test
            void handle_locations_with_suffix() throws URISyntaxException {
                URL resourceDirectory = resolveURL("scriptreader");
                String absolutePathToScriptreader = Paths.get(resourceDirectory.toURI()).toFile().getAbsolutePath();
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                        .setLocations(Arrays.asList("file:"+absolutePathToScriptreader+"/issue36/location",
                                "file:"+absolutePathToScriptreader+"/issue36/location_with_suffix"))
                        .setEsMigrationPrefix("c"));

                List<RawMigrationScript<?>> actual = reader.read();

                assertThat(actual)
                        .containsExactlyInAnyOrder(
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content.http")
                                        .setContent(new ScriptMigrationContent("content!")),
                                new RawMigrationScript<ScriptMigrationContent>()
                                        .setFileName("content_sub.http")
                                        .setContent(new ScriptMigrationContent("sub content!")));
            }

            @Test
            void invalidPath() {
                final MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(config
                        .setLocations(singletonList("file:X:/snc/scripts"))
                        .setEsMigrationPrefix("c"));

                assertThatThrownBy(underTest::read)
                        .isInstanceOf(MigrationException.class)
                        .hasMessage("couldn't read migrations from file:X:/snc/scripts");
            }


            @Test
            void validAndInvalidPath() throws URISyntaxException {
                URL resourceDirectory = resolveURL("scriptreader");
                String absolutePathToScriptreader = Paths.get(resourceDirectory.toURI()).toFile().getAbsolutePath();
                final MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(config
                        .setLocations(Arrays.asList("file:X:/snc/scripts", "file:" + absolutePathToScriptreader))
                        .setEsMigrationPrefix("c"));

                assertThatThrownBy(underTest::read)
                        .isInstanceOf(MigrationException.class)
                        .hasMessage("couldn't read migrations from file:X:/snc/scripts");
            }

            @Test
            void validPathButNoFiles() throws URISyntaxException {
                URL resourceDirectory = resolveURL("scriptreader");
                String absolutePathToScriptreader = Paths.get(resourceDirectory.toURI()).toFile().getAbsolutePath();
                MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                        .setLocations(singletonList("file:" + absolutePathToScriptreader))
                        .setEsMigrationPrefix("d"));

                List<RawMigrationScript<?>> actual = reader.read();

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
        MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                .setLocations(singletonList("ignore"))
                .setEsMigrationPrefix("ignore")
                .setEsMigrationSuffixes(singletonList(".ignore"))
                .setLineSeparator(lineSeparator));

        final Stream<RawMigrationScript<ScriptMigrationContent>> res;
        try (BufferedReader bufferedReader = new BufferedReader(new StringReader(input))) {
            res = reader.read(bufferedReader, "filename");
        }

        assertThat(res)
                .containsExactlyInAnyOrder(new RawMigrationScript<ScriptMigrationContent>()
                        .setFileName("filename")
                        .setContent(new ScriptMigrationContent("foo" + lineSeparator + "bar" + lineSeparator)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "foo\nbar\n",
            "foo\r\nbar\r\n",
            "foo\rbar\r"
    })
    void read_should_trim_trailing_newlines_if_config_is_set(String input) throws IOException {
        final String lineSeparator = "<my-line-separator>";
        MigrationScriptReaderImpl reader = new MigrationScriptReaderImpl(config
                .setLocations(singletonList("ignore"))
                .setEncoding(StandardCharsets.UTF_8)
                .setEsMigrationPrefix("ignore")
                .setEsMigrationSuffixes(singletonList(".ignore"))
                .setLineSeparator(lineSeparator)
                .setTrimTrailingNewlineInMigrations(true));

        final Stream<RawMigrationScript<ScriptMigrationContent>> res;
        try (BufferedReader bufferedReader = new BufferedReader(new StringReader(input))) {
            res = reader.read(bufferedReader, "filename");
        }

        assertThat(res)
                .containsExactlyInAnyOrder(new RawMigrationScript<ScriptMigrationContent>()
                        .setFileName("filename")
                        .setContent(new ScriptMigrationContent("foo" + lineSeparator + "bar")));
    }

    @Test
    void read_should_use_provided_JavaConfigInstances_from_config() {
        MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(config
                .setLocations(singletonList("classpath:unknown"))
                .setJavaMigrations(List.of(new V1_2__Valid_Filename())));

        final List<RawMigrationScript<?>> res = underTest.read();

        assertThat(res)
                .hasSize(1)
                .anySatisfy(rawMigrationScript -> {
                    assertSoftly(softly -> {
                        softly.assertThat(rawMigrationScript.getFileName())
                                .as("fileName")
                                .isEqualTo("V1_2__Valid_Filename");
                        softly.assertThat(rawMigrationScript.getContent())
                                .as("content")
                                .isInstanceOf(JavaMigrationRequestContent.class);
                        JavaMigrationRequestContent content = (JavaMigrationRequestContent) rawMigrationScript.getContent();
                        softly.assertThat(content.javaMigration().getChecksum())
                                .as("checksum")
                                .isZero();
                        softly.assertThat(content.javaMigration().getMetadata())
                                .as("metadata")
                                .isNull();
                    });

                });
    }

    @Test
    void read_should_use_JavaMigrationClassProvider_from_config() {
        MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(config
                .setLocations(singletonList("classpath:unknown"))
                .setJavaMigrationClassProvider(cfg -> List.of(V1_2__Valid_Filename.class)));

        final List<RawMigrationScript<?>> res = underTest.read();

        assertThat(res)
                .hasSize(1)
                .anySatisfy(rawMigrationScript -> {
                    assertSoftly(softly -> {
                        softly.assertThat(rawMigrationScript.getFileName())
                                .as("fileName")
                                .isEqualTo("V1_2__Valid_Filename");
                        softly.assertThat(rawMigrationScript.getContent())
                                .as("content")
                                .isInstanceOf(JavaMigrationRequestContent.class);
                        JavaMigrationRequestContent content = (JavaMigrationRequestContent) rawMigrationScript.getContent();
                        softly.assertThat(content.javaMigration().getChecksum())
                                .as("checksum")
                                .isZero();
                        softly.assertThat(content.javaMigration().getMetadata())
                                .as("metadata")
                                .isNull();
                    });

                });
    }

    @Test
    void read_should_ThrowMigrationException_when_class_from_JavaMigrationClassProvider_has_no_NoArgsConstructor() {
        MigrationScriptReaderImpl underTest = new MigrationScriptReaderImpl(config
                .setLocations(singletonList("classpath:unknown"))
                .setJavaMigrationClassProvider(cfg -> List.of(V1_2__NoDefaultConstructor.class)));

        assertThatCode(underTest::read)
                .isInstanceOf(MigrationException.class)
                .hasMessage("Couldn't create instance of JavaMigration: %s", V1_2__NoDefaultConstructor.class.getName());
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