package com.toulios.reconsiliation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reasons describing why a transaction pair is considered unmatched.
 */
@Schema(
    name = "UnmatchedReason",
    description = "Enumeration of reasons why transactions could not be matched"
)
public enum UnmatchedReason {
    
    @Schema(description = "Transaction exists in one file but not in the other")
    MISSING_IN_OTHER_FILE,
    
    @Schema(description = "Same transaction amount but narrative or wallet reference differs")
    DETAILS_MISMATCH,
    
    @Schema(description = "Transactions have significant differences (amount, date, type, etc.)")
    NOT_IDENTICAL
}


