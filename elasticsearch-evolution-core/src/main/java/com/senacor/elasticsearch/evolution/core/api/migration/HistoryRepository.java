package com.senacor.elasticsearch.evolution.core.api.migration;

import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * @author Andreas Keefer
 */
public interface HistoryRepository {
    /**
     * @return sorted set by version. The earliest version is the first element and the latest version is the last element.
     */
    TreeSet<MigrationScriptProtocol> findAll();

    /**
     * @return the latest historical version which was successfully executed, if present.
     */
    Optional<MigrationScriptProtocol> findLatestSuccessfulVersion();

    /**
     * @param migrationScriptProtocol the protocol to save
     */
    void saveOrUpdate(MigrationScriptProtocol migrationScriptProtocol);

    /**
     * @param migrationScriptProtocols the protocols to save
     */
    void saveOrUpdate(List<MigrationScriptProtocol> migrationScriptProtocols);

    /**
     * @return true, if the index is locked and Elasticsearch-Evolution has to wait until the lock is released.
     */
    boolean isLocked();

    /**
     * This will lock the index for other Elasticsearch-Evolution instances
     *
     * @return true, if the lock was set successfully.
     */
    boolean lock();

    /**
     * This will unlock the index for other Elasticsearch-Evolution instances
     *
     * @return true, if the unlock was successfully.
     */
    boolean unlock();
}
