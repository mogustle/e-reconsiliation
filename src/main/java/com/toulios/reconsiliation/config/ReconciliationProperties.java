package com.toulios.reconsiliation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning parameters for reconciliation matching behavior.
 */
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

	public long getDateWindowSeconds() {
		return dateWindowSeconds;
	}

	public void setDateWindowSeconds(long dateWindowSeconds) {
		this.dateWindowSeconds = dateWindowSeconds;
	}

	public boolean isNormalizeCase() {
		return normalizeCase;
	}

	public void setNormalizeCase(boolean normalizeCase) {
		this.normalizeCase = normalizeCase;
	}

	public boolean isCollapseWhitespace() {
		return collapseWhitespace;
	}

	public void setCollapseWhitespace(boolean collapseWhitespace) {
		this.collapseWhitespace = collapseWhitespace;
	}

	public boolean isStripPunctuation() {
		return stripPunctuation;
	}

	public void setStripPunctuation(boolean stripPunctuation) {
		this.stripPunctuation = stripPunctuation;
	}

	public boolean isCompareWalletReference() {
		return compareWalletReference;
	}

	public void setCompareWalletReference(boolean compareWalletReference) {
		this.compareWalletReference = compareWalletReference;
	}

	public boolean isConsiderTransactionType() {
		return considerTransactionType;
	}

	public void setConsiderTransactionType(boolean considerTransactionType) {
		this.considerTransactionType = considerTransactionType;
	}
}


