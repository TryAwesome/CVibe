# Interface Contracts & Protocols

> **Note to OpenCode:** This document defines the "Laws" of communication between services. Strict adherence to Protobuf definitions and Kafka Schemas is mandatory. **NO breaking changes** are allowed without incrementing the version number (e.g., v1 -> v2).

## 1. gRPC & Protobuf Standards

All internal synchronous communication MUST use gRPC.

### 1.1 Directory Structure & Versioning
All `.proto` files reside in the `/proto` directory at the project root.
*   **Path Convention**: `proto/<domain>/<version>/<file>.proto`
*   **Package Name**: `career.<domain>.<version>`

**Example**:
```text
/proto
  /job
    /v1
      search.proto  (package career.job.v1)
  /resume
    /v1
      parser.proto  (package career.resume.v1)
  /common
    /v1
      error.proto   (package career.common.v1, defines BusinessError)
```

### 1.2 Style Guide
*   **Service Names**: `PascalCase` (e.g., `JobSearchService`).
*   **RPC Methods**: `PascalCase` (e.g., `SearchJobs`).
*   **Message Names**: `PascalCase` (e.g., `JobQueryRequest`).
*   **Field Names**: `snake_case` (e.g., `user_id`, `min_salary`).
*   **Enums**: `UPPER_SNAKE_CASE` (e.g., `JOB_STATUS_OPEN`).

### 1.3 Error Propagation (The "Rich Error" Model)
We strictly use `google.rpc.Status` for all gRPC errors.
*   **Standard Errors**: Use gRPC standard codes (e.g., `UNAVAILABLE`, `DEADLINE_EXCEEDED`) for transport/infrastructure failures.
*   **Business Errors**: Use `ABORTED` or `FAILED_PRECONDITION` code, but MUST attach a `career.common.v1.BusinessError` message in the `details` field.
    *   This detail message contains the **5-digit Business Code** (e.g., `30004`).
    *   *Constraint*: The Gateway/Biz Service must unwrap this detail to construct the REST response.

## 2. Kafka Message Contracts

Asynchronous communication is event-driven. All payloads MUST be valid JSON, defined by schemas stored in `/doc/schemas/kafka/`.

### 2.1 Topic Naming & DLQ
Format: `domain.entity.action`
*   **Main Topic**: `resume.file.uploaded`
*   **DLQ (Dead Letter Queue)**: `resume.file.uploaded.dlq`

### 2.2 Reliability & Ordering
*   **Message Key**: MANDATORY. Use logical IDs (e.g., `user_id`, `job_hash`, `resume_id`) to ensure partitioning and ordering.
*   **Semantics**: At-least-once.
*   **Idempotency**: Consumers MUST handle duplicate messages (e.g., by checking a `processed_ids` Redis set or DB unique constraints).
*   **Retry Policy**:
    *   **Transient Errors** (Network/Timeout): Retry 3 times with exponential backoff.
    *   **Permanent Errors** (Schema Mismatch/Data Corruption): Send immediately to DLQ. Do NOT retry.

### 2.3 Headers (Trace Propagation)
All Kafka messages MUST include:
*   `X-Trace-ID`: UUID (Propagated from the original HTTP request).
*   `X-Producer-ID`: Service Name (e.g., `biz-service`).
*   `X-Timestamp`: ISO8601 string.

## 3. Error Handling Specifications

We use a **5-digit Business Error Code** system.

### 3.1 Error Code Ranges
*   **10000 - 19999**: General / System Errors.
*   **20000 - 29999**: User & Auth (Invalid Token, Account Locked).
*   **30000 - 39999**: Resume & File.
*   **40000 - 49999**: Job & Search.
*   **50000 - 59999**: AI Engine (Safety, Quota).

### 3.2 Common Error Codes Table

| Code | Mapped HTTP Status | Description |
| :--- | :--- | :--- |
| `10001` | 500 | Internal Server Error. |
| `20001` | 401 | Unauthorized (Token expired/missing). |
| `20003` | 403 | Forbidden (Access Denied). |
| `30004` | 422 | Resume Parsing Failed. |
| `50005` | 400 | AI Safety Block. |

## 4. REST API & Auth Contracts (Frontend <-> Biz Service)

### 4.1 Authentication & RBAC
*   **Mechanism**: **JWT (JSON Web Token)** stored in `HttpOnly` Secure Cookie (named `CA_SESSION`).
*   **Lifespan**: Access Token (1 hour), Refresh Token (7 days).
*   **Roles (RBAC)**:
    *   `ROLE_USER`: Standard access.
    *   `ROLE_ADMIN`: Access to `/api/admin/**`.
*   **Gateway Responsibility**: Validates JWT signature. If invalid/expired -> 401.

### 4.2 Distributed Tracing
*   **Frontend**: MUST generate or propagate `X-Trace-ID` header for every request.
*   **Backend**: MUST log this ID in every log line and pass it to downstream gRPC/Kafka calls.

### 4.3 Response Format
```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "meta": {
    "trace_id": "abc-123",  // Must match X-Trace-ID
    "timestamp": "2023-10-27T10:00:00Z"
  }
}
```

**Error Response**:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": 30004,
    "message": "The PDF file is encrypted...",
    "details": "..." // Optional
  },
  "meta": { ... }
}
```
