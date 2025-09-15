package com.toulios.reconsiliation.exception;

/**
 * Enum defining all possible error types with their codes, titles, and descriptions.
 * Used by the GlobalExceptionHandler for consistent error responses.
 */
public enum ErrorType {
    
    UNSUPPORTED_API_VERSION(
        "UNSUPPORTED_API_VERSION",
        "Unsupported API Version",
        "unsupported-api-version",
        "The requested API version is not supported by this service"
    ),
    
    CSV_PROCESSING_ERROR(
        "CSV_PROCESSING_ERROR", 
        "CSV Processing Error",
        "csv-processing-error",
        "An error occurred while processing the CSV file"
    ),
    
    INVALID_FILE(
        "INVALID_FILE",
        "Invalid File",
        "invalid-file", 
        "The uploaded file is invalid, empty, or in the wrong format"
    ),
    
    FILE_TOO_LARGE(
        "FILE_TOO_LARGE",
        "File Too Large",
        "file-too-large",
        "The uploaded file exceeds the maximum allowed size"
    ),
    
    RETRY_EXHAUSTED(
        "RETRY_EXHAUSTED",
        "Retry Attempts Exhausted",
        "retry-exhausted",
        "All retry attempts have been exhausted for the requested operation"
    ),
    
    INTERNAL_SERVER_ERROR(
        "INTERNAL_SERVER_ERROR",
        "Internal Server Error", 
        "internal-server-error",
        "An unexpected error occurred on the server"
    );
    
    private final String code;
    private final String title;
    private final String urlFragment;
    private final String description;
    
    ErrorType(String code, String title, String urlFragment, String description) {
        this.code = code;
        this.title = title;
        this.urlFragment = urlFragment;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getUrlFragment() {
        return urlFragment;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Find ErrorType by error code.
     * @param code the error code to search for
     * @return the matching ErrorType, or INTERNAL_SERVER_ERROR if not found
     */
    public static ErrorType fromCode(String code) {
        for (ErrorType errorType : values()) {
            if (errorType.code.equals(code)) {
                return errorType;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
}
