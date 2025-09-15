# Financial Transaction Reconciliation Service

A Spring Boot application that performs intelligent reconciliation between two CSV files containing financial transaction data. The service identifies matched, mismatched, and missing transactions using a sophisticated grouping and matching algorithm.

## Table of Contents

- [Introduction](#introduction)
- [Core Reconciliation Logic](#core-reconciliation-logic)
- [Use Cases](#use-cases)
- [Tech Stack](#tech-stack)
- [API Endpoints](#api-endpoints)
- [Request/Response DTOs](#requestresponse-dtos)
- [Running Locally](#running-locally)
- [Heroku Deployment](#heroku-deployment)
- [Configuration](#configuration)
- [API Versioning & Exception Handling](#api-versioning--exception-handling)

## Introduction

This reconciliation service addresses the common financial industry challenge of matching transactions across different systems or data sources. It processes CSV files containing transaction records and provides detailed analysis of matches, discrepancies, and missing entries.

The service uses a two-stage algorithm:
1. **Intelligent Grouping**: Records are grouped by transaction ID or composite keys (amount + time window + profile + type)
2. **Greedy Matching**: Within each group, transactions are paired using a hierarchy of matching criteria

## Core Reconciliation Logic

### 1. Grouping Strategy

Records are grouped using the best available key:

- **Primary**: If `TransactionID` exists and is non-blank → `ID:<TransactionID>`
- **Fallback**: Composite key → `K|<amount>|<dateBucket>|<normalizedProfile>|<type>`

The date bucket groups transactions within a configurable time window (default: 5 minutes).

### 2. Matching Algorithm

Within each group, the service uses a **LIFO (Last-In-First-Out) greedy matching** approach:

```
For each grouping key:
  While both lists have records:
    1. Take last record from each list
    2. Apply matching criteria hierarchy:
       - IDENTICAL: All fields match exactly → Count as matched
       - DETAILS_MISMATCH: Same amount, different narrative/wallet → Flag mismatch
       - NOT_IDENTICAL: Other differences → Flag as not identical
    3. Remove both records from lists
  
  Remaining records in either list → MISSING_IN_OTHER_FILE
```

### 3. Text Normalization

Configurable text normalization is applied during comparison:
- **Case folding**: Convert to lowercase
- **Whitespace collapsing**: Trim and collapse multiple spaces
- **Punctuation stripping**: Remove punctuation characters

### 4. Matching Criteria Details

| Criteria | Description | Example |
|----------|-------------|---------|
| **IDENTICAL** | All fields match exactly | Perfect match across all transaction fields |
| **DETAILS_MISMATCH** | Same amount but narrative/wallet differ | `$100 "Coffee Shop"` vs `$100 "Coffee Shp"` |
| **NOT_IDENTICAL** | Other field differences | `$100 Coffee` vs `$95 Coffee` |
| **MISSING_IN_OTHER_FILE** | No counterpart found | Transaction exists in one file only |

## Use Cases

### Banking & Financial Services
- **Account reconciliation**: Match transactions between core banking systems and external processors
- **Payment processing**: Reconcile merchant transactions with payment gateway records
- **Regulatory reporting**: Ensure transaction consistency across different regulatory reports

### E-commerce & Retail
- **Payment reconciliation**: Match online orders with payment processor transactions
- **Multi-channel sales**: Reconcile transactions across web, mobile, and in-store systems
- **Refund tracking**: Match refund requests with actual payment reversals

### Enterprise Finance
- **System integration**: Reconcile data between ERP, CRM, and financial systems
- **Audit preparation**: Identify discrepancies before financial audits
- **Data quality**: Detect and report data inconsistencies across systems

## Tech Stack

- **Framework**: Spring Boot 3.3.4
- **Java Version**: 21
- **Build Tool**: Maven
- **CSV Processing**: OpenCSV 5.9
- **Documentation**: SpringDoc OpenAPI 2.6.0
- **Utilities**: Lombok, Apache Commons IO
- **Exception Handling**: RFC 7807 ProblemDetail for structured error responses
- **Deployment**: Heroku-ready with Procfile

## API Endpoints

### POST /api/reconciliation

Uploads two CSV files and returns reconciliation results.

**Headers:**
- `X-API-Version: 1` (optional, defaults to "1") - Supported versions: ["1", "2"]
- `Content-Type: multipart/form-data`

**Parameters:**
- `file1`: First CSV file (multipart)
- `file2`: Second CSV file (multipart)

**Response:** JSON with reconciliation results

**Error Handling:**
The API uses RFC 7807 ProblemDetail format for structured error responses. Common errors include:
- `UNSUPPORTED_API_VERSION`: When an invalid API version is requested
- `INVALID_FILE`: When uploaded files are empty or invalid
- `CSV_PROCESSING_ERROR`: When CSV parsing fails
- `FILE_TOO_LARGE`: When file size exceeds limits

### Additional Endpoints

- **Swagger UI**: `/swagger-ui.html`
- **OpenAPI Spec**: `/v3/api-docs`
- **Health Check**: `/actuator/health` (if actuator enabled)

## Request/Response DTOs

### CSV File Format

Expected CSV headers:
```csv
ProfileName,TransactionDate,TransactionAmount,TransactionNarrative,TransactionDescription,TransactionID,TransactionType,WalletReference
```

**Field Descriptions:**
- `ProfileName`: Account or profile identifier
- `TransactionDate`: ISO format datetime (e.g., "2014-01-11 22:27:44")
- `TransactionAmount`: Decimal amount (positive/negative)
- `TransactionNarrative`: Transaction description/memo
- `TransactionDescription`: Transaction category/type description
- `TransactionID`: Unique transaction identifier (optional)
- `TransactionType`: Integer type code
- `WalletReference`: Wallet or account reference

### Response DTO

```json
{
  "matchedCount": 295,
  "unmatchedCount": 12,
  "unmatched": [
    {
      "transactionId": "TX123",
      "file1": {
        "profileName": "ACME_CORP",
        "transactionDate": "2025-09-14T10:15:30",
        "transactionAmount": 199.99,
        "transactionNarrative": "Invoice 5567",
        "transactionDescription": "INVOICE",
        "transactionId": "TX123",
        "transactionType": 1,
        "walletReference": "W-001"
      },
      "file2": {
        "profileName": "ACME_CORP",
        "transactionDate": "2025-09-14T10:15:30",
        "transactionAmount": 199.99,
        "transactionNarrative": "Invoice 5567 Payment",
        "transactionDescription": "INVOICE",
        "transactionId": "TX123",
        "transactionType": 1,
        "walletReference": "W-002"
      },
      "reason": "DETAILS_MISMATCH"
    }
  ]
}
```

**UnmatchedReason Values:**
- `MISSING_IN_OTHER_FILE`: Transaction exists in one file only
- `DETAILS_MISMATCH`: Same amount but narrative/wallet differ
- `NOT_IDENTICAL`: Other field differences

### Error Response Format (RFC 7807)

When an error occurs, the API returns a structured error response:

```json
{
  "type": "http://localhost:8080/swagger-ui.html#/error-handling/unsupported-api-version",
  "title": "Unsupported API Version",
  "detail": "API version '3' is not supported. Supported versions: 1, 2",
  "errorCode": "UNSUPPORTED_API_VERSION",
  "timestamp": "2025-09-15T11:08:37Z",
  "description": "The requested API version is not supported by this service",
  "requestedVersion": "3",
  "supportedVersions": "1, 2"
}
```

**Common Error Codes:**
- `UNSUPPORTED_API_VERSION`: Invalid API version requested
- `INVALID_FILE`: File validation failed (empty, null, wrong format)
- `CSV_PROCESSING_ERROR`: CSV parsing or processing failed
- `FILE_TOO_LARGE`: File size exceeds configured limits
- `INTERNAL_SERVER_ERROR`: Unexpected server error

## Running Locally

### Prerequisites
- Java 21
- Maven 3.6+

### Steps

1. **Clone the repository**
```bash
git clone <repository-url>
cd reconsiliation
```

2. **Build the application**
```bash
mvn clean package -DskipTests
```

3. **Run the application**
```bash
mvn spring-boot:run
```

4. **Access the application**
- API Base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Making API Calls

**Using cURL:**
```bash
curl -X POST http://localhost:8080/api/reconciliation \
  -H "X-API-Version: 1" \
  -F file1=@path/to/transactions1.csv \
  -F file2=@path/to/transactions2.csv
```

**Using Postman:**
1. Set method to `POST`
2. URL: `http://localhost:8080/api/reconciliation`
3. Headers: `X-API-Version: 1`
4. Body: `form-data` with `file1` and `file2` file uploads

## Heroku Deployment

The application is configured for Heroku deployment with:
- `Procfile`: Web process configuration
- `system.properties`: Java version specification
- Dynamic port binding via `$PORT` environment variable

### Deployed Application

**Base URL:** `https://your-app-name.herokuapp.com`

**Making API Calls:**
```bash
curl -X POST https://your-app-name.herokuapp.com/api/reconciliation \
  -H "X-API-Version: 1" \
  -F file1=@path/to/transactions1.csv \
  -F file2=@path/to/transactions2.csv
```

**Swagger UI:** `https://your-app-name.herokuapp.com/swagger-ui.html`

## Configuration

### Application Properties (YAML)

```yaml
spring:
  application:
    name: reconsiliation
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB
  mvc:
    async:
      request-timeout: 60000ms

server:
  port: ${PORT:8080}

reconciliation:
  date-window-seconds: 300      # 5 minutes grouping window
  normalize-case: true          # Convert text to lowercase
  collapse-whitespace: true     # Collapse multiple spaces
  strip-punctuation: false      # Remove punctuation
  compare-wallet-reference: true
  consider-transaction-type: true

api:
  versioning:
    header: X-API-Version
    default-version: "1"
    supported: ["1", "2"]
```

### Key Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `date-window-seconds` | 300 | Time window for grouping transactions (seconds) |
| `normalize-case` | true | Convert text to lowercase for comparison |
| `collapse-whitespace` | true | Collapse multiple whitespace to single space |
| `strip-punctuation` | false | Remove punctuation from text fields |
| `api.versioning.supported` | ["1", "2"] | List of supported API versions |
| `api.versioning.default-version` | "1" | Default API version when none specified |

## API Versioning & Exception Handling

### Version Support

The API supports versioning through the `X-API-Version` header:

- **Supported Versions**: Configurable list in `application.yaml` (currently ["1", "2"])
- **Default Version**: "1" (used when no version header is provided)
- **Validation**: Requests with unsupported versions return `400 Bad Request` with detailed error information

### Custom Exception Hierarchy

The application uses a structured exception hierarchy for better error handling:

```
ReconciliationException (abstract base)
├── UnsupportedApiVersionException
├── CsvProcessingException
└── InvalidFileException
```

### Error Type Management

All error types are managed through an `ErrorType` enum that provides:

- **Consistent Error Codes**: Standardized error codes across the application
- **Human-Readable Titles**: User-friendly error titles
- **URL Fragments**: Swagger documentation links for each error type
- **Descriptions**: Detailed explanations of each error condition

**Available Error Types:**
- `UNSUPPORTED_API_VERSION`: Invalid API version requested
- `CSV_PROCESSING_ERROR`: CSV parsing or processing failed
- `INVALID_FILE`: File validation failed (empty, null, wrong format)  
- `FILE_TOO_LARGE`: File size exceeds configured limits
- `INTERNAL_SERVER_ERROR`: Unexpected server error

### Global Exception Handler

All exceptions are handled by a centralized `@RestControllerAdvice` that:

- **RFC 7807 Compliance**: Returns ProblemDetail responses following RFC 7807 standard
- **Dynamic URLs**: Error type URLs point to Swagger documentation for context
- **Structured Responses**: Consistent error codes, titles, descriptions, and timestamps
- **Context-Specific Information**: Additional properties for specific error types (e.g., supported versions)
- **Centralized Management**: Single point for all exception handling logic
- **Environment-Aware**: URLs adapt to server configuration (port, context path)

### Testing Exception Scenarios

**Test unsupported API version:**
```bash
curl -X POST http://localhost:8080/api/reconciliation \
  -H "X-API-Version: 99" \
  -F file1=@test1.csv \
  -F file2=@test2.csv
```

**Expected Response (400 Bad Request):**
```json
{
  "type": "http://localhost:8080/swagger-ui.html#/error-handling/unsupported-api-version",
  "title": "Unsupported API Version",
  "detail": "API version '99' is not supported. Supported versions: 1, 2",
  "errorCode": "UNSUPPORTED_API_VERSION",
  "timestamp": "2025-09-15T11:08:37Z",
  "description": "The requested API version is not supported by this service",
  "requestedVersion": "99",
  "supportedVersions": "1, 2"
}
```

### Environment Variables (Heroku)

- `PORT`: Application port (set automatically by Heroku)
- `JAVA_OPTS`: JVM options (optional)

## Sample Data

The repository includes sample CSV data in `src/main/resources/static/` for testing purposes.

## Logging

The application uses SLF4J with detailed logging:
- **INFO**: Processing progress, timing, final results
- **DEBUG**: Group distribution, per-key processing details
- **ERROR**: Validation failures, parsing errors

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License.
