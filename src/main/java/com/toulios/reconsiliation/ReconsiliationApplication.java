package com.toulios.reconsiliation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;

import com.toulios.reconsiliation.config.ApiVersioningProperties;
import com.toulios.reconsiliation.config.ReconciliationProperties;
import com.toulios.reconsiliation.config.RetryProperties;

@SpringBootApplication
@EnableConfigurationProperties({ReconciliationProperties.class, ApiVersioningProperties.class, RetryProperties.class})
@EnableRetry
public class ReconsiliationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReconsiliationApplication.class, args);
	}

}
