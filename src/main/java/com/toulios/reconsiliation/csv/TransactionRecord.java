package com.toulios.reconsiliation.csv;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representation of a single CSV transaction row.
 *
 * <p>Fields are bound by header names with OpenCSV annotations.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "TransactionRecord",
    description = "A single financial transaction record from CSV file"
)
public class TransactionRecord {
	
	@CsvBindByName(column = "ProfileName")
	@Schema(description = "Account or profile identifier", example = "ACME_CORP")
	private String profileName;
	
	@CsvCustomBindByName(column = "TransactionDate", converter = LocalDateTimeConverter.class)
	@Schema(description = "Transaction date and time", example = "2025-09-14T10:15:30")
	private LocalDateTime transactionDate;
	
	@CsvBindByName(column = "TransactionAmount")
	@Schema(description = "Transaction amount (positive or negative)", example = "199.99")
	private BigDecimal transactionAmount;
	
	@CsvBindByName(column = "TransactionNarrative")
	@Schema(description = "Transaction description or memo", example = "Invoice 5567")
	private String transactionNarrative;
	
	@CsvBindByName(column = "TransactionDescription")
	@Schema(description = "Transaction category or type description", example = "INVOICE")
	private String transactionDescription;
	
	@CsvBindByName(column = "TransactionID")
	@Schema(description = "Unique transaction identifier", example = "TX123")
	private String transactionId;
	
	@CsvBindByName(column = "TransactionType")
	@Schema(description = "Integer type code for the transaction", example = "1")
	private Integer transactionType;
	
	@CsvBindByName(column = "WalletReference")
	@Schema(description = "Wallet or account reference", example = "W-001")
	private String walletReference;

	// Record-style getters (for existing code compatibility)
	public String profileName() { return profileName; }
	public LocalDateTime transactionDate() { return transactionDate; }
	public BigDecimal transactionAmount() { return transactionAmount; }
	public String transactionNarrative() { return transactionNarrative; }
	public String transactionDescription() { return transactionDescription; }
	public String transactionId() { return transactionId; }
	public Integer transactionType() { return transactionType; }
	public String walletReference() { return walletReference; }
}


