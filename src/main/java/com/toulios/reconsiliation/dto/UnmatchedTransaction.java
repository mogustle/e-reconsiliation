package com.toulios.reconsiliation.dto;

import com.toulios.reconsiliation.csv.TransactionRecord;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Details for an unmatched transaction, including both sides when available.
 */
@Schema(
    name = "UnmatchedTransaction",
    description = "Details of a transaction that could not be matched between the two files"
)
public record UnmatchedTransaction(
    
    @Schema(
        description = "Transaction identifier (when available)",
        example = "TX123",
        nullable = true
    )
    String transactionId,
    
    @Schema(
        description = "Transaction record from the first file (null if missing in file1)",
        implementation = TransactionRecord.class,
        nullable = true
    )
    TransactionRecord file1,
    
    @Schema(
        description = "Transaction record from the second file (null if missing in file2)",
        implementation = TransactionRecord.class,
        nullable = true
    )
    TransactionRecord file2,
    
    @Schema(
        description = "Reason why the transaction could not be matched",
        implementation = UnmatchedReason.class
    )
    UnmatchedReason reason
) {}


