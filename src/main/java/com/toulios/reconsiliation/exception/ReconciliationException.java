package com.toulios.reconsiliation.exception;

/**
 * Base runtime exception for all reconciliation-related errors.
 */
public abstract class ReconciliationException extends RuntimeException {

    public ReconciliationException(String message) {
        super(message);
    }

    public ReconciliationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns the error code for this exception type.
     * Used for consistent error response formatting.
     */
    public abstract String getErrorCode();

    /**
     * Returns the HTTP status code that should be returned for this exception.
     */
    public abstract int getHttpStatusCode();
}
