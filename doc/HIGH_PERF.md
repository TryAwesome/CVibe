# Go Service: High Performance Crawler & Search

> **Note to OpenCode:** This document specifies the design and implementation principles for the `/search-service` (Go). Focus on concurrency, resilience, and efficiency. This service is critical for data acquisition and should be robust against network failures and anti-bot measures.

## 1. Core Principles

*   **Concurrency**: Maximize parallelism using Go's goroutines and channels for I/O bound tasks (network requests).
*   **Efficiency**: Minimize resource consumption (CPU, Memory, Network Bandwidth) through optimized parsing and data structures.
*   **Robustness**: Implement comprehensive error handling, retry mechanisms, and graceful degradation under stress.
*   **Anti-Ban**: Proactive strategies to avoid detection and blocking by target websites.
*   **Observability**: Provide clear metrics and logging for monitoring crawl health and performance.

## 2. Architectural Components

The `/search-service` will primarily function as a Kafka consumer and a gRPC server.

### 2.1 `main.go`
*   Initializes all components (Kafka, gRPC, Redis, Logger).
*   Starts Kafka consumer group.
*   Starts gRPC server.
*   Handles graceful shutdown.

### 2.2 `kafka_consumer/`
*   **`consumer.go`**:
    *   Listens to `job.crawl_task` topic.
    *   Dispatches crawl tasks to the `CrawlerPool` using channels.
    *   Handles message offsets, commits, and DLQ for failed tasks.

### 2.3 `grpc_server/`
*   **`server.go`**:
    *   Implements the `career.job.v1.JobSearchService` defined in `/proto/job/v1/search.proto`.
    *   Provides RPC methods for `SearchJobs` (queries local cache/index) and potentially `GetJobDetails`.

### 2.4 `crawler_pool/`
*   **`pool.go`**:
    *   Manages a fixed number of worker goroutines (`CrawlerWorker`).
    *   Receives tasks from Kafka consumer and assigns to workers.
    *   Implements a circuit breaker pattern for domains that consistently fail.
*   **`worker.go`**:
    *   Fetches the target URL.
    *   Applies anti-ban strategies (proxy, user-agent).
    *   Passes raw HTML to `parser/`.
    *   Handles HTTP errors, retries.

### 2.5 `parser/`
*   **`parser.go`**:
    *   Receives raw HTML.
    *   Extracts structured job data (title, company, description, requirements) using CSS selectors or XPath.
    *   Uses a template-based parsing approach (e.g., per-site parsing rules).
    *   Outputs cleaned job data.

### 2.6 `deduplicator/`
*   **`deduplicator.go`**:
    *   Uses Redis to check if a URL has been crawled recently (`dedup:url:{hash}`).
    *   Prevents redundant crawling.

### 2.7 `proxy_manager/`
*   **`manager.go`**:
    *   Manages a pool of rotating proxies.
    *   Monitors proxy health and removes failed ones.
    *   Supports different proxy types (HTTP, SOCKS5).

## 3. Concurrency Model

*   **Kafka Consumer**: Multiple goroutines consuming partitions.
*   **Crawler Pool**: Fixed number of worker goroutines.
*   **Channels**: Used for task distribution (Kafka -> Pool) and result collection (Worker -> Parser -> Kafka Producer).
*   **Context**: Use `context.Context` for graceful shutdown and request timeouts propagation.

## 4. Anti-Ban Strategies

*   **Proxy Rotation**: Use `proxy_manager` to rotate IP addresses per request or per domain.
*   **User-Agent Rotation**: Maintain a list of common browser user-agents and rotate them randomly.
*   **Rate Limiting**:
    *   Per-domain rate limiting in `crawler_pool` (e.g., `token bucket` algorithm).
    *   Use Redis to store global rate limit states.
*   **Request Headers**: Mimic real browser headers (Accept, Accept-Language, etc.).
*   **Error Detection**: Analyze response status codes (403, 429) and HTML content (CAPTCHA, "Access Denied") to detect bans.

<h2>5. Data Processing Pipeline</h2>

1.  **Kafka `job.crawl_task`**: Consume task (URL).
2.  **Deduplicate**: Check Redis. If already crawled, skip.
3.  **Crawl**: Fetch URL with anti-ban.
4.  **Parse**: Extract data from raw HTML.
5.  **Produce to Kafka**:
    *   If successful: Produce cleaned JSON to `job.found`.
    *   If permanent failure (e.g., invalid URL, consistent ban): Produce to `job.crawl_task.dlq`.

<h2>6. Error Handling & Resilience</h2>

*   **Go-Idiomatic**: `if err != nil` is paramount.
*   **Retry Mechanisms**: Use `tenacity`-like logic for transient network errors (DNS, connection reset).
*   **Circuit Breaker**: Implement per-domain circuit breakers to prevent overwhelming unhealthy targets.
*   **Structured Logging**: Use `logrus` or similar, ensure `X-Trace-ID` is present in logs.
*   **Metrics**: Expose Prometheus metrics (`/metrics` endpoint) for crawl success rate, error rates, latency, active workers.

<h2>7. Integrations</h2>

*   **Kafka**: Producer for `job.found` topic, Consumer for `job.crawl_task`.
*   **Redis**: For deduplication (`dedup:url:{hash}`), rate limiting (`ratelimit:crawl:{domain}`), proxy health.
*   **gRPC**: As a server for `JobSearchService`. As a client to `Biz-Service` if necessary (e.g., reporting crawl statistics).

<h2>8. Development & Testing</h2>

*   **Unit Tests**: Extensive coverage for `parser`, `deduplicator`, `proxy_manager`.
*   **Integration Tests**: Test Kafka consumption and production, gRPC server functionality.
*   **End-to-End Tests**: Simulate a full crawl task from Kafka to `job.found` message production.
