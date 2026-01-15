-- V4__init_jobs.sql
-- Job Matching Tables (Phase 4)

-- Jobs table: Stores crawled job postings from various sources
CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(50) NOT NULL,
    external_id VARCHAR(255),
    url TEXT NOT NULL,
    url_hash VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    salary_min INTEGER,
    salary_max INTEGER,
    salary_currency VARCHAR(10) DEFAULT 'USD',
    employment_type VARCHAR(50),
    experience_level VARCHAR(50),
    description_markdown TEXT,
    requirements_json TEXT,
    posted_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    first_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_checked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    is_remote BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for jobs table
CREATE INDEX IF NOT EXISTS idx_jobs_source ON jobs(source);
CREATE INDEX IF NOT EXISTS idx_jobs_company ON jobs(company);
CREATE INDEX IF NOT EXISTS idx_jobs_location ON jobs(location);
CREATE INDEX IF NOT EXISTS idx_jobs_is_active ON jobs(is_active);
CREATE INDEX IF NOT EXISTS idx_jobs_first_seen_at ON jobs(first_seen_at);
CREATE INDEX IF NOT EXISTS idx_jobs_experience_level ON jobs(experience_level);

-- Job Embeddings table: Stores vector embeddings for semantic search
-- Note: In production with PostgreSQL, use pgvector extension
-- For H2 development, embeddings are stored as TEXT (JSON array format)
CREATE TABLE IF NOT EXISTS job_embeddings (
    job_id UUID PRIMARY KEY REFERENCES jobs(id) ON DELETE CASCADE,
    embedding_model VARCHAR(100) NOT NULL DEFAULT 'text-embedding-3-small',
    embedding_vector TEXT NOT NULL,
    embedding_dimension INTEGER NOT NULL DEFAULT 1536,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Job Matches table: Stores user-job match results
CREATE TABLE IF NOT EXISTS job_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    match_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    match_reason TEXT,
    matched_skills TEXT,
    missing_skills TEXT,
    is_viewed BOOLEAN DEFAULT false,
    is_saved BOOLEAN DEFAULT false,
    is_applied BOOLEAN DEFAULT false,
    applied_at TIMESTAMP WITH TIME ZONE,
    user_rating INTEGER CHECK (user_rating >= 1 AND user_rating <= 5),
    user_feedback TEXT,
    matched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    viewed_at TIMESTAMP WITH TIME ZONE,
    saved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_job_matches_user_job UNIQUE (user_id, job_id)
);

-- Indexes for job_matches table
CREATE INDEX IF NOT EXISTS idx_job_matches_user_id ON job_matches(user_id);
CREATE INDEX IF NOT EXISTS idx_job_matches_job_id ON job_matches(job_id);
CREATE INDEX IF NOT EXISTS idx_job_matches_score ON job_matches(match_score);
CREATE INDEX IF NOT EXISTS idx_job_matches_is_saved ON job_matches(is_saved);
CREATE INDEX IF NOT EXISTS idx_job_matches_is_applied ON job_matches(is_applied);
CREATE INDEX IF NOT EXISTS idx_job_matches_matched_at ON job_matches(matched_at);

-- ============================================
-- Production PostgreSQL with pgvector extension
-- Uncomment below for production deployment
-- ============================================
-- CREATE EXTENSION IF NOT EXISTS vector;
-- 
-- ALTER TABLE job_embeddings 
--     ALTER COLUMN embedding_vector TYPE vector(1536);
-- 
-- CREATE INDEX IF NOT EXISTS idx_job_embeddings_vector 
--     ON job_embeddings 
--     USING hnsw (embedding_vector vector_cosine_ops);

-- Insert some sample jobs for development
INSERT INTO jobs (id, source, url, url_hash, title, company, location, salary_min, salary_max, employment_type, experience_level, description_markdown, requirements_json, is_remote, is_active) VALUES
(gen_random_uuid(), 'LINKEDIN', 'https://example.com/job1', 'hash001', 'Senior Software Engineer', 'TechCorp Inc.', 'San Francisco, CA', 150000, 200000, 'FULL_TIME', 'SENIOR', 
 '## Senior Software Engineer\n\nWe are looking for an experienced software engineer to join our team.\n\n### Responsibilities\n- Design and implement scalable systems\n- Mentor junior developers\n- Lead technical initiatives\n\n### Requirements\n- 5+ years of experience\n- Strong Java/Python skills\n- Experience with cloud platforms',
 '{"tech": ["Java", "Python", "AWS", "Kubernetes", "PostgreSQL"], "soft": ["Leadership", "Communication"]}',
 true, true),

(gen_random_uuid(), 'INDEED', 'https://example.com/job2', 'hash002', 'Full Stack Developer', 'StartupXYZ', 'New York, NY', 120000, 160000, 'FULL_TIME', 'MID',
 '## Full Stack Developer\n\nJoin our fast-growing startup!\n\n### What youll do\n- Build features end-to-end\n- Work with modern tech stack\n- Collaborate with product team\n\n### Skills needed\n- React and Node.js experience\n- SQL and NoSQL databases\n- RESTful API design',
 '{"tech": ["React", "Node.js", "TypeScript", "MongoDB", "GraphQL"], "soft": ["Teamwork", "Problem-solving"]}',
 false, true),

(gen_random_uuid(), 'COMPANY_SITE', 'https://example.com/job3', 'hash003', 'Machine Learning Engineer', 'AI Labs', 'Remote', 180000, 250000, 'FULL_TIME', 'SENIOR',
 '## Machine Learning Engineer\n\nPush the boundaries of AI!\n\n### Your role\n- Develop ML models for production\n- Optimize model performance\n- Research new techniques\n\n### Must have\n- Deep learning expertise\n- Python and PyTorch/TensorFlow\n- Production ML experience',
 '{"tech": ["Python", "PyTorch", "TensorFlow", "Docker", "AWS"], "soft": ["Research", "Innovation"]}',
 true, true),

(gen_random_uuid(), 'GLASSDOOR', 'https://example.com/job4', 'hash004', 'DevOps Engineer', 'CloudScale', 'Seattle, WA', 140000, 180000, 'FULL_TIME', 'MID',
 '## DevOps Engineer\n\nScale our infrastructure!\n\n### Responsibilities\n- Manage cloud infrastructure\n- Implement CI/CD pipelines\n- Monitor and optimize systems\n\n### Requirements\n- Kubernetes and Docker\n- AWS or GCP experience\n- Infrastructure as Code',
 '{"tech": ["Kubernetes", "Docker", "Terraform", "AWS", "Jenkins"], "soft": ["Automation", "Monitoring"]}',
 false, true),

(gen_random_uuid(), 'LINKEDIN', 'https://example.com/job5', 'hash005', 'Junior Frontend Developer', 'WebDesign Co.', 'Austin, TX', 70000, 90000, 'FULL_TIME', 'JUNIOR',
 '## Junior Frontend Developer\n\nStart your career with us!\n\n### What youll learn\n- Modern React development\n- UI/UX best practices\n- Responsive design\n\n### We need\n- JavaScript/TypeScript basics\n- HTML/CSS proficiency\n- Eagerness to learn',
 '{"tech": ["JavaScript", "React", "HTML", "CSS", "Git"], "soft": ["Learning", "Creativity"]}',
 false, true);
