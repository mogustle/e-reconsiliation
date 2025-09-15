package com.toulios.reconsiliation.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RetryExhaustedException.
 */
class RetryExhaustedExceptionTest {

    @Test
    void constructor_shouldSetAllProperties_withBasicConstructor() {
        // Given
        String operation = "CSV parsing";
        int maxAttempts = 3;
        String originalError = "File not found";
        RuntimeException cause = new RuntimeException("Original cause");
        
        // When
        RetryExhaustedException exception = new RetryExhaustedException(
            operation, maxAttempts, originalError, cause);
        
        // Then
        assertThat(exception.getOperation()).isEqualTo(operation);
        assertThat(exception.getMaxAttempts()).isEqualTo(maxAttempts);
        assertThat(exception.getOriginalErrorMessage()).isEqualTo(originalError);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getErrorCode()).isEqualTo("RETRY_EXHAUSTED");
        assertThat(exception.getHttpStatusCode()).isEqualTo(503);
        assertThat(exception.getMessage()).contains(operation);
        assertThat(exception.getMessage()).contains(String.valueOf(maxAttempts));
        assertThat(exception.getMessage()).contains(originalError);
    }

    @Test
    void constructor_shouldSetAllProperties_withCustomMessageConstructor() {
        // Given
        String operation = "file reconciliation";
        int maxAttempts = 5;
        String originalError = "Connection timeout";
        String customMessage = "Custom failure message";
        RuntimeException cause = new RuntimeException("Network error");
        
        // When
        RetryExhaustedException exception = new RetryExhaustedException(
            operation, maxAttempts, originalError, customMessage, cause);
        
        // Then
        assertThat(exception.getOperation()).isEqualTo(operation);
        assertThat(exception.getMaxAttempts()).isEqualTo(maxAttempts);
        assertThat(exception.getOriginalErrorMessage()).isEqualTo(originalError);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getErrorCode()).isEqualTo("RETRY_EXHAUSTED");
        assertThat(exception.getHttpStatusCode()).isEqualTo(503);
        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }

    @Test
    void errorType_shouldHaveCorrectProperties() {
        // When
        ErrorType errorType = ErrorType.RETRY_EXHAUSTED;
        
        // Then
        assertThat(errorType.getCode()).isEqualTo("RETRY_EXHAUSTED");
        assertThat(errorType.getTitle()).isEqualTo("Retry Attempts Exhausted");
        assertThat(errorType.getUrlFragment()).isEqualTo("retry-exhausted");
        assertThat(errorType.getDescription()).isEqualTo("All retry attempts have been exhausted for the requested operation");
    }

    @Test
    void fromCode_shouldReturnCorrectErrorType() {
        // When
        ErrorType errorType = ErrorType.fromCode("RETRY_EXHAUSTED");
        
        // Then
        assertThat(errorType).isEqualTo(ErrorType.RETRY_EXHAUSTED);
    }
}
