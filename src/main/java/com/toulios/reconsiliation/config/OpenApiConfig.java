package com.toulios.reconsiliation.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration supplying application-level metadata for Swagger UI and docs.
 */
@Configuration
public class OpenApiConfig {

	/**
	 * Builds the {@link OpenAPI} model containing API information rendered by Swagger UI.
	 *
	 * @return configured OpenAPI instance
	 */
	@Bean
	public OpenAPI reconsiliationOpenAPI() {
		return new OpenAPI()
			.info(new Info()
					.title("Reconciliation API")
					.description("API to upload two CSV files and get a reconciliation summary")
					.version("v1")
					.contact(new Contact().name("Reconciliation Service").email("noreply@example.com"))
					.license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
			.externalDocs(new ExternalDocumentation()
					.description("OpenCSV")
					.url("https://opencsv.sourceforge.net/"));
	}
}


