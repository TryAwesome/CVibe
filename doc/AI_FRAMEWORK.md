# AI Engine: Custom Agent Orchestration Framework

> **Note to OpenCode:** This document outlines the architecture and principles for the **custom Python AI orchestration framework**. Strict adherence to these guidelines is crucial. **DO NOT introduce heavy, opinionated AI frameworks** like LangChain, LlamaIndex, Haystack, etc. Focus on building modular, explicit components using core Python and specified libraries.

## 1. Core Principles

*   **Modularity**: Each component (Planner, Executor, Memory, Tools) should be a distinct, testable unit.
*   **Explicitness**: State management and agent "thought process" should be transparent and traceable.
*   **Separation of Concerns**: Clearly define responsibilities for planning, executing, tool usage, and memory management.
*   **LLM Agnostic (within limits)**: Design to allow swapping different LLMs (OpenAI, Gemini) with minimal code changes.
*   **Efficiency**: Optimize for token usage and latency.

## 2. Architectural Components

The framework will consist of the following key modules and classes in `/ai-engine/`:

### 2.1 `agent_core/`
*   **`base_agent.py`**:
    *   An abstract base class (`abc.ABC`) defining the core interface for any agent.
    *   Methods: `plan(user_input: str, context: dict) -> AgentPlan`, `execute(plan: AgentPlan) -> AgentOutput`.
    *   Properties: `tool_registry: ToolRegistry`, `memory_manager: MemoryManager`.
*   **`orchestrator.py`**:
    *   Manages the lifecycle of an agent instance (loading, execution flow).
    *   Responsible for initializing agents, feeding input, and processing output.
    *   Can coordinate multiple agents for complex tasks.
*   **`agent_types/`**:
    *   Specific agent implementations, inheriting from `BaseAgent`.
    *   `ResumeAnalyzerAgent`: Specializes in parsing resumes, identifying skills.
    *   `JobMatcherAgent`: Matches resume to job descriptions.
    *   `CareerCoachAgent`: Provides gap analysis and learning paths.

### 2.3 `tool_management/`
*   **`tool_registry.py`**:
    *   A central registry for all available tools (e.g., `SearchService.search_jobs`, `Database.update_resume`).
    *   Tools are functions or methods with clear input/output JSON schemas.
    *   Provides introspection for LLM to select tools.
*   **`tools.py`**:
    *   Concrete implementations of tools.
    *   Example: `grpc_client.py` (for interacting with Biz/Search Services), `redis_client.py`.

### 2.3 `memory_management/`
*   **`memory_manager.py`**:
    *   Abstract base class for memory components.
    *   Methods: `load_context(user_id: UUID) -> dict`, `save_context(user_id: UUID, context: dict)`.
    *   Manages short-term conversation history (Redis) and long-term knowledge base (PostgreSQL via Biz Service).
*   **`redis_memory.py`**:
    *   Concrete implementation using Redis for short-term conversation and ephemeral data.
*   **`kb_manager.py`**:
    *   Manages interaction with the Knowledge Base (e.g., fetching resume chunks, job embeddings from PostgreSQL via gRPC to Biz-Service).

### 2.4 `prompt_management/`
*   **`prompt_manager.py`**:
    *   Centralizes prompt templates (Jinja2 or F-strings).
    *   Ensures consistent system prompts, tool instructions, and few-shot examples.
    *   Manages versioning of prompts.
*   **`prompt_templates/`**:
    *   Directory for `.txt` or `.json` files containing various prompt templates.

## 3. Forbidden & Allowed Libraries

### 3.1 Forbidden Libraries
*   LangChain, LlamaIndex, Haystack, Semantic Kernel, CrewAI, Autogen.
    *   *Reason*: These frameworks introduce too much abstraction and opinionated design that conflicts with our custom framework's goals.

### 3.2 Allowed Libraries
*   **Core Python**: `asyncio`, `json`, `logging`, `abc`.
*   **LLM Integration**: `openai`, `google.generativeai` (or similar direct client SDKs).
*   **Data Validation**: `Pydantic` (for input/output schemas).
*   **Async HTTP**: `httpx`.
*   **gRPC**: `grpcio`, `grpcio-tools`.
*   **Kafka**: `confluent-kafka-python`.
*   **Redis**: `redis-py`.
*   **Vector Operations**: `numpy`, `scipy` (if needed for custom distance metrics).
*   **Utility**: `tenacity` (for retries).

## 4. Key Integrations & Data Flow

### 4.1 LLM Interaction
*   Direct API calls to OpenAI/Gemini endpoints.
*   Use `Pydantic` to define expected LLM output formats.

### 4.2 External Service Communication
*   **gRPC Client**: Interact with `Biz-Service` (for DB access, user data) and `Search-Service` (for job crawling, job data).
*   **Kafka Producer/Consumer**: Consume `resume.uploaded`, produce `job.crawl_task`.

### 4.3 RAG Strategy (Retrieval Augmented Generation)
1.  **Resume Parsing**: On `resume.uploaded` event, `ResumeAnalyzerAgent` parses `content_markdown`.
2.  **Chunking**: Breaks down resume into `resume_chunks` (e.g., per project, per experience item).
3.  **Embedding**: Embeds each chunk using `text-embedding-3-small`.
4.  **Storage**: Sends embeddings (via gRPC to Biz-Service) to `pgvector` (`resume_chunks` table).
5.  **Retrieval**: When generating a tailored resume, `JobMatcherAgent` queries `pgvector` for top-k relevant `resume_chunks` based on job description embedding.

<h2>5. AI & Data Constraints</h2>

*   **JSON Schema Output**: All AI Agent outputs intended for system processing MUST conform to defined JSON Schemas (Pydantic models).
*   **Provenance**: Every AI-generated piece of information or recommendation MUST include a `provenance` field.
*   **No Direct DB Writes**: AI Engine is forbidden from directly writing to PostgreSQL. All persistence operations (saving embeddings, updating resume status) must be delegated to `Biz-Service` via gRPC.
