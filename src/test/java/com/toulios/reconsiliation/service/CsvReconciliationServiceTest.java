package com.toulios.reconsiliation.service;

import com.toulios.reconsiliation.config.ReconciliationProperties;
import com.toulios.reconsiliation.config.RetryProperties;
import com.toulios.reconsiliation.csv.TransactionRecord;
import com.toulios.reconsiliation.dto.ReconciliationResult;
import com.toulios.reconsiliation.dto.UnmatchedReason;
import com.toulios.reconsiliation.dto.UnmatchedTransaction;
import com.toulios.reconsiliation.exception.InvalidFileException;
import com.toulios.reconsiliation.exception.CsvProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link CsvReconciliationService}.
 * 
 * Tests cover the main public methods:
 * - groupCsvAsync: CSV parsing and grouping functionality
 * - reconsile: Full reconciliation workflow with various matching scenarios
 */
@ExtendWith(MockitoExtension.class)
class CsvReconciliationServiceTest {

    private CsvReconciliationService service;
    private ReconciliationProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ReconciliationProperties();
        properties.setDateWindowSeconds(300); // 5 minutes
        properties.setNormalizeCase(true);
        properties.setCollapseWhitespace(true);
        properties.setStripPunctuation(false);
        
        RetryProperties retryProperties = new RetryProperties();
        service = new CsvReconciliationService(properties, retryProperties);
    }

    // ========== groupCsvAsync Tests ==========

    @Test
    void groupCsvAsync_shouldParseValidCsv_andGroupByTransactionId() throws Exception {
        // Given
        String csvContent = """
            ProfileName,TransactionDate,TransactionAmount,TransactionNarrative,TransactionDescription,TransactionID,TransactionType,WalletReference
            John Doe,2023-01-01 10:00:00,100.50,Payment for services,Service payment,TXN001,1,WALLET123
            Jane Smith,2023-01-01 10:05:00,250.00,Grocery shopping,Food purchase,TXN002,2,WALLET456
            """;
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CompletableFuture<Map<String, List<TransactionRecord>>> future = service.groupCsvAsync(file);
        Map<String, List<TransactionRecord>> result = future.get();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("ID:TXN001", "ID:TXN002");
        
        List<TransactionRecord> group1 = result.get("ID:TXN001");
        assertThat(group1).hasSize(1);
        assertThat(group1.get(0).transactionId()).isEqualTo("TXN001");
        assertThat(group1.get(0).profileName()).isEqualTo("John Doe");
        assertThat(group1.get(0).transactionAmount()).isEqualByComparingTo(new BigDecimal("100.50"));

        List<TransactionRecord> group2 = result.get("ID:TXN002");
        assertThat(group2).hasSize(1);
        assertThat(group2.get(0).transactionId()).isEqualTo("TXN002");
        assertThat(group2.get(0).profileName()).isEqualTo("Jane Smith");
    }

    @Test
    void groupCsvAsync_shouldGroupByCompositeKey_whenNoTransactionId() throws Exception {
        // Given
        String csvContent = """
            ProfileName,TransactionDate,TransactionAmount,TransactionNarrative,TransactionDescription,TransactionID,TransactionType,WalletReference
            John Doe,2023-01-01 10:00:00,100.50,Payment for services,Service payment,,1,WALLET123
            Jane Smith,2023-01-01 10:05:00,100.50,Different narrative,Different desc,,1,WALLET456
            """;
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CompletableFuture<Map<String, List<TransactionRecord>>> future = service.groupCsvAsync(file);
        Map<String, List<TransactionRecord>> result = future.get();

        // Then
        assertThat(result).hasSize(2); // Different composite keys due to different profiles
        
        // Both should have composite keys starting with "K|100.5|" (amount part)
        assertThat(result.keySet()).allMatch(key -> key.startsWith("K|100.5|"));
    }

    @Test
    void groupCsvAsync_shouldHandleTrailingCommas() throws Exception {
        // Given
        String csvContent = """
            ProfileName,TransactionDate,TransactionAmount,TransactionNarrative,TransactionDescription,TransactionID,TransactionType,WalletReference,
            John Doe,2023-01-01 10:00:00,100.50,Payment for services,Service payment,TXN001,1,WALLET123,
            """;
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CompletableFuture<Map<String, List<TransactionRecord>>> future = service.groupCsvAsync(file);
        Map<String, List<TransactionRecord>> result = future.get();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsKey("ID:TXN001");
    }

    @Test
    void groupCsvAsync_shouldThrowException_whenInvalidCsv() {
        // Given - CSV with proper headers but invalid date format
        String invalidCsvContent = """
            ProfileName,TransactionDate,TransactionAmount,TransactionNarrative,TransactionDescription,TransactionID,TransactionType,WalletReference
            John Doe,invalid-date-format,100.50,Payment,Description,TXN001,1,WALLET123
            """;
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", invalidCsvContent.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> {
            CompletableFuture<Map<String, List<TransactionRecord>>> future = service.groupCsvAsync(file);
            future.get(); // This will throw the exception
        }).hasRootCauseInstanceOf(java.time.format.DateTimeParseException.class);
    }

    @Test
    void groupCsvAsync_shouldHandleEmptyFile() throws Exception {
        // Given
        String emptyCsvContent = "ProfileName,TransactionDate,TransactionAmount,TransactionNarrative,TransactionDescription,TransactionID,TransactionType,WalletReference\n";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", emptyCsvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CompletableFuture<Map<String, List<TransactionRecord>>> future = service.groupCsvAsync(file);
        Map<String, List<TransactionRecord>> result = future.get();

        // Then
        assertThat(result).isEmpty();
    }

    // ========== reconsile Tests ==========

    @Test
    void reconsile_shouldThrowException_whenFile1IsNull() {
        // Given
        MockMultipartFile file2 = createValidCsvFile("file2.csv", "TXN001");

        // When & Then
        assertThatThrownBy(() -> service.reconsile(null, file2))
            .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void reconsile_shouldThrowException_whenFile2IsNull() {
        // Given
        MockMultipartFile file1 = createValidCsvFile("file1.csv", "TXN001");

        // When & Then
        assertThatThrownBy(() -> service.reconsile(file1, null))
            .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void reconsile_shouldThrowException_whenFile1IsEmpty() {
        // Given
        MockMultipartFile file1 = new MockMultipartFile("file1", "file1.csv", "text/csv", new byte[0]);
        MockMultipartFile file2 = createValidCsvFile("file2.csv", "TXN001");

        // When & Then
        assertThatThrownBy(() -> service.reconsile(file1, file2))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("file1 is required");
    }

    @Test
    void reconsile_shouldReturnPerfectMatch_whenIdenticalRecords() {
        // Given
        MockMultipartFile file1 = createCsvFileWithRecord(
            "file1.csv", "John Doe", "2023-01-01 10:00:00", "100.50", "Payment", "TXN001", "1", "WALLET123");
        MockMultipartFile file2 = createCsvFileWithRecord(
            "file2.csv", "John Doe", "2023-01-01 10:00:00", "100.50", "Payment", "TXN001", "1", "WALLET123");

        // When
        ReconciliationResult result = service.reconsile(file1, file2);

        // Then
        assertThat(result.matchedCount()).isEqualTo(1);
        assertThat(result.unmatchedCount()).isEqualTo(0);
        assertThat(result.unmatched()).isEmpty();
    }

    @Test
    void reconsile_shouldDetectDetailsMismatch_whenSameAmountDifferentNarrative() {
        // Given
        MockMultipartFile file1 = createCsvFileWithRecord(
            "file1.csv", "John Doe", "2023-01-01 10:00:00", "100.50", "Payment for services", "TXN001", "1", "WALLET123");
        MockMultipartFile file2 = createCsvFileWithRecord(
            "file2.csv", "John Doe", "2023-01-01 10:00:00", "100.50", "Different payment description", "TXN001", "1", "WALLET123");

        // When
        ReconciliationResult result = service.reconsile(file1, file2);

        // Then
        assertThat(result.matchedCount()).isEqualTo(0);
        assertThat(result.unmatchedCount()).isEqualTo(1);
        assertThat(result.unmatched()).hasSize(1);
        
        UnmatchedTransaction unmatched = result.unmatched().get(0);
        assertThat(unmatched.reason()).isEqualTo(UnmatchedReason.DETAILS_MISMATCH);
        assertThat(unmatched.transactionId()).isEqualTo("TXN001");
        assertThat(unmatched.file1()).isNotNull();
        assertThat(unmatched.file2()).isNotNull();
    }

    @Test
    void reconsile_shouldDetectDetailsMismatch_whenSameAmountDifferentWalletReference() {
        // Given
        MockMultipartFile file1 = createCsvFileWithRecord(
            "file1.csv", "John Doe", "2023-01-01 10:00:00", "100.50", "Payment", "TXN001", "1", "WALLET123");
        MockMultipartFile file2 = createCsvFileWithRecord(
            "file2.csv", "John Doe", "2023-01-01 10:00:00", "100.50", "Payment", "TXN001", "1", "WALLET456");

        // When
        ReconciliationResult result = service.reconsile(file1, file2);

        // Then
        assertThat(result.matchedCount()).isEqualTo(0);
        assertThat(result.unmatchedCount()).isEqualTo(1);
        assertThat(result.unmatched()).hasSize(1);
        
        UnmatchedTransaction unmatched = result.unmatched().get(0);
        assertThat(unmatched.reason()).isEqualTo(UnmatchedReason.DETAILS_MISMATCH);
    }

    @Test
    void reconsile_shouldDetectNotIdentical_whenDifferentAmounts() {
        // Given
        MockMultipartFile file1 = createCsvFileWithRecord(
            "file1.csv", "John Doe", "2023-01-01 10:00:00", "100.50", "Payment", "TXN001", "1", "WALLET123");
        MockMultipartFile file2 = createCsvFileWithRecord(
            "file2.csv", "John Doe", "2023-01-01 10:00:00", "200.75", "Payment", "TXN001", "1", "WALLET123");

        // When
        ReconciliationResult result = service.reconsile(file1, file2);

        // Then
        assertThat(result.matchedCount()).isEqualTo(0);
        assertThat(result.unmatchedCount()).isEqualTo(1);
        assertThat(result.unmatched()).hasSize(1);
        
        UnmatchedTransaction unmatched = result.unmatched().get(0);
        assertThat(unmatched.reason()).isEqualTo(UnmatchedReason.NOT_IDENTICAL);
    }

    @Test
    void reconsile_shouldDetectMissingInOtherFile_whenRecordOnlyInFile1() {
        // Given
        MockMultipartFile file1 = createCsvFileWithRecord(
            "file1.csv", "John Doe", "2023-01-01 10:00:00", "100.50", "Payment", "TXN001", "1", "WALLET123");
        MockMultipartFile file2 = createEmptyCsvFile("file2.csv");

        // When
        ReconciliationResult result = service.reconsile(file1, file2);

        // Then
        assertThat(result.matchedCount()).isEqualTo(0);
        assertThat(result.unmatchedCount()).isEqualTo(1);
        assertThat(result.unmatched()).hasSize(1);
        
        UnmatchedTransaction unmatched = result.unmatched().get(0);
        assertThat(unmatched.reason()).isEqualTo(UnmatchedReason.MISSING_IN_OTHER_FILE);
        assertThat(unmatched.file1()).isNotNull();
        assertThat(unmatched.file2()).isNull();
    }

    @Test
    void reconsile_shouldDetectMissingInOtherFile_whenRecordOnlyInFile2() {
        // Given
        MockMultipartFile file1 = createEmptyCsvFile("file1.csv");
        MockMultipartFile file2 = createCsvFileWithRecord(
            "file2.csv", "Jane Smith", "2023-01-01 10:00:00", "250.00", "Shopping", "TXN002", "2", "WALLET456");

        // When
        ReconciliationResult result = service.reconsile(file1, file2);

        // Then
        assertThat(result.matchedCount()).isEqualTo(0);
        assertThat(result.unmatchedCount()).isEqualTo(1);
        assertThat(result.unmatched()).hasSize(1);
        
        UnmatchedTransaction unmatched = result.unmatched().get(0);
        assertThat(unmatched.reason()).isEqualTo(UnmatchedReason.MISSING_IN_OTHER_FILE);
        assertThat(unmatched.file1()).isNull();
        assertThat(unmatched.file2()).isNotNull();
    }

    @Test
    void reconsile_shouldHandleMultipleRecordsInSameGroup() {
        // Given - File1 has 2 records with same grouping key, File2 has 1 record with same key
        String csvContent1 = """
            ProfileName,TransactionDate,TransactionAmount,TransactionNarrative,TransactionDescription,TransactionID,TransactionType,WalletReference
            John Doe,2023-01-01 10:00:00,100.50,Payment 1,Service payment,TXN001,1,WALLET123
            John Doe,2023-01-01 10:00:00,100.50,Payment 2,Service payment,TXN002,1,WALLET123
            """;
        
        String csvContent2 = """
            ProfileName,TransactionDate,TransactionAmount,TransactionNarrative,TransactionDescription,TransactionID,TransactionType,WalletReference
            John Doe,2023-01-01 10:00:00,100.50,Payment 1,Service payment,TXN001,1,WALLET123
            """;
        
        MockMultipartFile file1 = new MockMultipartFile("file1", "file1.csv", "text/csv", csvContent1.getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file2", "file2.csv", "text/csv", csvContent2.getBytes());

        // When
        ReconciliationResult result = service.reconsile(file1, file2);

        // Then
        assertThat(result.matchedCount()).isEqualTo(1); // TXN001 matches perfectly
        assertThat(result.unmatchedCount()).isEqualTo(1); // TXN002 has no match in file2
        
        UnmatchedTransaction unmatched = result.unmatched().get(0);
        assertThat(unmatched.reason()).isEqualTo(UnmatchedReason.MISSING_IN_OTHER_FILE);
        assertThat(unmatched.file1().transactionId()).isEqualTo("TXN002");
        assertThat(unmatched.file2()).isNull();
    }

    @Test
    void reconsile_shouldHandleBigDecimalComparison_withDifferentScales() {
        // Given - Same amounts but different scales (100.50 vs 100.5)
        MockMultipartFile file1 = createCsvFileWithRecord(
            "file1.csv", "John Doe", "2023-01-01 10:00:00", "100.50", "Payment", "TXN001", "1", "WALLET123");
        MockMultipartFile file2 = createCsvFileWithRecord(
            "file2.csv", "John Doe", "2023-01-01 10:00:00", "100.5", "Payment", "TXN001", "1", "WALLET123");

        // When
        ReconciliationResult result = service.reconsile(file1, file2);

        // Then
        assertThat(result.matchedCount()).isEqualTo(1);
        assertThat(result.unmatchedCount()).isEqualTo(0);
    }

    @Test
    void reconsile_shouldRespectTextNormalizationSettings() {
        // Given - Different cases and spacing in narrative, but same amount
        // Since normalized narratives are the same, this should be NOT_IDENTICAL
        // (fails areIdentical due to raw narrative difference, but passes details mismatch check)
        properties.setNormalizeCase(true);
        properties.setCollapseWhitespace(true);
        RetryProperties retryProperties = new RetryProperties();
        service = new CsvReconciliationService(properties, retryProperties);
        
        MockMultipartFile file1 = createCsvFileWithRecord(
            "file1.csv", "John Doe", "2023-01-01 10:00:00", "100.50", "PAYMENT   FOR   SERVICES", "TXN001", "1", "WALLET123");
        MockMultipartFile file2 = createCsvFileWithRecord(
            "file2.csv", "John Doe", "2023-01-01 10:00:00", "100.50", "payment for services", "TXN001", "1", "WALLET123");

        // When
        ReconciliationResult result = service.reconsile(file1, file2);

        // Then - Should be NOT_IDENTICAL because raw narratives differ but normalized ones are same
        assertThat(result.matchedCount()).isEqualTo(0);
        assertThat(result.unmatchedCount()).isEqualTo(1);
        assertThat(result.unmatched().get(0).reason()).isEqualTo(UnmatchedReason.NOT_IDENTICAL);
    }

    // ========== Helper Methods ==========

    private MockMultipartFile createValidCsvFile(String filename, String transactionId) {
        String csvContent = String.format("""
            ProfileName,TransactionDate,TransactionAmount,TransactionNarrative,TransactionDescription,TransactionID,TransactionType,WalletReference
            John Doe,2023-01-01 10:00:00,100.50,Payment for services,Service payment,%s,1,WALLET123
            """, transactionId);
        
        return new MockMultipartFile("file", filename, "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile createCsvFileWithRecord(String filename, String profileName, String date, 
            String amount, String narrative, String transactionId, String type, String walletRef) {
        String csvContent = String.format("""
            ProfileName,TransactionDate,TransactionAmount,TransactionNarrative,TransactionDescription,TransactionID,TransactionType,WalletReference
            %s,%s,%s,%s,Description,%s,%s,%s
            """, profileName, date, amount, narrative, transactionId, type, walletRef);
        
        return new MockMultipartFile("file", filename, "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile createEmptyCsvFile(String filename) {
        String csvContent = "ProfileName,TransactionDate,TransactionAmount,TransactionNarrative,TransactionDescription,TransactionID,TransactionType,WalletReference\n";
        return new MockMultipartFile("file", filename, "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
    }
}
