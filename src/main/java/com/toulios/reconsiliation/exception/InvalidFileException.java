package com.toulios.reconsiliation.exception;

/**
 * Exception thrown when uploaded files are invalid (empty, wrong format, etc.).
 */
public class InvalidFileException extends ReconciliationException {

    private static final String ERROR_CODE = "INVALID_FILE";
    private static final int HTTP_STATUS_CODE = 400; // Bad Request

    public InvalidFileException(String message) {
        super(message);
    }

    public InvalidFileException(String message, Throwable cause) {
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
