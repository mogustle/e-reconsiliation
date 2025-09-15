package com.toulios.reconsiliation.web;

import com.toulios.reconsiliation.service.ReconciliationStrategyResolver;
import com.toulios.reconsiliation.config.ApiVersioningProperties;
import com.toulios.reconsiliation.dto.ReconciliationResult;
import com.toulios.reconsiliation.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


/**
 * REST controller exposing the reconciliation endpoint for CSV uploads.
 */
@RestController
@RequestMapping(path = "/api/reconciliation")
@Validated
@Tag(name = "Reconciliation", description = "Upload two CSV files and get a reconciliation summary")
public class ReconciliationController {

	private final ReconciliationStrategyResolver strategyResolver;
	private final ApiVersioningProperties versioningProperties;

	public ReconciliationController(ReconciliationStrategyResolver strategyResolver, ApiVersioningProperties versioningProperties) {
		this.strategyResolver = strategyResolver;
		this.versioningProperties = versioningProperties;
	}

	@Operation(
		summary = "Reconcile two CSV files", 
		description = "Uploads two CSV files containing financial transactions and performs reconciliation analysis. " +
					  "Returns detailed results including matched transactions count and list of unmatched transactions with reasons."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200", 
			description = "Reconciliation completed successfully",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = ReconciliationResult.class)
			)
		),
		@ApiResponse(
			responseCode = "400", 
			description = "Bad Request - Invalid input data",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = ErrorResponse.class)
			)
		),
		@ApiResponse(
			responseCode = "413", 
			description = "Payload Too Large - File size exceeds limit",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = ErrorResponse.class)
			)
		),
		@ApiResponse(
			responseCode = "500", 
			description = "Internal Server Error",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = ErrorResponse.class)
			)
		)
	})
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ReconciliationResult> upload(
		@Parameter(
			description = "API version to use for reconciliation. Determines the reconciliation strategy and response format.",
			example = "1",
			schema = @Schema(allowableValues = {"1", "2"})
		)
		@RequestHeader(name = "X-API-Version", required = false) String apiVersion,
		
		@Parameter(
			description = "First CSV file containing transaction records. Must include required headers: ProfileName, TransactionDate, TransactionAmount, etc.",
			required = true,
			content = @Content(mediaType = "text/csv")
		)
		@RequestParam("file1") MultipartFile file1,
		
		@Parameter(
			description = "Second CSV file containing transaction records to reconcile against file1. Must have the same format as file1.",
			required = true,
			content = @Content(mediaType = "text/csv")
		)
		@RequestParam("file2") MultipartFile file2
	) {
		var strategy = strategyResolver.resolve(apiVersion, versioningProperties.getDefaultVersion());
		ReconciliationResult result = strategy.reconcile(file1, file2);
		return ResponseEntity.ok(result);
	}
}


