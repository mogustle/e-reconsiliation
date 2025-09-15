package com.toulios.reconsiliation.exception;

/**
 * Exception thrown when an unsupported API version is requested.
 */
public class UnsupportedApiVersionException extends ReconciliationException {

    private static final String ERROR_CODE = "UNSUPPORTED_API_VERSION";
    private static final int HTTP_STATUS_CODE = 400; // Bad Request

    private final String requestedVersion;
    private final String supportedVersions;

    public UnsupportedApiVersionException(String requestedVersion, String supportedVersions) {
        super(String.format("API version '%s' is not supported. Supported versions: %s", 
              requestedVersion, supportedVersions));
        this.requestedVersion = requestedVersion;
        this.supportedVersions = supportedVersions;
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    @Override
    public int getHttpStatusCode() {
        return HTTP_STATUS_CODE;
    }

    public String getRequestedVersion() {
        return requestedVersion;
    }

    public String getSupportedVersions() {
        return supportedVersions;
    }
}
