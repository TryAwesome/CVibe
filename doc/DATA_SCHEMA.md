# Data Schema & Persistence Model

> **Note to OpenCode:** This document defines the storage structure for PostgreSQL, Redis, and Vector Indices. Changes here require database migrations.

## 1. PostgreSQL Schema (Biz-Service)

**Extension Required**: `vector` (pgvector), `uuid-ossp`.

### 1.1 Users & Auth
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    google_sub VARCHAR(255) UNIQUE NOT NULL, -- Google OAuth ID
    full_name VARCHAR(100),
    avatar_url TEXT,
    role VARCHAR(20) DEFAULT 'ROLE_USER', -- 'ROLE_USER', 'ROLE_ADMIN'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE user_backups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    backup_type VARCHAR(50), -- 'FULL_EXPORT', 'RESUME_SNAPSHOT'
    s3_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### 1.2 Resumes (The Core Asset)
We use a **Hybrid approach**:
*   `file_url`: The original PDF.
*   `content_json`: Structured data (Skills, Education) for UI rendering and Filtering.
*   `content_markdown`: Clean text for LLM Context.

```sql
CREATE TABLE resumes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(100), -- e.g., "Backend Engineer v1"
    version INT DEFAULT 1,
    
    -- Storage
    original_file_url TEXT,
    content_markdown TEXT,  -- For LLM context window
    content_json JSONB,     -- For UI Parsing & structured queries
                            -- Schema: { "skills": [], "education": [], "projects": [] }

    -- State
    status VARCHAR(20),     -- 'PROCESSING', 'PARSED', 'FAILED'
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for searching within JSON (e.g., find users with 'Python')
CREATE INDEX idx_resume_skills ON resumes USING gin ((content_json -> 'skills'));
```

### 1.3 Jobs (Market Data)
```sql
CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    url_hash VARCHAR(64) UNIQUE NOT NULL, -- SHA256 of URL for dedup
    source_url TEXT NOT NULL,
    
    -- Content
    title VARCHAR(255),
    company VARCHAR(255),
    location VARCHAR(100),
    raw_html_s3_url TEXT,   -- Offload heavy HTML to S3
    
    description_markdown TEXT, -- Cleaned description for Matching
    requirements_json JSONB,   -- { "years": 3, "tech": ["Go", "Kafka"] }
    
    -- Metadata
    first_seen_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_crawled_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE
);
```

## 2. Vector Store (pgvector)

**Embedding Model**: OpenAI `text-embedding-3-small` (Dimensions: **1536**).

### 2.1 Job Embeddings
Used for "Find jobs matching my resume".

```sql
CREATE TABLE job_embeddings (
    job_id UUID REFERENCES jobs(id) ON DELETE CASCADE,
    embedding vector(1536), -- Description embedding
    PRIMARY KEY (job_id)
);

-- HNSW Index for fast similarity search
CREATE INDEX idx_job_embedding ON job_embeddings 
USING hnsw (embedding vector_cosine_ops);
```

### 2.2 Resume Experience Embeddings
Used for "Find my best project for this job description" (RAG).
Instead of embedding the whole resume, we embed chunks (e.g., individual projects).

```sql
CREATE TABLE resume_chunks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    resume_id UUID REFERENCES resumes(id) ON DELETE CASCADE,
    chunk_text TEXT, -- e.g., "Project A: Built a high-frequency trading bot..."
    embedding vector(1536)
);

CREATE INDEX idx_resume_chunk_embedding ON resume_chunks 
USING hnsw (embedding vector_cosine_ops);
```

## 3. Redis Schema (Cache & State)

| Key Pattern | Type | TTL | Purpose |
| :--- | :--- | :--- | :--- |
| `sess:{session_id}` | String | 7 days | Stores JWT payload/User Session data. |
| `cache:job:{job_id}` | String | 1 hour | Cache hot job details for Frontend. |
| `ratelimit:crawl:{domain}` | Int | 1 min | Crawler rate limiting per domain. |
| `dedup:url:{hash}` | String | 24h | Bloom filter or simple key to prevent re-crawling same URL daily. |
| `chat:history:{user_id}` | List | 1 hour | Short-term conversation history for low-latency chat. |

<h2>4. Kafka Topic Schemas</h2>

*Note: Full JSON Schemas should be stored in `/doc/schemas/kafka/`.*

<h3><code>job.found</code> (Example Structure)</h3>
<pre><code class="language-json">{
  "url_hash": "a1b2...",
  "source": "linkedin",
  "data": {
    "title": "Senior Go Engineer",
    "description": "..."
  },
  "crawled_at": "2023-10-27T10:00:00Z"
}
</code></pre>
