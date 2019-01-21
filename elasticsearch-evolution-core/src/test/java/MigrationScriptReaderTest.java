import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.internal.migration.input.MigrationScriptReader;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;
import org.junit.jupiter.api.Nested;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andreas Keefer
 */
class MigrationScriptReaderTest {
    @Nested
    class readResources {

        @Test
        void fromClassPathJar() {
            MigrationScriptReader reader = new MigrationScriptReader(Arrays.asList("META-INF/maven/org.assertj/assertj-core"),
                    StandardCharsets.UTF_8,
                    "p",
                    Arrays.asList(".properties"));
            List<RawMigrationScript> actual = reader.readAllScripts();
            assertThat(actual).hasSize(1);
        }

        @Test
        void fromClassPathResourcesDircetory() {
            MigrationScriptReader reader = new MigrationScriptReader(Arrays.asList("scriptreader"),
                    StandardCharsets.UTF_8,
                    "c",
                    Arrays.asList(".http"));
            List<RawMigrationScript> actual = reader.readAllScripts();
            assertThat(actual).contains(new RawMigrationScript().setFileName("content.http").setContent("content!"),
                    new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"));

        }

        @Test
        void fromClassPathResourcesDirectoryAndMultipleSuffixes() {
            MigrationScriptReader reader = new MigrationScriptReader(Arrays.asList("scriptreader"),
                    StandardCharsets.UTF_8,
                    "c",
                    Arrays.asList(".http", ".other"));
            List<RawMigrationScript> actual = reader.readAllScripts();
            assertThat(actual).contains(new RawMigrationScript().setFileName("content.http").setContent("content!"),
                    new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"),
                    new RawMigrationScript().setFileName("content.other").setContent("content!"));
        }

        @Test
        void fromClasspathResourcesDirectoryWithClassPathPrepended() {
            MigrationScriptReader reader = new MigrationScriptReader(Arrays.asList("classpath:scriptreader"),
                    StandardCharsets.UTF_8,
                    "c",
                    Arrays.asList(".http", ".other"));
            List<RawMigrationScript> actual = reader.readAllScripts();
            assertThat(actual).contains(new RawMigrationScript().setFileName("content.http").setContent("content!"),
                    new RawMigrationScript().setFileName("content_sub.http").setContent("sub content!"),
                    new RawMigrationScript().setFileName("content.other").setContent("content!"));
        }

        @Test(expected = MigrationException.class)
        void fromClasspathResourcesDirectoryWithWrongProtocol() {
            MigrationScriptReader reader = new MigrationScriptReader(Arrays.asList("classpath:scriptreader", "http:scriptreader"),
                    StandardCharsets.UTF_8,
                    "c",
                    Arrays.asList(".http", ".other"));

        }

    }
}