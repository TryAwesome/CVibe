# Testing & Quality Assurance Strategy

> **Note to OpenCode:** This document outlines the testing strategy, tools, and quality gates for the CareerAgent system. The goal is to ensure high reliability across all microservices and the frontend through a "Testing Pyramid" approach.

## 1. Testing Pyramid Strategy

We prioritize tests that are fast and stable (Unit Tests) over those that are slow and flaky (E2E Tests).

### 1.1 Unit Tests (70%)
*   **Scope**: Individual functions, classes, and components in isolation.
*   **Tools**:
    *   **Frontend**: `Vitest` + `React Testing Library`.
    *   **Java (Biz)**: `JUnit 5` + `Mockito`.
    *   **Go (Search)**: Standard `testing` package + `testify`.
    *   **Python (AI)**: `pytest`.
*   **Requirement**: Minimum 80% code coverage for core business logic.

### 1.2 Integration Tests (20%)
*   **Scope**: Interactions between modules within a service or with external resources (DB, Redis) using Docker containers.
*   **Tools**:
    *   **Java**: `Testcontainers` (PostgreSQL, Kafka).
    *   **Go**: `dockertest` or standard test with real DB connections.
*   **Focus**: API endpoints, Repository queries, Message parsing.

### 1.3 Contract Tests (5%)
*   **Scope**: Verifying gRPC contracts and Kafka schemas between services.
*   **Tool**: `Pact` (optional) or automated compatibility checks in CI.
*   **Goal**: Ensure `Biz-Service` changes don't break `Frontend` or `AI-Engine`.

### 1.4 End-to-End (E2E) Tests (5%)
*   **Scope**: Full user journeys from UI to Backend to DB.
*   **Tool**: `Playwright` (TypeScript).
*   **Critical Flows**:
    *   User Login -> Dashboard Load.
    *   Resume Upload -> AI Analysis -> Result Display.
    *   Job Search -> Filter -> Detail View.

## 2. AI Evaluation (LLM Ops)

Testing AI features requires a different approach than deterministic code.

*   **Golden Dataset**: Maintain a set of curated inputs (e.g., sample resumes, job descriptions) and expected outputs.
*   **Automated Eval**:
    *   **RAG Precision**: Measure if the correct resume chunks are retrieved for a given job query.
    *   **Output Quality**: Use a stronger LLM (e.g., GPT-4) to grade the output of the production model (e.g., "Is this resume suggestion relevant?").
    *   **Safety Checks**: Automated adversarial testing (red-teaming) to ensure the AI refuses to generate harmful content.

## 3. Performance & Load Testing

*   **Tool**: `k6` (JavaScript-based load testing).
*   **Scenarios**:
    *   **Spike Test**: Simulate sudden traffic surge (e.g., marketing campaign).
    *   **Soak Test**: Run moderate load for 24 hours to find memory leaks.
    *   **Stress Test**: Determine the breaking point of the `Search-Service` and `Biz-Service`.
*   **Targets**:
    *   Resume Upload endpoint.
    *   Job Search API (Cached vs Uncached).
    *   AI Chat latency (Time to First Token).

## 4. Continuous Integration (CI) Pipeline

Every Pull Request (PR) triggers the following checks:

1.  **Linting**: `ESLint` (JS), `Checkstyle` (Java), `golangci-lint` (Go), `Ruff` (Python).
2.  **Unit Tests**: Run all unit tests in parallel.
3.  **Build**: Ensure all Docker images build successfully.
4.  **Security Scan**: Run `trivy` on dependencies.

**Blocker**: PRs cannot be merged if any check fails or coverage drops below threshold.

<h2>5. Quality Standards for Code</h2>

*   **Clean Code**: Follow standard style guides (Airbnb for JS, Google for Java/Go).
*   **Comments**: Explain "Why", not "What". Public APIs must have Javadoc/GoDoc.
*   **Error Messages**: User-facing errors must be localized and friendly; system logs must be detailed.

<h2>6. Manual QA Process</h2>

*   **Exploratory Testing**: Manual testing of new features to find edge cases automated tests missed.
*   **Usability Testing**: Internal "Dogfooding" to ensure the UX is intuitive (e.g., is the chat stream too slow?).
