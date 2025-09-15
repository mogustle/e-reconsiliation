package com.toulios.reconsiliation.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.toulios.reconsiliation.exception.CsvProcessingException;

import java.util.Map;

/**
 * Configuration for retry mechanisms in the application.
 * Follows Spring Boot 3 best practices with proper configuration properties integration.
 */
@Configuration
@EnableConfigurationProperties(RetryProperties.class)
@ConditionalOnProperty(prefix = "reconciliation.retry", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RetryConfig {

    private final RetryProperties retryProperties;

    /**
     * Creates a RetryTemplate with configuration from application properties.
     * 
     * @return configured RetryTemplate
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Set retry policy using configuration properties
        retryTemplate.setRetryPolicy(retryPolicy());
        
        // Set backoff policy using configuration properties
        retryTemplate.setBackOffPolicy(backOffPolicy());
        
        return retryTemplate;
    }
    
    /**
     * Defines which exceptions should trigger retries and maximum retry attempts.
     * Uses configuration properties for max attempts.
     * 
     * @return configured retry policy
     */
    @Bean
    public RetryPolicy retryPolicy() {
        // Define which exceptions should trigger retries
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
            CsvProcessingException.class, true,
            RuntimeException.class, true,
            Exception.class, true
        );
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
            retryProperties.getMaxAttempts(), 
            retryableExceptions
        );
        
        return retryPolicy;
    }
    
    /**
     * Defines exponential backoff strategy for retry attempts.
     * Uses configuration properties for timing parameters.
     * 
     * @return configured backoff policy
     */
    @Bean
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryProperties.getInitialInterval());
        backOffPolicy.setMultiplier(retryProperties.getMultiplier());
        backOffPolicy.setMaxInterval(retryProperties.getMaxInterval());
        return backOffPolicy;
    }
}
