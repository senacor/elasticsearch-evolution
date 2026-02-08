package com.senacor.elasticsearch.evolution.core.api.migration.java;

import com.senacor.elasticsearch.evolution.rest.abstraction.EvolutionRestClient;

/**
 * Interface to be implemented by Java-based Migrations.
 *
 * <p>Java-based migrations are a great fit for all changes that can't easily be expressed as a single HTTP request.</p>
 *
 * <p>These would typically be things like</p>
 * <ul>
 *     <li>do more than one HTTP request, e.g. in loops</li>
 *     <li>Advanced bulk data changes (Recalculations, advanced format changes, …)</li>
 *     <li>call async APIs like <a href="https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-reindex">reindex</a> (with `wait_for_completion=false`) and wait until it has finished</li>
 * </ul>
 *
 * <p>Migration classes implementing this interface will be
 * automatically discovered when placed in a configured location on the classpath.</p>
 *
 * Implementations must provide a public no-args constructor
 */
public interface JavaMigration {
    /**
     * @return The version and description of the schema or <code>null</code> if the default behavior should be applied to retrieve
     * these information from the class name.
     */
    default JavaMigrationMetadata getMetadata() {
        return null;
    }

    /**
     * @return The checksum of this migration.
     */
    default int getChecksum() {
        return 0;
    }

    /**
     * Executes this migration.
     *
     * @param context The context relevant for this migration, containing things like the {@link EvolutionRestClient}
     *                and the current Elasticsearch-Evolution configuration.
     * @throws Exception when the migration failed.
     */
    void migrate(Context context) throws Exception;
}
