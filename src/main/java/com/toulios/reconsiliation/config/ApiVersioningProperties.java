package com.toulios.reconsiliation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration for header-based API versioning.
 */
@Data
@ConfigurationProperties(prefix = "api.versioning")
public class ApiVersioningProperties {

	/** The request header name carrying the API version. */
	private String header = "X-API-Version";

	/** The default version to assume when none is provided. */
	private String defaultVersion = "1";

	/** Supported API versions. If empty, any version is accepted. */
	private List<String> supported = List.of("1");
}


