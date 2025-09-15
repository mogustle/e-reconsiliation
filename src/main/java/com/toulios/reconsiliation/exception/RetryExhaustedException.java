package com.toulios.reconsiliation.exception;

import lombok.Getter;

/**
 * Exception thrown when all retry attempts for an operation have been exhausted.
 * This exception provides detailed information about the failed operation and the underlying cause.
 */
@Getter
public class RetryExhaustedException extends ReconciliationException {

    private final String operation;
    private final int maxAttempts;
    private final String originalErrorMessage;

    /**
     * Constructs a new RetryExhaustedException.
     *
     * @param operation the name of the operation that failed
     * @param maxAttempts the maximum number of retry attempts that were made
     * @param originalErrorMessage the original error message that caused the retries
     * @param cause the underlying exception that caused the failure
     */
    public RetryExhaustedException(String operation, int maxAttempts, String originalErrorMessage, Throwable cause) {
        super(
            String.format("Operation '%s' failed after %d retry attempts. Original error: %s", 
                operation, maxAttempts, originalErrorMessage),
            cause
        );
        this.operation = operation;
        this.maxAttempts = maxAttempts;
        this.originalErrorMessage = originalErrorMessage;
    }

    /**
     * Constructs a new RetryExhaustedException with a custom message.
     *
     * @param operation the name of the operation that failed
     * @param maxAttempts the maximum number of retry attempts that were made
     * @param originalErrorMessage the original error message that caused the retries
     * @param customMessage a custom error message
     * @param cause the underlying exception that caused the failure
     */
    public RetryExhaustedException(String operation, int maxAttempts, String originalErrorMessage, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.operation = operation;
        this.maxAttempts = maxAttempts;
        this.originalErrorMessage = originalErrorMessage;
    }

    @Override
    public String getErrorCode() {
        return ErrorType.RETRY_EXHAUSTED.getCode();
    }

    @Override
    public int getHttpStatusCode() {
        return 503; // Service Unavailable - temporary failure
    }
}
