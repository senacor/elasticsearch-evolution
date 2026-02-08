package com.senacor.elasticsearch.evolution.core.api.config;

import com.senacor.elasticsearch.evolution.core.api.migration.java.ClassProvider;
import com.senacor.elasticsearch.evolution.core.api.migration.java.JavaMigration;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * This interface provides a read-only view on the configuration of Elasticsearch Evolution.
 *
 * @see ElasticsearchEvolutionConfigImpl
 */
public interface ElasticsearchEvolutionConfig {

    ElasticsearchEvolutionConfig validate() throws IllegalStateException, NullPointerException;

    boolean isEnabled();

    List<String> getLocations();

    Charset getEncoding();

    String getLineSeparator();

    String getDefaultContentType();

    String getHistoryIndex();

    Map<String, String> getPlaceholders();

    String getPlaceholderPrefix();

    String getPlaceholderSuffix();

    boolean isPlaceholderReplacement();

    String getEsMigrationPrefix();

    List<String> getEsMigrationSuffixes();

    int getHistoryMaxQuerySize();

    boolean isValidateOnMigrate();

    boolean isTrimTrailingNewlineInMigrations();

    String getBaselineVersion();

    boolean isOutOfOrder();

    /**
     * The manually added Java-based migrations. These are not Java-based migrations discovered through classpath
     * scanning and instantiated by Elasticsearch-Evolution. Instead, these are manually added instances of {@link JavaMigration}.
     * This is particularly useful when working with a dependency injection container, where you may want the DI
     * container to instantiate the class and wire up its dependencies for you.
     *
     * @return The manually added Java-based migrations. An empty List if none, never <code>null</code>. (default: none)
     */
    List<JavaMigration> getJavaMigrations();

    /**
     * Retrieves the custom ClassProvider to be used to look up {@link JavaMigration} classes. If not set, the default strategy will be used.
     *
     * @return The custom ClassProvider to be used to look up {@link JavaMigration} classes (default: null)
     */
    ClassProvider<JavaMigration> getJavaMigrationClassProvider();
}
