package com.senacor.elasticsearch.evolution.core.api;

import com.senacor.elasticsearch.evolution.core.internal.model.FileNameInfo;
import lombok.NonNull;

import java.util.List;

/**
 * Exception thrown when Elasticsearch Evolution encounters a problem in the validate step.
 */
public class ValidateException extends IllegalStateException {

    public ValidateException(@NonNull List<FileNameInfo> pendingScriptsToBeExecuted) {
        this("There are pending migrations to be executed: " + pendingScriptsToBeExecuted);
    }

    public ValidateException(String message) {
        super(message);
    }

    public ValidateException(String message, Throwable cause) {
        super(message, cause);
    }
}
