# Overview

**Duration**: 60 minutes

**Approach**: Test-Driven Development (TDD) - Preferred

**Language**: Java/Kotlin

**Structure**: This exercise has **3 progressive milestones**. Complete them in order:

- **Milestone 1**: Core functionality (40-45 minutes) - Average candidates should complete this
- **Milestone 2**: Production-ready essentials (15-20 minutes) - Good candidates complete M1 + M2 partially
- **Milestone 3**: Advanced features (only if time permits) - Expert candidates may start this

**Realistic Expectation**: In 60 minutes, most candidates will complete Milestone 1 + partial Milestone 2. That’s
success!

---

## Background

eKYC is the digital process of verifying a customer’s identity remotely. Financial institutions use eKYC to onboard
customers while complying with regulatory requirements.

You will build an **eKYC Verification Service** that orchestrates verification requests across multiple external
microservices.

### Microservices Architecture

Your service communicates with 4 external microservices:

1. **Document Verification Service** - `/api/v1/verify-document`
2. **Biometric Service** - `/api/v1/face-match`
3. **Address Verification Service** - `/api/v1/verify-address`
4. **Sanctions Screening Service** - `/api/v1/check-sanctions`

**Important Constraints**:

- Each service has a rate limit of **10 requests per minute**
- Services may timeout or fail (must handle gracefully)
- Services may return partial failures requiring manual review

---

## Data Models

### Customer

```python
{
  "customer_id": "CUST-001",
  "full_name": "Jane Doe",
  "date_of_birth": "1990-05-15",
  "email": "jane.doe@example.com",
  "phone": "+1-555-0123",
  "address": "123 Main St, Springfield, IL 62701"
}
```

### Verification Request

```python
{
  "request_id": "REQ-12345",
  "customer_id": "CUST-001",
  "verification_types": ["ID_DOCUMENT", "FACE_MATCH", "ADDRESS", "SANCTIONS"],
  "timestamp": "2026-01-08T10:30:00Z"
}
```

### Verification Result

```python
{
  "verification_type": "ID_DOCUMENT",
  "status": "PASS" | "FAIL" | "MANUAL_REVIEW",
  "confidence": 95,  # 0-100
  "reasons": [],  # List of failure reasons if any
  "timestamp": "2026-01-08T10:30:05Z"
}
```

### KYC Decision

```python
{
  "decision": "APPROVED" | "REJECTED" | "MANUAL_REVIEW",
  "verification_results": [...],  # All verification results
  "timestamp": "2026-01-08T10:30:10Z"
}
```

---

## External Service APIs

### 1. Document Verification Service

**Endpoint**: `POST /api/v1/verify-document`

**Timeout**: 5 seconds

**Rate Limit**: 10 req/min

**Request**:

```json
{
  "customer_id": "CUST-001",
  "document_type": "PASSPORT",
  "document_number": "P12345678",
  "expiry_date": "2027-12-31",
  "document_image_url": "https://..."
}
```

**Response**:

```json
{
  "status": "PASS",
  "confidence": 95,
  "reasons": []
}
```

### 2. Biometric Service

**Endpoint**: `POST /api/v1/face-match`

**Timeout**: 8 seconds

**Rate Limit**: 10 req/min

**Request**:

```json
{
  "customer_id": "CUST-001",
  "selfie_url": "https://...",
  "id_photo_url": "https://..."
}
```

**Response**:

```json
{
  "status": "PASS",
  "confidence": 92,
  "similarity_score": 92.5
}
```

### 3. Address Verification Service

**Endpoint**: `POST /api/v1/verify-address`

**Timeout**: 5 seconds

**Rate Limit**: 10 req/min

**Request**:

```json
{
  "customer_id": "CUST-001",
  "address": "123 Main St, Springfield, IL 62701",
  "proof_type": "UTILITY_BILL",
  "proof_date": "2025-12-15",
  "proof_url": "https://..."
}
```

**Response**:

```json
{
  "status": "PASS",
  "confidence": 88,
  "reasons": []
}
```

### 4. Sanctions Screening Service (CRITICAL)

**Endpoint**: `POST /api/v1/check-sanctions`

**Timeout**: 3 seconds

**Rate Limit**: 10 req/min

**Request**:

```json
{
  "customer_id": "CUST-001",
  "full_name": "Jane Doe",
  "date_of_birth": "1990-05-15",
  "nationality": "US"
}
```

**Response**:

```json
{
  "status": "CLEAR" | "HIT",
  "match_count": 0,
  "matches": []
}