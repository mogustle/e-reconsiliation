package com.toulios.reconsiliation.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.toulios.reconsiliation.csv.TransactionRecord;
import com.toulios.reconsiliation.config.ReconciliationProperties;
import com.toulios.reconsiliation.config.RetryProperties;
import com.toulios.reconsiliation.dto.ReconciliationResult;
import com.toulios.reconsiliation.dto.UnmatchedReason;
import com.toulios.reconsiliation.dto.UnmatchedTransaction;
import com.toulios.reconsiliation.exception.CsvProcessingException;
import com.toulios.reconsiliation.exception.InvalidFileException;
import com.toulios.reconsiliation.exception.RetryExhaustedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Service responsible for:
 * <ul>
 * 	<li>Parsing uploaded CSV files into {@link TransactionRecord} instances.</li>
 * 	<li>Grouping records by a stable key to reduce the reconciliation search space.</li>
 * 	<li>Reconciling two sets of transactions and summarizing matched and unmatched cases.</li>
 * </ul>
 *
 * <p>The reconciliation algorithm works in two stages:</p>
 * <ol>
 * 	<li><b>Grouping</b>: Each file is parsed and records are bucketed by a grouping key.
 * 	If a record has a non-blank TransactionID it uses the key {@code ID:<id>}.
 * 	Otherwise, a composite key is derived from amount, a time bucket of the date,
 * 	profile name (normalized), and transaction type. The time bucket size is controlled
 * 	by {@link ReconciliationProperties#getDateWindowSeconds()}.</li>
 * 	<li><b>Greedy matching within a key</b>: For each key present in either file, records from the two
 * 	corresponding lists are greedily paired in last-in-first-out order. Pairs are classified as
 * 	IDENTICAL, DETAILS_MISMATCH (same amount but narrative or wallet differs after normalization),
 * 	or NOT_IDENTICAL. Any unpaired records remaining in a list are reported as
 * 	{@link UnmatchedReason#MISSING_IN_OTHER_FILE}.</li>
 * </ol>
 *
 * <p>Text normalization is configurable via {@link ReconciliationProperties} (case folding, whitespace
 * collapsing, and punctuation stripping) and is applied where noted.</p>
 */
@Slf4j
@Service
public class CsvReconciliationService {

	private final ReconciliationProperties properties;
	private final RetryProperties retryProperties;

	public CsvReconciliationService(ReconciliationProperties properties, RetryProperties retryProperties) {
		this.properties = properties;
		this.retryProperties = retryProperties;
	}

	/**
	 * Builds a grouped index of transactions keyed by the best-available grouping key.
	 *
	 * <p>Key selection:</p>
	 * <ul>
	 * 	<li>If {@code TransactionID} is present and non-blank: {@code ID:<TransactionID>}.</li>
	 * 	<li>Else: composite key {@code K|<amount>|<dateBucket>|<normalizedProfile>|<type>} where:
	 * 		<ul>
	 * 			<li><b>amount</b>: BigDecimal amount in plain string without trailing zeros (or {@code null}).</li>
	 * 			<li><b>dateBucket</b>: seconds since epoch divided by {@link ReconciliationProperties#getDateWindowSeconds()}.</li>
	 * 			<li><b>normalizedProfile</b>: profile name after {@link #normalizeText(String)}.</li>
	 * 			<li><b>type</b>: the integer transaction type (or {@code null}).</li>
	 * 		</ul>
	 * 	</li>
	 * </ul>
	 *
	 * <p>Includes retry logic with maximum 3 attempts and exponential backoff for resilient processing.</p>
	 *
	 * @param file multipart CSV file
	 * @return future with map of grouping key to list of records
	 * @throws IllegalArgumentException if parsing fails
	 */
	@Async("csvExecutor")
	@Retryable(
		retryFor = {CsvProcessingException.class, RuntimeException.class},
		maxAttemptsExpression = "${reconciliation.retry.max-attempts:3}",
		backoff = @Backoff(
			delayExpression = "${reconciliation.retry.initial-interval:1000}",
			multiplierExpression = "${reconciliation.retry.multiplier:2.0}",
			maxDelayExpression = "${reconciliation.retry.max-interval:10000}"
		)
	)
	public CompletableFuture<Map<String, List<TransactionRecord>>> groupCsvAsync(MultipartFile file) {
		log.info("Starting CSV parsing for file: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			// Pre-process to remove trailing commas from each line
			String content = reader.lines()
				.map(line -> line.replaceAll(",$", ""))
				.collect(java.util.stream.Collectors.joining("\n"));
			
			log.debug("Pre-processed CSV content, removed trailing commas");
			
			try (StringReader cleanReader = new StringReader(content)) {
				CsvToBean<TransactionRecord> csvToBean = new CsvToBeanBuilder<TransactionRecord>(cleanReader)
					.withType(TransactionRecord.class)
					.withIgnoreLeadingWhiteSpace(true)
					.withIgnoreQuotations(false)
					.withSeparator(',')
					.build();
				// Build a map of grouping key -> list of records sharing that key
				Map<String, List<TransactionRecord>> grouped = new HashMap<>();
				var iterator = csvToBean.iterator();
				while (iterator.hasNext()) {
					TransactionRecord record = iterator.next();
					// Compute a stable grouping key to limit pairwise comparisons during reconciliation
					String key = computeGroupingKey(record);
					grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
				}

				return CompletableFuture.completedFuture(grouped);
			}
		} catch (Exception e) {
			log.error("Failed to parse/group CSV file: {}", file.getOriginalFilename(), e);
			throw new CsvProcessingException("Failed to parse/group CSV: " + file.getOriginalFilename(), e);
		}
	}

	/**
	 * Validates and parses two CSV files concurrently, then reconciles and summarizes results.
	 *
	 * <p>Flow:</p>
	 * <ol>
	 * 	<li>Validate that both files are provided and non-empty.</li>
	 * 	<li>Group each file's records concurrently via {@link #groupCsvAsync(MultipartFile)}.</li>
	 * 	<li>For the union of all grouping keys, greedily pair records from both lists:</li>
	 * 		<ul>
	 * 			<li>If all fields match exactly per {@link #areIdentical(TransactionRecord, TransactionRecord)}, count as matched.</li>
	 * 			<li>Else if only amount matches, but narrative or wallet differ after {@link #normalizeText(String)}: classify as DETAILS_MISMATCH.</li>
	 * 			<li>Else: classify as NOT_IDENTICAL.</li>
	 * 			<li>Any leftovers in either list are reported as MISSING_IN_OTHER_FILE.</li>
	 * 		</ul>
	 * </ol>
	 *
	 * <p>Includes retry logic with maximum 3 attempts and exponential backoff for resilient processing.</p>
	 *
	 * @param file1 first multipart CSV file
	 * @param file2 second multipart CSV file
	 * @return {@link ReconciliationResult} with matched/unmatched counts and details
	 * @throws IllegalArgumentException if either file is missing or empty, or parsing fails
	 */
	@Retryable(
		retryFor = {CsvProcessingException.class, RuntimeException.class},
		maxAttemptsExpression = "${reconciliation.retry.max-attempts:3}",
		backoff = @Backoff(
			delayExpression = "${reconciliation.retry.initial-interval:1000}",
			multiplierExpression = "${reconciliation.retry.multiplier:2.0}",
			maxDelayExpression = "${reconciliation.retry.max-interval:10000}"
		)
	)
	public ReconciliationResult reconsile(MultipartFile file1, MultipartFile file2) {
		if (file1 == null || file1.isEmpty()) {
			log.error("File1 is null or empty");
			throw new InvalidFileException("file1 is required and must not be empty");
		}
		if (file2 == null || file2.isEmpty()) {
			log.error("File2 is null or empty");
			throw new InvalidFileException("file2 is required and must not be empty");
		}
		
		log.info("Starting reconciliation between files: {} and {}", 
			file1.getOriginalFilename(), file2.getOriginalFilename());

		long startTime = System.currentTimeMillis();
		
		CompletableFuture<Map<String, List<TransactionRecord>>> futureGrouped1 = groupCsvAsync(file1);
		CompletableFuture<Map<String, List<TransactionRecord>>> futureGrouped2 = groupCsvAsync(file2);

		// Wait for both grouping operations to complete
		Map<String, List<TransactionRecord>> grouped1 = futureGrouped1.join();
		Map<String, List<TransactionRecord>> grouped2 = futureGrouped2.join();

		Set<String> allKeys = new HashSet<>(grouped1.keySet());
		allKeys.addAll(grouped2.keySet());
		
		log.info("Starting reconciliation");

		int matchedCount = 0;
		int detailsMismatchCount = 0;
		int notIdenticalCount = 0;
		int missingCount = 0;
		List<UnmatchedTransaction> unmatched = new ArrayList<>();

		for (String key : allKeys) {
			List<TransactionRecord> list1 = new ArrayList<>(grouped1.getOrDefault(key, List.of()));
			List<TransactionRecord> list2 = new ArrayList<>(grouped2.getOrDefault(key, List.of()));

			log.debug("Processing key '{}': {} records in file1, {} records in file2", 
				key, list1.size(), list2.size());

			/*
			 * GREEDY PAIR MATCHING ALGORITHM:
			 * 
			 * This is the core reconciliation logic that pairs transactions from both files
			 * within the same grouping key. We use a greedy approach for simplicity and performance.
			 * 
			 * Algorithm: LIFO (Last-In-First-Out) pairing
			 * - Take the last record from each list (most recently added to group)
			 * - Compare them using a hierarchy of matching criteria
			 * - Remove both records from their respective lists once paired
			 * - Continue until one or both lists are empty
			 * 
			 * Why LIFO? It's simple, fast (O(1) removal), and works well when records
			 * are naturally ordered (e.g., by time).
			 */
			while (!list1.isEmpty() && !list2.isEmpty()) {
				// Remove last record from each list (LIFO - Last In First Out)
				TransactionRecord r1 = list1.remove(list1.size() - 1);
				TransactionRecord r2 = list2.remove(list2.size() - 1);
				
				/*
				 * MATCHING CRITERIA HIERARCHY (checked in order):
				 * 
				 * 1. IDENTICAL: All fields match exactly
				 *    - ProfileName, TransactionDate, TransactionAmount, etc.
				 *    - This is a perfect match - count as reconciled
				 */
				if (areIdentical(r1, r2)) {
					matchedCount++;
					// Perfect match - no further action needed
					
				/*
				 * 2. DETAILS_MISMATCH: Same amount but narrative/wallet differ
				 *    - TransactionAmount matches exactly
				 *    - BUT TransactionNarrative or WalletReference differ (after normalization)
				 *    - This suggests same transaction with different descriptions/references
				 *    - Common in real-world scenarios (typos, different systems, etc.)
				 */
				} else if (isAmountSame(r1, r2) && 
						(!Objects.equals(normalizeText(r1.transactionNarrative()), normalizeText(r2.transactionNarrative()))
						|| !Objects.equals(normalizeText(r1.walletReference()), normalizeText(r2.walletReference())))) {
					
					unmatched.add(new UnmatchedTransaction(coalesceId(r1, r2), r1, r2, UnmatchedReason.DETAILS_MISMATCH));
					detailsMismatchCount++;
					
				/*
				 * 3. NOT_IDENTICAL: Everything else that doesn't match
				 *    - Different amounts, dates, types, etc.
				 *    - Could be completely different transactions that happened to be grouped together
				 *    - Or transactions with significant differences (amount, timing, etc.)
				 */
				} else {
					unmatched.add(new UnmatchedTransaction(coalesceId(r1, r2), r1, r2, UnmatchedReason.NOT_IDENTICAL));
					notIdenticalCount++;
				}
			}

			/*
			 * HANDLE REMAINING UNPAIRED RECORDS:
			 * 
			 * After the greedy pairing above, we may have leftover records in either list.
			 * These represent transactions that exist in one file but have no counterpart
			 * in the other file (within the same grouping key).
			 * 
			 * Scenarios:
			 * - File1 has 3 records, File2 has 1 record → 2 records left in list1
			 * - File1 has 1 record, File2 has 4 records → 3 records left in list2
			 * - One file has records for a grouping key, the other file has none
			 * 
			 * All remaining records are classified as MISSING_IN_OTHER_FILE since they
			 * couldn't be paired with anything from the opposite file.
			 */
			
			// Records remaining in file1's list → missing counterparts in file2
			for (TransactionRecord r1 : list1) {
				unmatched.add(new UnmatchedTransaction(coalesceId(r1, null), r1, null, UnmatchedReason.MISSING_IN_OTHER_FILE));
				missingCount++;
			}
			
			// Records remaining in file2's list → missing counterparts in file1  
			for (TransactionRecord r2 : list2) {
				unmatched.add(new UnmatchedTransaction(coalesceId(null, r2), null, r2, UnmatchedReason.MISSING_IN_OTHER_FILE));
				missingCount++;
			}
		}

		long duration = System.currentTimeMillis() - startTime;
		log.info("Reconciliation completed in {}ms. Results: {} matched, {} unmatched " +
			"(details mismatch: {}, not identical: {}, missing: {})", 
			duration, matchedCount, unmatched.size(), detailsMismatchCount, notIdenticalCount, missingCount);

		return new ReconciliationResult(matchedCount, unmatched.size(), unmatched);
	}

	/**
	 * Returns {@code true} if two records are identical across all fields considered.
	 *
	 * <p>Amount comparison uses {@link BigDecimal#compareTo(BigDecimal)} so that values like
	 * {@code 10.0} and {@code 10.00} are treated as equal.</p>
	 */
	private static boolean areIdentical(TransactionRecord a, TransactionRecord b) {
		return Objects.equals(a.profileName(), b.profileName())
				&& Objects.equals(a.transactionDate(), b.transactionDate())
				&& isBigDecimalIdentical(a.transactionAmount(), b.transactionAmount())
				&& Objects.equals(a.transactionNarrative(), b.transactionNarrative())
				&& Objects.equals(a.transactionDescription(), b.transactionDescription())
				&& Objects.equals(a.transactionId(), b.transactionId())
				&& Objects.equals(a.transactionType(), b.transactionType())
				&& Objects.equals(a.walletReference(), b.walletReference());
	}

	/**
	 * Compares only the monetary amount using BigDecimal numeric equality.
	 */
	private static boolean isAmountSame(TransactionRecord a, TransactionRecord b) {
		return isBigDecimalIdentical(a.transactionAmount(), b.transactionAmount());
	}

	/**
	 * BigDecimal equality based on numeric value via {@code compareTo}.
	 */
	private static boolean isBigDecimalIdentical(BigDecimal a, BigDecimal b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		return a.compareTo(b) == 0;
	}

	/**
	 * Computes a grouping key for a transaction record. Prefer direct ID-based grouping when
	 * {@code TransactionID} is provided; otherwise fall back to a composite key to approximate
	 * logical equivalence (amount + time bucket + normalized profile + type).
	 */
	private String computeGroupingKey(TransactionRecord r) {
		String id = r.transactionId();
		if (id != null && !id.isBlank()) {
			return "ID:" + id;
		}
		String amt = r.transactionAmount() == null ? "null" : r.transactionAmount().stripTrailingZeros().toPlainString();
		String bucket = r.transactionDate() == null ? "null" : Long.toString(dateBucketSeconds(r));
		String profile = normalizeText(r.profileName());
		String type = r.transactionType() == null ? "null" : r.transactionType().toString();
		return String.join("|", "K", amt, bucket, profile, type);
	}

	/**
	 * Returns the date bucket identifier in seconds for the record based on the configured window.
	 *
	 * <p>For example, with a 300-second window (5 minutes), two timestamps within the same 5-minute
	 * interval will share the same bucket value.</p>
	 */
	private long dateBucketSeconds(TransactionRecord r) {
		long epoch = r.transactionDate().atZone(ZoneId.systemDefault()).toEpochSecond();
		long window = Math.max(1L, properties.getDateWindowSeconds());
		return epoch / window;
	}

	/**
	 * Applies configured normalization to text fields:
	 * <ul>
	 * 	<li><b>Case folding</b>: lowercases the string if enabled.</li>
	 * 	<li><b>Whitespace collapsing</b>: trims and collapses runs of whitespace into a single space.</li>
	 * 	<li><b>Punctuation stripping</b>: removes punctuation characters if enabled.</li>
	 * </ul>
	 */
	private String normalizeText(String s) {
		if (s == null) return null;
		String out = s;
		if (properties.isNormalizeCase()) {
			out = out.toLowerCase(Locale.ROOT);
		}
		if (properties.isCollapseWhitespace()) {
			out = out.trim().replaceAll("\\s+", " ");
		}
		if (properties.isStripPunctuation()) {
			out = out.replaceAll("\\p{Punct}+", "");
		}
		return out;
	}

	/**
	 * Chooses a representative transaction ID from either record, preferring the first non-blank.
	 * Returns {@code null} if neither record has a usable ID.
	 */
	private String coalesceId(TransactionRecord a, TransactionRecord b) {
		String idA = a != null ? a.transactionId() : null;
		String idB = b != null ? b.transactionId() : null;
		if (idA != null && !idA.isBlank()) return idA;
		if (idB != null && !idB.isBlank()) return idB;
		return null;
	}

	/**
	 * Recovery method for groupCsvAsync when all retry attempts are exhausted.
	 * 
	 * @param ex the exception that caused the retries to fail
	 * @param file the file that failed to process
	 * @throws RetryExhaustedException always thrown to indicate retry failure
	 */
	@Recover
	public CompletableFuture<Map<String, List<TransactionRecord>>> recoverGroupCsvAsync(
			Exception ex, MultipartFile file) {
		String operation = "CSV parsing and grouping";
		String fileName = file != null ? file.getOriginalFilename() : "unknown";
		
		log.error("All retry attempts exhausted for CSV grouping of file: {}. Error: {}", 
				fileName, ex.getMessage(), ex);
		
		throw new RetryExhaustedException(
			operation,
			retryProperties.getMaxAttempts(),
			ex.getMessage(),
			String.format("Failed to process CSV file '%s' after %d retry attempts. Please check the file format and try again.", 
				fileName, retryProperties.getMaxAttempts()),
			ex
		);
	}

	/**
	 * Recovery method for reconsile when all retry attempts are exhausted.
	 * 
	 * @param ex the exception that caused the retries to fail
	 * @param file1 the first file
	 * @param file2 the second file
	 * @throws RetryExhaustedException always thrown to indicate retry failure
	 */
	@Recover
	public ReconciliationResult recoverReconsile(Exception ex, MultipartFile file1, MultipartFile file2) {
		String operation = "file reconciliation";
		String fileName1 = file1 != null ? file1.getOriginalFilename() : "unknown";
		String fileName2 = file2 != null ? file2.getOriginalFilename() : "unknown";
		
		log.error("All retry attempts exhausted for reconciliation of files: {} and {}. Error: {}", 
				fileName1, fileName2, ex.getMessage(), ex);
		
		throw new RetryExhaustedException(
			operation,
			retryProperties.getMaxAttempts(),
			ex.getMessage(),
			String.format("Failed to reconcile files '%s' and '%s' after %d retry attempts. Please check the files and try again.", 
				fileName1, fileName2, retryProperties.getMaxAttempts()),
			ex
		);
	}
}


