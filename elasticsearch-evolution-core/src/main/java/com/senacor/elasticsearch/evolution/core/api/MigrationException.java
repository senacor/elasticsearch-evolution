package com.senacor.elasticsearch.evolution.core.api;

/**
 * Exception thrown when ES Evolution encounters a problem.
 *
 * @author Andreas Keefer
 */
public class MigrationException extends RuntimeException {

    public MigrationException(String message) {
        super(message);
    }

    public MigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
