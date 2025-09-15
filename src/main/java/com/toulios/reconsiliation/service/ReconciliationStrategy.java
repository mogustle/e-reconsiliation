package com.toulios.reconsiliation.service;

import com.toulios.reconsiliation.dto.ReconciliationResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * Strategy for performing reconciliation for a specific API version.
 */
public interface ReconciliationStrategy {

	String version();

	ReconciliationResult reconcile(MultipartFile file1, MultipartFile file2);
}


