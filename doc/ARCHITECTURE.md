# CareerAgent System Architecture

> **Note to OpenCode:** This document is the **Single Source of Truth** for the high-level system design, service boundaries, and infrastructure configuration. Always refer to this when creating new services or modifying infrastructure.

## 1. System Overview

CareerAgent is a polyglot microservices application designed to help users optimize their career paths through AI-driven resume analysis, gap analysis, and automated job matching.

The system adopts a **Monorepo** structure to facilitate code sharing (Protobuf contracts) and unified CI/CD.

### Architectural Pattern
*   **Microservices**: Distinct services separated by business domain and technical requirements.
*   **Polyglot**: Java (Business), Python (AI), Go (High Performance).
*   **Communication**:
    *   **Synchronous**: gRPC (Internal service-to-service ONLY).
    *   **Asynchronous**: Kafka (Event-driven architecture with strict Schema).
    *   **External**: REST/HTTP (Browser <-> Gateway ONLY).

## 2. Service Inventory

| Service ID | Directory | Language | Framework | Responsibility |
| :--- | :--- | :--- | :--- | :--- |
| **Gateway / Frontend** | `/frontend` | TS/JS | Next.js (React) | **User Client (Registration, Login, Core UI)**, **Admin Backend (Control Panel)**, SSR, Auth Proxy, API Aggregation. **Sole entry point for external traffic.** |
| **Biz Service** | `/biz-service` | Java | Spring Boot 3 | User Mgmt, Resume Parsing orchestration, General API. **Owner of Core Domain Data.** |
| **AI Engine** | `/ai-engine` | Python | Custom (Py) | Resume Optimization, Gap Analysis, Chatbot Logic (RAG). **Internal Service Only.** |
| **Search Service** | `/search-service`| Go | gRPC/Native | Job Crawling, High-concurrency Searching, Cleaning. **Raw Data Producer.** |

## 3. Data Flow & Communication Rules

### 3.1 API Access Strategy
1.  **Browser Access**: All external requests MUST go through the **Gateway/Frontend**. Direct access to backend services is blocked by network policy.
2.  **Internal Communication**:
    *   Service-to-Service synchronous calls MUST use **gRPC**.
    *   **AI Engine** exposes port 5000 strictly to the internal docker network. No public exposure.

### 3.2 Kafka (Event Driven Contracts)

All messages use **JSON Schema** for payload validation.

#### Topic: `resume.uploaded`
*   **Purpose**: Trigger AI analysis after a user uploads a resume.
*   **Producer**: `Biz-Service`
*   **Consumer**: `AI-Engine`
*   **Key**: `user_id` (Ensure sequential processing for same user).
*   **Schema**:
    ```json
    { "user_id": "uuid", "resume_url": "s3://...", "uploaded_at": "iso8601" }
    ```
*   **Delivery**: At-least-once.
*   **Idempotency**: AI Engine checks `resume_version` in Redis before processing.
*   **Retry/DLQ**: 3 Retries -> `resume.uploaded.dlq`.

#### Topic: `job.crawl_task`
*   **Purpose**: Dispatch crawling tasks based on user gap analysis or manual search.
*   **Producer**: `AI-Engine` (Agent decision) or `Biz-Service` (User request).
*   **Consumer**: `Search-Service`.
*   **Key**: `url_hash` (For partitioning and deduplication).
*   **Schema**:
    ```json
    { "task_id": "uuid", "target_url": "https://...", "strategy": "bfs|dfs", "depth": 2 }
    ```
*   **Deduplication**: Consumer checks `dedup_key = hash(target_url + date)` in Redis Set.
*   **Retry/DLQ**: 5 Retries (exponential backoff) -> `job.crawl_task.dlq`.

#### Topic: `job.found`
*   **Purpose**: Stream crawled job data for storage and indexing.
*   **Producer**: `Search-Service`
*   **Consumer**: `Biz-Service` (Persistence).
*   **Key**: `job_id` (generated hash).
*   **Schema**:
    ```json
    { "job_hash": "...", "raw_html": "...", "parsed_data": {...}, "status": "raw|cleaned" }
    ```
*   **Delivery**: At-least-once.

## 4. Persistence Ownership & Data Layer

### 4.1 Storage Strategy
| Component | Image/Version | Role & Ownership |
| :--- | :--- | :--- |
| **PostgreSQL** | `postgres:15-alpine` | **Primary Truth**. Owned by **Biz-Service**. Includes user data, resumes, jobs, and **metadata for user backup management**. `Search-Service` and `AI-Engine` must NOT write directly (Read-only replicas allowed, or fetch via gRPC). |
| **Redis** | `redis:7-alpine` | **Shared State**. Session, Dedup Keys, Rate Limits. |
| **Kafka** | `confluentinc/cp-kafka:7.4` | **Message Bus**. |
| **Object Store** | `MinIO` (Dev) / `S3` | **File Storage**. Raw PDF/Word resumes. Owned by **Biz-Service**. |
| **Vector DB** | `pgvector` (in Postgres) | **Embeddings**. Owned by **Biz-Service** (managed via JPA), queried by **AI-Engine**. |

### 4.2 Write Path Responsibilities
*   **Users & Resumes**: `Biz-Service` validates and writes to PostgreSQL.
*   **Jobs**: `Search-Service` produces to Kafka; `Biz-Service` consumes and writes to PostgreSQL (ensures data consistency).
*   **Embeddings**: `AI-Engine` computes embeddings, returns via gRPC to `Biz-Service` to save into `pgvector`.
    *   *Constraint*: AI Engine never directly inserts into DB.

## 5. Network Topology (Port Mapping)

| Service | Container Port | Host Port (Dev) | Visibility |
| :--- | :--- | :--- | :--- |
| **Frontend** | 3000 | `3000` | Public |
| **Biz Service** | 8080 | `8080` | Internal (exposed for dev) |
| **Search Service**| 9090 | `9090` | Internal (exposed for dev) |
| **AI Engine** | 5000 | `5000` | **Strictly Internal** |
| **PostgreSQL** | 5432 | `5432` | Internal |
| **Redis** | 6379 | `6379` | Internal |
| **Kafka** | 9092 | `9092` | Internal |
| **MinIO** | 9000/9001 | `9000/9001` | Internal |

## 6. Directory Structure (Root)

```text
/
├── .gemini/                  # OpenCode/Gemini specific configs
├── doc/                      # Documentation (Architecture, Contracts, etc.)
├── proto/                    # [Source of Truth] .proto files for gRPC
├── frontend/                 # Next.js Application
├── biz-service/              # Java Spring Boot Application
├── ai-engine/                # Python AI Framework & Agents
├── search-service/           # Go Crawler & Search Engine
├── infra/                    # Docker-compose, K8s configs, SQL inits
└── README.md
```

## 7. AI & Compliance Constraints

1.  **JSON Schema Output**: All AI Engine text outputs intended for system consumption must strictly validate against defined JSON Schemas. `Biz-Service` will discard invalid formats.
2.  **Provenance Tracking**: Any content generated by AI (RAG results, resume suggestions) MUST include a `provenance` field indicating the source (e.g., `{"source": "user_knowledge_base", "doc_id": "123"}` or `{"source": "llm_inference"}`).
3.  **No Direct DB Write**: AI Agents are prohibited from executing `INSERT/UPDATE` SQL commands. They must propose changes via gRPC or Kafka.
