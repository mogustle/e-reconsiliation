package com.toulios.reconsiliation.service;

import com.toulios.reconsiliation.dto.ReconciliationResult;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * API v1 strategy delegating to the existing CsvReconciliationService.
 */
@Component
public class ReconciliationStrategyV1 implements ReconciliationStrategy {

	private final CsvReconciliationService csvService;

	public ReconciliationStrategyV1(CsvReconciliationService csvService) {
		this.csvService = csvService;
	}

	@Override
	public String version() {
		return "1";
	}

	@Override
	public ReconciliationResult reconcile(MultipartFile file1, MultipartFile file2) {
		return csvService.reconsile(file1, file2);
	}
}


