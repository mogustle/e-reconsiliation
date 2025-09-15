package com.toulios.reconsiliation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Result DTO capturing counts and a list of unmatched transactions.
 */
@Schema(
    name = "ReconciliationResult",
    description = "Result of reconciling two CSV files containing financial transactions"
)
public record ReconciliationResult(
    
    @Schema(
        description = "Number of transactions that matched perfectly between the two files",
        example = "295",
        minimum = "0"
    )
    int matchedCount,
    
    @Schema(
        description = "Total number of transactions that did not match",
        example = "12",
        minimum = "0"
    )
    int unmatchedCount,
    
    @Schema(
        description = "Detailed list of all unmatched transactions with reasons",
        implementation = UnmatchedTransaction.class
    )
    List<UnmatchedTransaction> unmatched
) {}


