package com.toulios.reconsiliation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    
    /**
     * Constructor for basic error response.
     */
    public ErrorResponse(String type, String title, int status, String detail) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
    }
}
