package com.toulios.reconsiliation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning parameters for reconciliation matching behavior.
 */
@Data
@ConfigurationProperties(prefix = "reconciliation")
public class ReconciliationProperties {

	/**
	 * Allowed date-time difference in seconds for considering two transactions as the same logical event.
	 */
	private long dateWindowSeconds = 300; // 5 minutes

	/** Normalize case of text fields before comparison. */
	private boolean normalizeCase = true;

	/** Collapse multiple whitespace characters to a single space before comparison. */
	private boolean collapseWhitespace = true;

	/** Strip punctuation from text before comparison. */
	private boolean stripPunctuation = false;

	/** Whether to include wallet reference in identity checks. */
	private boolean compareWalletReference = true;

	/** Whether to include transaction type in identity checks. */
	private boolean considerTransactionType = true;
}


