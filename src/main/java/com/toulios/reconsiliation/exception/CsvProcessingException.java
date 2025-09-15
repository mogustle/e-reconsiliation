package com.toulios.reconsiliation.exception;

/**
 * Exception thrown when CSV file processing fails.
 */
public class CsvProcessingException extends ReconciliationException {

    private static final String ERROR_CODE = "CSV_PROCESSING_ERROR";
    private static final int HTTP_STATUS_CODE = 400; // Bad Request

    public CsvProcessingException(String message) {
        super(message);
    }

    public CsvProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    @Override
    public int getHttpStatusCode() {
        return HTTP_STATUS_CODE;
    }
}
