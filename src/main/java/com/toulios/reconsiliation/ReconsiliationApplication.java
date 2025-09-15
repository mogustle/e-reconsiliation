package com.toulios.reconsiliation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.toulios.reconsiliation.config.ApiVersioningProperties;
import com.toulios.reconsiliation.config.ReconciliationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ReconciliationProperties.class, ApiVersioningProperties.class})
public class ReconsiliationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReconsiliationApplication.class, args);
	}

}
