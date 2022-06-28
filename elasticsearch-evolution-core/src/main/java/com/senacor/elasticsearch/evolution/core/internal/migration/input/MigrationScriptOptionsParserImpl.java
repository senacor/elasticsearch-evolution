package com.senacor.elasticsearch.evolution.core.internal.migration.input;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import com.senacor.elasticsearch.evolution.core.api.migration.MigrationScriptOptionsParser;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.MigrationScriptExecuteOptions;
import com.senacor.elasticsearch.evolution.core.internal.model.migration.RawMigrationScript;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class MigrationScriptOptionsParserImpl implements MigrationScriptOptionsParser {

    private final Map<String, String> placeholders;
    private final String placeholderPrefix;
    private final String placeholderSuffix;
    private final boolean placeholderReplacement;

    private final String scriptExecuteOptionsFileName;

    public MigrationScriptOptionsParserImpl(Map<String, String> placeholders,
                                     String placeholderPrefix,
                                     String placeholderSuffix,
                                     boolean placeholderReplacement,
                                     String scriptExecuteOptionsFileName) {
        this.placeholders = placeholders;
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
        this.placeholderReplacement = placeholderReplacement;
        this.scriptExecuteOptionsFileName = scriptExecuteOptionsFileName;
    }
    @Override
    public Map<String, MigrationScriptExecuteOptions> parse(Collection<RawMigrationScript> rawMigrationScripts) {
        requireNonNull(rawMigrationScripts, "rawMigrationScripts must not be null");
        int filesCount = rawMigrationScripts.size();
        // expecting exactly one script options file
        if (filesCount == 0) {
            return Collections.emptyMap();
        }
        if (rawMigrationScripts.size() > 1) {
            throw new MigrationException("found more than one script options file: " + rawMigrationScripts.stream().map(RawMigrationScript::getFileName).collect(Collectors.toList()) );
        }
        RawMigrationScript scriptExecuteOptionsFile = rawMigrationScripts.stream().findFirst().get();
        if (!scriptExecuteOptionsFileName.equals(scriptExecuteOptionsFile.getFileName())) {
            throw new MigrationException("Could not find script options file: " + scriptExecuteOptionsFileName );
        }
        return parse(scriptExecuteOptionsFile).stream()
                .collect(Collectors.toMap(
                        s -> s.getFileName(),
                        s -> s,
                        (oldVal, newVal) -> {
                            if (Objects.equals(oldVal.getFileName(), newVal.getFileName())) {
                                throw new MigrationException("duplicate options for the same file: " + oldVal.getFileName());
                            } else {
                                return newVal;
                            }
                        }));
    }

    List<MigrationScriptExecuteOptions> parse(RawMigrationScript rawMigrationScript) {
        ObjectMapper objectMapper = new ObjectMapper();
        MigrationScriptExecuteOptionsConfiguration migrationScriptExecuteOptionsConfiguration;
        try {
            migrationScriptExecuteOptionsConfiguration = objectMapper.readValue(rawMigrationScript.getContent(), MigrationScriptExecuteOptionsConfiguration.class);
        } catch (JsonProcessingException e) {
            throw new MigrationException("failed parsing content of " + rawMigrationScript.getFileName(), e);
        }
        return migrationScriptExecuteOptionsConfiguration.getMigrationScriptsOptions();
    }

}
