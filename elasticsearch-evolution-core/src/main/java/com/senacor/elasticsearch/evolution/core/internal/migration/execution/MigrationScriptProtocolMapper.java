package com.senacor.elasticsearch.evolution.core.internal.migration.execution;

import com.senacor.elasticsearch.evolution.core.internal.model.dbhistory.MigrationScriptProtocol;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/**
 * @author Andreas Keefer
 */
public class MigrationScriptProtocolMapper {

    public static final String LOCKED_FIELD_NAME = "locked";
    public static final String CHECKSUM_FIELD_NAME = "checksum";
    public static final String DESCRIPTION_FIELD_NAME = "description";
    public static final String EXECUTION_RUNTIME_IN_MILLIS_FIELD_NAME = "executionRuntimeInMillis";
    public static final String EXECUTION_TIMESTAMP_FIELD_NAME = "executionTimestamp";
    public static final String SUCCESS_FIELD_NAME = "success";
    public static final String VERSION_FIELD_NAME = "version";
    public static final String INDEX_NAME_FIELD_NAME = "indexName";
    public static final String SCRIPT_NAME_FIELD_NAME = "scriptName";

    public HashMap<String, Object> mapToMap(MigrationScriptProtocol migrationScriptProtocol) {
        HashMap<String, Object> res = new HashMap<>(10);
        res.put(LOCKED_FIELD_NAME, migrationScriptProtocol.isLocked());
        res.put(CHECKSUM_FIELD_NAME, migrationScriptProtocol.getChecksum());
        res.put(DESCRIPTION_FIELD_NAME, migrationScriptProtocol.getDescription());
        res.put(EXECUTION_RUNTIME_IN_MILLIS_FIELD_NAME, migrationScriptProtocol.getExecutionRuntimeInMillis());
        res.put(EXECUTION_TIMESTAMP_FIELD_NAME, null == migrationScriptProtocol.getExecutionTimestamp()
                ? null
                : migrationScriptProtocol.getExecutionTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        res.put(SUCCESS_FIELD_NAME, migrationScriptProtocol.isSuccess());
        res.put(VERSION_FIELD_NAME, null == migrationScriptProtocol.getVersion()
                ? null
                : migrationScriptProtocol.getVersion().getVersion());
        res.put(INDEX_NAME_FIELD_NAME, migrationScriptProtocol.getIndexName());
        res.put(SCRIPT_NAME_FIELD_NAME, migrationScriptProtocol.getScriptName());
        return res;
    }
}
