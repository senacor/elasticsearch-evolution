package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Andreas Keefer
 */
class MigrationScriptReaderTest {
    @Nested
    class readResources {

        @Test
        void fromClassPathJar() throws URISyntaxException, IOException {
            assertThat(readMigrationScripts(
                    "META-INF/maven/org.assertj/assertj-core",
                    ".properties"))
                    .isNotEmpty();
        }

        @Test
        void fromClassPathResourcesDircetory() throws URISyntaxException, IOException {
            assertThat(readMigrationScripts("scriptreader/test", ".http"))
                    .isNotEmpty();
        }
    }

    private List<String> readMigrationScripts(String location, String suffix) throws URISyntaxException, IOException {
        URL url = resolveURL(location);
        System.out.println("url=" + url);
        URI uri = url.toURI();
        System.out.println("uri=" + uri);

        List<String> migrationScripts = processResource(uri, path -> {
            System.out.println("path=" + path);
            return Files
                    .find(path, 10, (p, basicFileAttributes) ->
                            !basicFileAttributes.isDirectory()
                                    && p.toString().endsWith(suffix)
                                    && basicFileAttributes.size() > 0)
                    .map(file -> {
                        String filename = file.getFileName().toString();
                        System.out.println("filename=" + filename);
                        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                            String content = reader.lines()
                                    .collect(Collectors.joining(System.lineSeparator()));
                            System.out.println("content=" + content);
                            return filename + ":\n" + content + "\n";
                        } catch (IOException e) {
                            throw new IllegalStateException("can't read file: " + file.getFileName().toString(), e);
                        }
                    }).collect(Collectors.toList());
        });

        System.out.println("migrationScripts=" + migrationScripts);
        return migrationScripts;
    }

    public static URL resolveURL(String path) {
        ClassLoader classLoader = getDefaultClassLoader();
        if (classLoader != null) {
            return classLoader.getResource(path);
        } else {
            return ClassLoader.getSystemResource(path);
        }
    }

    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = ClassUtils.class.getClassLoader();
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

    @FunctionalInterface
    interface IOFunction<T, R> {
        R accept(T t) throws IOException;
    }

    public static <R> R processResource(URI uri, IOFunction<Path, R> action) throws IOException {
        try {
            Path p = Paths.get(uri);
            return action.accept(p);
        } catch (FileSystemNotFoundException ex) {
            // handle resources in jar files
            try (FileSystem fs = FileSystems.newFileSystem(
                    uri, Collections.emptyMap())) {
                Path p = fs.provider().getPath(uri);
                return action.accept(p);
            }
        }
    }
}