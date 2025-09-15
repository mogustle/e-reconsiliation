package com.toulios.reconsiliation.service;

import com.toulios.reconsiliation.config.ApiVersioningProperties;
import com.toulios.reconsiliation.exception.UnsupportedApiVersionException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves a reconciliation strategy for a given API version.
 */
@Component
public class ReconciliationStrategyResolver {

	private final Map<String, ReconciliationStrategy> versionToStrategy;
	private final ApiVersioningProperties versioningProperties;

	public ReconciliationStrategyResolver(List<ReconciliationStrategy> strategies, 
			ApiVersioningProperties versioningProperties) {
		this.versionToStrategy = strategies.stream().collect(Collectors.toUnmodifiableMap(
				ReconciliationStrategy::version,
				Function.identity()
		));
		this.versioningProperties = versioningProperties;
	}

	public ReconciliationStrategy resolve(String version, String defaultVersion) {
		String requestedVersion = (version == null || version.isBlank()) ? defaultVersion : version;
		
		// Validate that the requested version is supported
		if (!versioningProperties.getSupported().isEmpty() && 
			!versioningProperties.getSupported().contains(requestedVersion)) {
			throw new UnsupportedApiVersionException(
				requestedVersion, 
				String.join(", ", versioningProperties.getSupported())
			);
		}
		
		// Get the strategy for the requested version
		ReconciliationStrategy strategy = versionToStrategy.get(requestedVersion);
		if (strategy == null) {
			// If no strategy found for requested version, try default
			strategy = versionToStrategy.get(defaultVersion);
			if (strategy == null) {
				throw new UnsupportedApiVersionException(
					requestedVersion, 
					String.join(", ", versionToStrategy.keySet())
				);
			}
		}
		
		return strategy;
	}
}


