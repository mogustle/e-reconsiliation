package com.toulios.reconsiliation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response following RFC 7807 ProblemDetail format.
 * Used for Swagger documentation of error responses.
 */
@Schema(
    name = "ErrorResponse",
    description = "Standard error response following RFC 7807 ProblemDetail format"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Schema(
        description = "URI reference that identifies the problem type",
        example = "http://localhost:8080/swagger-ui.html#/error-handling/unsupported-api-version"
    )
    private String type;

    @Schema(
        description = "Short, human-readable summary of the problem",
        example = "Unsupported API Version"
    )
    private String title;

    @Schema(
        description = "HTTP status code",
        example = "400"
    )
    private int status;

    @Schema(
        description = "Human-readable explanation specific to this occurrence",
        example = "API version '3' is not supported. Supported versions: 1, 2"
    )
    private String detail;

    @Schema(
        description = "URI reference that identifies the specific occurrence of the problem",
        example = "/api/reconciliation"
    )
    private String instance;

    @Schema(
        description = "Application-specific error code",
        example = "UNSUPPORTED_API_VERSION"
    )
    private String errorCode;

    @Schema(
        description = "Detailed description of the error condition",
        example = "The requested API version is not supported by this service"
    )
    private String description;

    @Schema(
        description = "Timestamp when the error occurred",
        example = "2025-09-15T11:08:37Z"
    )
    private Instant timestamp;

    @Schema(
        description = "Additional context-specific properties",
        example = "{\"requestedVersion\": \"3\", \"supportedVersions\": \"1, 2\"}"
    )
    private Map<String, Object> additionalProperties;

    // Constructors
    public ErrorResponse() {}

    public ErrorResponse(String type, String title, int status, String detail) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}
