package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.api.migration.HistoryRepository;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationService;
import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.ParsedMigrationScript;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Andreas Keefer
 */
public class MigrationServiceImpl implements MigrationService {

    private final HistoryRepository historyRepository;

    public MigrationServiceImpl(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Override
    public List<MigrationScriptProtocol> executePendingScripts(Collection<ParsedMigrationScript> migrationScripts) {
        if (!migrationScripts.isEmpty()) {
            // TODO (ak) impl
        }
        return Collections.emptyList();
    }
}
