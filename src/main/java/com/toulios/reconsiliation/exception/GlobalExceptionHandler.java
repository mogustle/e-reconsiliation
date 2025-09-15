package com.toulios.reconsiliation.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;

/**
 * Global exception handler for all REST controllers.
 * Uses Spring Boot 3's ProblemDetail (RFC 7807) for consistent error responses.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Value("${server.servlet.context-path:}")
    private String contextPath;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    /**
     * Constructs the base URL for problem documentation.
     * Points to the Swagger UI documentation for error handling.
     */
    private String getProblemBaseUrl() {
        String baseUrl = "http://localhost:" + serverPort;
        if (!contextPath.isEmpty()) {
            baseUrl += contextPath;
        }
        return baseUrl + "/swagger-ui.html#/error-handling/";
    }

    /**
     * Handle retry exhausted exceptions with detailed information.
     */
    @ExceptionHandler(RetryExhaustedException.class)
    public ResponseEntity<ProblemDetail> handleRetryExhaustedException(
            RetryExhaustedException ex, WebRequest request) {
        
        log.error("Retry exhausted exception occurred: {}", ex.getMessage(), ex);
        
        ErrorType errorType = ErrorType.fromCode(ex.getErrorCode());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(ex.getHttpStatusCode()), 
                ex.getMessage()
        );
        
        problemDetail.setType(URI.create(getProblemBaseUrl() + errorType.getUrlFragment()));
        problemDetail.setTitle(errorType.getTitle());
        problemDetail.setProperty("errorCode", errorType.getCode());
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("description", errorType.getDescription());
        
        // Add specific properties for RetryExhaustedException
        problemDetail.setProperty("operation", ex.getOperation());
        problemDetail.setProperty("maxAttempts", ex.getMaxAttempts());
        problemDetail.setProperty("originalError", ex.getOriginalErrorMessage());
        problemDetail.setProperty("retryAdvice", "Please check your input files and try again. If the problem persists, contact support.");
        
        return ResponseEntity.status(ex.getHttpStatusCode()).body(problemDetail);
    }

    /**
     * Handle all other custom reconciliation exceptions.
     */
    @ExceptionHandler(ReconciliationException.class)
    public ResponseEntity<ProblemDetail> handleReconciliationException(
            ReconciliationException ex, WebRequest request) {
        
        log.warn("Reconciliation exception occurred: {}", ex.getMessage(), ex);
        
        ErrorType errorType = ErrorType.fromCode(ex.getErrorCode());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(ex.getHttpStatusCode()), 
                ex.getMessage()
        );
        
        problemDetail.setType(URI.create(getProblemBaseUrl() + errorType.getUrlFragment()));
        problemDetail.setTitle(errorType.getTitle());
        problemDetail.setProperty("errorCode", errorType.getCode());
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("description", errorType.getDescription());
        
        // Add specific properties for UnsupportedApiVersionException
        if (ex instanceof UnsupportedApiVersionException versionEx) {
            problemDetail.setProperty("requestedVersion", versionEx.getRequestedVersion());
            problemDetail.setProperty("supportedVersions", versionEx.getSupportedVersions());
        }
        
        return ResponseEntity.status(ex.getHttpStatusCode()).body(problemDetail);
    }


    /**
     * Handle all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected exception occurred", ex);
        
        ErrorType errorType = ErrorType.INTERNAL_SERVER_ERROR;
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
        
        problemDetail.setType(URI.create(getProblemBaseUrl() + errorType.getUrlFragment()));
        problemDetail.setTitle(errorType.getTitle());
        problemDetail.setProperty("errorCode", errorType.getCode());
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("description", errorType.getDescription());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}
