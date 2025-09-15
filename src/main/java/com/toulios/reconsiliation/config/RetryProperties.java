package com.toulios.reconsiliation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for retry mechanisms.
 */
@Data
@ConfigurationProperties(prefix = "reconciliation.retry")
public class RetryProperties {

    /**
     * Maximum number of retry attempts.
     */
    private int maxAttempts = 3;

    /**
     * Initial delay between retry attempts in milliseconds.
     */
    private long initialInterval = 1000;

    /**
     * Maximum delay between retry attempts in milliseconds.
     */
    private long maxInterval = 10000;

    /**
     * Multiplier for exponential backoff.
     */
    private double multiplier = 2.0;

    /**
     * Whether retry is enabled.
     */
    private boolean enabled = true;
}
