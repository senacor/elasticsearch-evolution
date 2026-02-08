package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationScriptParser;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationVersion;
import com.senacor.elasticsearch.evolution.core.api.migration.java.JavaMigration;
import com.senacor.elasticsearch.evolution.core.api.migration.java.JavaMigrationMetadata;
import com.senacor.elasticsearch.evolution.core.internal.model.FileNameInfo;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.*;
import com.senacor.elasticsearch.evolution.rest.abstraction.HttpMethod;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author Andreas Keefer
 */
@ExtendWith(MockitoExtension.class)
class MigrationScriptParserImplTest {

    @Nested
    class replaceParams {

        @Test
        void withOneReplacement_isReplaced() {
            String template = "POST /foo/${index}/bar\nfoo:bar";
            MigrationScriptParserImpl parser = new MigrationScriptParserImpl(null,
                    null,
                    Maps.newHashMap("index", "myIndex"),
                    "${",
                    "}",
                    true,
                    "\n");

            String replaced = parser.replaceParams(template);

            assertThat(replaced).isEqualTo("POST /foo/myIndex/bar\nfoo:bar");
        }

        @Test
        void withMultipleReplacementsOfSameKey_isReplaced() {
            String template = "POST /foo/${index}/bar\nfoo:${index}";
            MigrationScriptParserImpl parser = new MigrationScriptParserImpl(null,
                    null,
                    Maps.newHashMap("index", "myIndex"),
                    "${",
                    "}",
                    true,
                    "\n");

            String replaced = parser.replaceParams(template);

            assertThat(replaced).isEqualTo("POST /foo/myIndex/bar\nfoo:myIndex");
        }

        @Test
        void withMultipleReplacementsOfDifferentKey_isReplaced() {
            String template = "POST /foo/${index}/bar\nfoo:${foo-header}";
            Map<String, String> placeholders = Maps.newHashMap("index", "myIndex");
            placeholders.put("foo-header", "foobar");
            MigrationScriptParserImpl parser = new MigrationScriptParserImpl(null,
                    null,
                    placeholders,
                    "${",
                    "}",
                    true,
                    "\n");

            String replaced = parser.replaceParams(template);

            assertThat(replaced).isEqualTo("POST /foo/myIndex/bar\nfoo:foobar");
        }
    }

    @Nested
    class parseFilename {

        private final MigrationScriptParserImpl underTest = new MigrationScriptParserImpl(
                "V",
                Collections.singletonList(".http"),
                null,
                null,
                null,
                false,
                "\n");

        @Test
        void isParsable() {
            String fileName = "V0123.01_0002__my_description text.http";
            FileNameInfo fileNameInfo = underTest.parseFileNameFromScriptMigration(fileName);

            assertSoftly(softly -> {
                softly.assertThat(fileNameInfo.getDescription()).isEqualTo("my description text");
                softly.assertThat(fileNameInfo.getScriptName()).isEqualTo(fileName);
                softly.assertThat(fileNameInfo.getVersion()).isEqualTo(MigrationVersion.fromVersion("123.1.2"));
            });
        }

        @Test
        void notMatching_MajorVersionMustBeGreaterThan0() {
            String fileName = "V0.1__my_description text.http";

            assertThatThrownBy(() -> underTest.parseFileNameFromScriptMigration(fileName))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("used version '0.1' in migration file '%s' is not allowed. Major version must be greater than 0"
                            , fileName);
        }

        @Test
        void notMatching_otherFileEnding() {
            String fileName = "V0123.01_0002__my_description text.https";

            assertThatThrownBy(() -> underTest.parseFileNameFromScriptMigration(fileName))
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("Wrong versioned migration name format: '%s'. It must end with a configured suffix: '[.http]'"
                            , fileName);
        }

        @Test
        void notMatching_wrongSeparator() {
            String fileName = "V0123.01_0002-my_description text.http";

            assertThatThrownBy(() -> underTest.parseFileNameFromScriptMigration(fileName))
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("Description in migration filename is required: '%s'. It should look like this: 'V1.2__some_desctiption here.http'"
                            , fileName);
        }

        @Test
        void notMatching_wrongVersion() {
            String fileName = "V123.a__my_description text.http";

            assertThatThrownBy(() -> underTest.parseFileNameFromScriptMigration(fileName))
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("Invalid version containing non-numeric characters. Only 0..9 and . are allowed. Invalid version: 123.a");
        }
    }

    @Nested
    class parseCollection {
        private final MigrationScriptParser underTest = new MigrationScriptParserImpl(
                "V",
                Collections.singletonList(".http"),
                null,
                null,
                null,
                false,
                "\n");

        @Test
        void success() {
            String defaultContent = createDefaultContent();

            Collection<ParsedMigration<?>> res = underTest.parse(Arrays.asList(
                    new RawMigrationScript<>()
                            .setFileName("V1__create.http")
                            .setContent(new ScriptMigrationContent(defaultContent)),
                    new RawMigrationScript<>()
                            .setFileName("V2__update.http")
                            .setContent(new ScriptMigrationContent(defaultContent))));


            MigrationScriptRequest expectedRequest = new MigrationScriptRequest()
                    .setHttpMethod(HttpMethod.PUT)
                    .setPath("/")
                    .setHttpHeader(Map.of("Header", "value"))
                    .addToBody("{" + lineSeparator() + "\"body\":\"value\"" + lineSeparator() + "}");
            assertThat(res).containsExactlyInAnyOrder(
                    new ParsedMigration<>()
                            .setChecksum(defaultContent.hashCode())
                            .setFileNameInfo(new FileNameInfoImpl(MigrationVersion.fromVersion("1"), "create", "V1__create.http"))
                            .setMigrationRequest(expectedRequest),
                    new ParsedMigration<>()
                            .setChecksum(defaultContent.hashCode())
                            .setFileNameInfo(new FileNameInfoImpl(MigrationVersion.fromVersion("2"), "update", "V2__update.http"))
                            .setMigrationRequest(expectedRequest));
        }
    }

    @Nested
    class parseSingle {
        private final MigrationScriptParserImpl underTest = new MigrationScriptParserImpl(
                "V",
                Collections.singletonList(".http"),
                null,
                null,
                null,
                false,
                "\n");

        @Test
        void success_withMethodAndPathAndHeaderAndBody() {
            String defaultContent = createDefaultContent();

            String fileName = "V1__create.http";
            ParsedMigration<MigrationScriptRequest> res = (ParsedMigration<MigrationScriptRequest>) underTest.parse(new RawMigrationScript<ScriptMigrationContent>()
                    .setFileName(fileName)
                    .setContent(new ScriptMigrationContent(defaultContent)));

            assertSoftly(softly -> {
                softly.assertThat(res.getChecksum())
                        .as("Checksum")
                        .isEqualTo(defaultContent.hashCode());
                softly.assertThat(res.getFileNameInfo().getVersion())
                        .as("version")
                        .isEqualTo(MigrationVersion.fromVersion("1"));
                softly.assertThat(res.getFileNameInfo().getScriptName())
                        .as("scriptName")
                        .isEqualTo(fileName);
                softly.assertThat(res.getFileNameInfo().getDescription())
                        .as("description")
                        .isEqualTo("create");
                softly.assertThat(res.getMigrationRequest().getHttpMethod())
                        .as("methot")
                        .isEqualTo(HttpMethod.PUT);
                softly.assertThat(res.getMigrationRequest().getPath())
                        .as("path")
                        .isEqualTo("/");
                softly.assertThat(res.getMigrationRequest().getHttpHeader())
                        .as("header")
                        .containsEntry("Header", "value")
                        .hasSize(1);
                softly.assertThat(res.getMigrationRequest().getBody())
                        .as("body")
                        .isEqualTo("{" + lineSeparator() + "\"body\":\"value\"" + lineSeparator() + "}");
            });
        }

        @Test
        void success_noHeader() {
            String defaultContent = "PUT /" + lineSeparator() +
                    lineSeparator() +
                    "{" + lineSeparator() + "\"body\":\"value\"" + lineSeparator() + "}";

            String fileName = "V1__create.http";
            ParsedMigration<MigrationScriptRequest> res = (ParsedMigration<MigrationScriptRequest>) underTest.parse(new RawMigrationScript<ScriptMigrationContent>()
                    .setFileName(fileName)
                    .setContent(new ScriptMigrationContent(defaultContent)));

            assertSoftly(softly -> {
                softly.assertThat(res.getChecksum())
                        .as("Checksum")
                        .isEqualTo(defaultContent.hashCode());
                softly.assertThat(res.getFileNameInfo().getVersion())
                        .as("version")
                        .isEqualTo(MigrationVersion.fromVersion("1"));
                softly.assertThat(res.getFileNameInfo().getScriptName())
                        .as("scriptName")
                        .isEqualTo(fileName);
                softly.assertThat(res.getFileNameInfo().getDescription())
                        .as("description")
                        .isEqualTo("create");
                softly.assertThat(res.getMigrationRequest().getHttpMethod())
                        .as("methot")
                        .isEqualTo(HttpMethod.PUT);
                softly.assertThat(res.getMigrationRequest().getPath())
                        .as("path")
                        .isEqualTo("/");
                softly.assertThat(res.getMigrationRequest().getHttpHeader())
                        .as("header")
                        .isEmpty();
                softly.assertThat(res.getMigrationRequest().getBody())
                        .as("body")
                        .isEqualTo("{" + lineSeparator() + "\"body\":\"value\"" + lineSeparator() + "}");
            });
        }

        @Test
        void success_noBody() {
            String defaultContent = "PUT /" + lineSeparator() +
                    "Header = value:a";

            String fileName = "V1__create.http";
            ParsedMigration<MigrationScriptRequest> res = (ParsedMigration<MigrationScriptRequest>) underTest.parse(new RawMigrationScript<ScriptMigrationContent>()
                    .setFileName(fileName)
                    .setContent(new ScriptMigrationContent(defaultContent)));

            assertSoftly(softly -> {
                softly.assertThat(res.getChecksum())
                        .as("Checksum")
                        .isEqualTo(defaultContent.hashCode());
                softly.assertThat(res.getFileNameInfo().getVersion())
                        .as("version")
                        .isEqualTo(MigrationVersion.fromVersion("1"));
                softly.assertThat(res.getFileNameInfo().getScriptName())
                        .as("scriptName")
                        .isEqualTo(fileName);
                softly.assertThat(res.getFileNameInfo().getDescription())
                        .as("description")
                        .isEqualTo("create");
                softly.assertThat(res.getMigrationRequest().getHttpMethod())
                        .as("methot")
                        .isEqualTo(HttpMethod.PUT);
                softly.assertThat(res.getMigrationRequest().getPath())
                        .as("path")
                        .isEqualTo("/");
                softly.assertThat(res.getMigrationRequest().getHttpHeader())
                        .as("header")
                        .containsEntry("Header", "value:a")
                        .hasSize(1);
                softly.assertThat(res.getMigrationRequest().getBody())
                        .as("body")
                        .isBlank();
            });
        }

        @Test
        void success_noHeaderAndNoBody() {
            String defaultContent = "put /";

            String fileName = "V1__create.http";
            ParsedMigration<MigrationScriptRequest> res = (ParsedMigration<MigrationScriptRequest>) underTest.parse(new RawMigrationScript<ScriptMigrationContent>()
                    .setFileName(fileName)
                    .setContent(new ScriptMigrationContent(defaultContent)));

            assertSoftly(softly -> {
                softly.assertThat(res.getChecksum())
                        .as("Checksum")
                        .isEqualTo(defaultContent.hashCode());
                softly.assertThat(res.getFileNameInfo().getVersion())
                        .as("version")
                        .isEqualTo(MigrationVersion.fromVersion("1"));
                softly.assertThat(res.getFileNameInfo().getScriptName())
                        .as("scriptName")
                        .isEqualTo(fileName);
                softly.assertThat(res.getFileNameInfo().getDescription())
                        .as("description")
                        .isEqualTo("create");
                softly.assertThat(res.getMigrationRequest().getHttpMethod())
                        .as("methot")
                        .isEqualTo(HttpMethod.PUT);
                softly.assertThat(res.getMigrationRequest().getPath())
                        .as("path")
                        .isEqualTo("/");
                softly.assertThat(res.getMigrationRequest().getHttpHeader())
                        .as("header")
                        .isEmpty();
                softly.assertThat(res.getMigrationRequest().getBody())
                        .as("body")
                        .isBlank();
            });
        }

        @Test
        void success_replacePlaceholders() {
            String defaultContent = "PUT /${index}" + lineSeparator() +
                    "Authorization: ${auth}" + lineSeparator() +
                    lineSeparator() +
                    "{" + lineSeparator() + "\"index\":\"${index}\"" + lineSeparator() + "}";
            String fileName = "V1__create.http";
            HashMap<String, String> placeholders = new HashMap<>();
            placeholders.put("index", "my-index");
            placeholders.put("auth", "my-auth-key");

            ParsedMigration<MigrationScriptRequest> res = (ParsedMigration<MigrationScriptRequest>) new MigrationScriptParserImpl(
                    "V",
                    Collections.singletonList(".http"),
                    placeholders,
                    "${",
                    "}",
                    true,
                    "\n")
                    .parse(new RawMigrationScript<ScriptMigrationContent>()
                            .setFileName(fileName)
                            .setContent(new ScriptMigrationContent(defaultContent)));

            assertSoftly(softly -> {
                softly.assertThat(res.getChecksum())
                        .as("Checksum")
                        .isEqualTo(defaultContent.hashCode());
                softly.assertThat(res.getFileNameInfo().getVersion())
                        .as("version")
                        .isEqualTo(MigrationVersion.fromVersion("1"));
                softly.assertThat(res.getFileNameInfo().getScriptName())
                        .as("scriptName")
                        .isEqualTo(fileName);
                softly.assertThat(res.getFileNameInfo().getDescription())
                        .as("description")
                        .isEqualTo("create");
                softly.assertThat(res.getMigrationRequest().getHttpMethod())
                        .as("methot")
                        .isEqualTo(HttpMethod.PUT);
                softly.assertThat(res.getMigrationRequest().getPath())
                        .as("path")
                        .isEqualTo("/my-index");
                softly.assertThat(res.getMigrationRequest().getHttpHeader())
                        .as("header")
                        .containsEntry("Authorization", "my-auth-key");
                softly.assertThat(res.getMigrationRequest().getBody())
                        .as("body")
                        .isEqualTo("{" + lineSeparator() + "\"index\":\"my-index\"" + lineSeparator() + "}");
            });
        }

        @Test
        void success_NotRemoveTrailingNewlines() {
            String defaultContent = "PUT /my-index" + lineSeparator() +
                    lineSeparator() +
                    "{" + lineSeparator() + "\"index\":\"my-index\"" + lineSeparator() + "}" +
                    lineSeparator();
            String fileName = "V1__create.http";
            HashMap<String, String> placeholders = new HashMap<>();

            ParsedMigration<MigrationScriptRequest> res = (ParsedMigration<MigrationScriptRequest>) new MigrationScriptParserImpl(
                    "V",
                    Collections.singletonList(".http"),
                    placeholders,
                    "${",
                    "}",
                    false,
                    "\n")
                    .parse(new RawMigrationScript<ScriptMigrationContent>()
                            .setFileName(fileName)
                            .setContent(new ScriptMigrationContent(defaultContent)));

            assertSoftly(softly -> {
                softly.assertThat(res.getChecksum())
                        .as("Checksum")
                        .isEqualTo(defaultContent.hashCode());
                softly.assertThat(res.getFileNameInfo().getVersion())
                        .as("version")
                        .isEqualTo(MigrationVersion.fromVersion("1"));
                softly.assertThat(res.getFileNameInfo().getScriptName())
                        .as("scriptName")
                        .isEqualTo(fileName);
                softly.assertThat(res.getFileNameInfo().getDescription())
                        .as("description")
                        .isEqualTo("create");
                softly.assertThat(res.getMigrationRequest().getHttpMethod())
                        .as("methot")
                        .isEqualTo(HttpMethod.PUT);
                softly.assertThat(res.getMigrationRequest().getPath())
                        .as("path")
                        .isEqualTo("/my-index");
                softly.assertThat(res.getMigrationRequest().getHttpHeader())
                        .as("header")
                        .isEmpty();
                softly.assertThat(res.getMigrationRequest().getBody())
                        .as("body")
                        .isEqualTo("{" + lineSeparator() + "\"index\":\"my-index\"" + lineSeparator() + "}" + lineSeparator());
            });
        }

        @Test
        void failed_MethodAndPathInvalid() {
            final RawMigrationScript<?> rawMigrationScript = new RawMigrationScript<ScriptMigrationContent>()
                    .setFileName("V1__create.http")
                    .setContent(new ScriptMigrationContent("/"));

            assertThatThrownBy(() -> underTest.parse(rawMigrationScript))
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("can't parse method and path: '/'. Method and path must be separated by space and should look like this: 'PUT /my_index'");
        }

        @Test
        void failed_methodNotSupported() {
            final RawMigrationScript<?> rawMigrationScript = new RawMigrationScript<ScriptMigrationContent>()
                    .setFileName("V1__create.http")
                    .setContent(new ScriptMigrationContent("TRACE /"));

            assertThatThrownBy(() -> underTest.parse(rawMigrationScript))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Method 'TRACE' not supported, only [GET, HEAD, POST, PUT, DELETE, OPTIONS, PATCH] is supported.");
        }

        @Test
        void failed_headerInvalid() {
            final RawMigrationScript<?> rawMigrationScript = new RawMigrationScript<ScriptMigrationContent>()
                    .setFileName("V1__create.http")
                    .setContent(new ScriptMigrationContent("PUT /" + lineSeparator() + "Header value"));

            assertThatThrownBy(() ->
                    underTest.parse(rawMigrationScript))
                    .isInstanceOf(MigrationException.class)
                    .hasMessage("can't parse header: 'Header value'. Header must be separated by ':' and should look like this: 'Content-Type: application/json'");
        }

        @Test
        void should_ParseJavaMigration_fromFileName(@Mock JavaMigration javaMigration) {
            final JavaMigrationRequestContent content = new JavaMigrationRequestContent(javaMigration);
            final String fileName = "V1_2__create";

            final ParsedMigration<?> res = underTest.parse(new RawMigrationScript<JavaMigrationRequestContent>()
                    .setFileName(fileName)
                    .setContent(content));

            assertThat(res.getMigrationRequest())
                    .as("MigrationRequest")
                    .isSameAs(content);
            assertThat(res.getChecksum())
                    .as("Checksum")
                    .isZero();
            assertThat(res.getFileNameInfo())
                    .as("FileNameInfo")
                    .isEqualTo(new FileNameInfoImpl(MigrationVersion.fromVersion("1.2"), "create", fileName));
        }

        @Test
        void should_ParseJavaMigration_fromMetadata(@Mock JavaMigration javaMigration) {
            final JavaMigrationMetadata metadata = new JavaMigrationMetadata(MigrationVersion.fromVersion("3.1"), "my description");
            Mockito.when(javaMigration.getMetadata())
                    .thenReturn(metadata);
            final JavaMigrationRequestContent content = new JavaMigrationRequestContent(javaMigration);
            final String fileName = "V1_2__create";

            final ParsedMigration<?> res = underTest.parse(new RawMigrationScript<JavaMigrationRequestContent>()
                    .setFileName(fileName)
                    .setContent(content));

            assertThat(res.getMigrationRequest())
                    .as("MigrationRequest")
                    .isSameAs(content);
            assertThat(res.getChecksum())
                    .as("Checksum")
                    .isZero();
            assertThat(res.getFileNameInfo())
                    .as("FileNameInfo")
                    .isEqualTo(new FileNameInfoImpl(metadata.version(), metadata.description(), fileName));
        }
    }

    private String createDefaultContent() {
        return "PUT /" + lineSeparator() +
                " # some # comment " + lineSeparator() +
                "Header: value" + lineSeparator() +
                " // some // comment " + lineSeparator() +
                lineSeparator() +
                "{" + lineSeparator() + "\"body\":\"value\"" + lineSeparator() + "}";
    }
}