package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * @author Andreas Keefer
 */
public class HistoryRepositoryImpl implements HistoryRepository {

    private final RestHighLevelClient restHighLevelClient;

    public HistoryRepositoryImpl(RestHighLevelClient restHighLevelClient) {
        this.restHighLevelClient = restHighLevelClient;
    }

    @Override
    public TreeSet<MigrationScriptProtocol> findAll() {
        // TODO (ak) impl
        TreeSet<MigrationScriptProtocol> res = new TreeSet<>();
        return res;
    }

    @Override
    public Optional<MigrationScriptProtocol> findLatestSuccessfulVersion() {
        // TODO (ak) impl
        return Optional.empty();
    }

    @Override
    public void saveOrUpdate(MigrationScriptProtocol migrationScriptProtocol) {
        // TODO (ak) impl
    }

    @Override
    public void saveOrUpdate(List<MigrationScriptProtocol> migrationScriptProtocols) {
        // TODO (ak) impl
    }

    @Override
    public boolean isLocked() {
        // TODO (ak) impl
        return false;
    }

    @Override
    public boolean lock() {
        // TODO (ak) impl
        return true;
    }

    @Override
    public boolean unlock() {
        // TODO (ak) impl
        return true;
    }
}
