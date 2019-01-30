package com.senacor.elasticsearch.evolution.core.api.migration;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;

import java.util.NavigableSet;

/**
 * @author Andreas Keefer
 */
public interface HistoryRepository {

    /**
     * @return sorted set by version. The earliest version is the first element and the latest version is the last element.
     * @throws MigrationException in case the operation failed
     */
    NavigableSet<MigrationScriptProtocol> findAll() throws MigrationException;

    /**
     * Put the protocol in the internal Elasticsearch-Evolution history index and use the version as ID.
     *
     * @param migrationScriptProtocol the protocol to save or update
     * @throws MigrationException in case the operation failed
     */
    void saveOrUpdate(MigrationScriptProtocol migrationScriptProtocol) throws MigrationException;

    /**
     * @return true, if the index is locked and Elasticsearch-Evolution has to wait until the lock is released.
     * @throws MigrationException in case the check failed
     */
    boolean isLocked() throws MigrationException;

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

    /**
     * Creates the internal elasticsearch-evolution history index in Elasticsearch is necessary.
     *
     * @return true, if the index was created, false if it's already present in Elasticsearch
     * @throws MigrationException in case the operation failed
     */
    boolean createIndexIfAbsent() throws MigrationException;
}
