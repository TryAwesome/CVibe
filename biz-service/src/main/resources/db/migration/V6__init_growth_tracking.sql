-- V6: Initialize Growth Tracking Tables
-- Career growth goals, skill gap analysis, and learning paths

-- Growth Goals Table
CREATE TABLE IF NOT EXISTS growth_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_role VARCHAR(255) NOT NULL,
    target_company VARCHAR(255),
    target_level VARCHAR(50),
    job_requirements TEXT,
    jd_file_path VARCHAR(512),
    target_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    progress_percent INT DEFAULT 0,
    match_score DOUBLE PRECISION,
    analysis_summary TEXT,
    last_analyzed_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Skill Gaps Table
CREATE TABLE IF NOT EXISTS skill_gaps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES growth_goals(id) ON DELETE CASCADE,
    skill_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    current_level INT DEFAULT 0,
    required_level INT DEFAULT 100,
    priority VARCHAR(50) DEFAULT 'MEDIUM',
    status VARCHAR(50) DEFAULT 'IDENTIFIED',
    is_required BOOLEAN DEFAULT FALSE,
    is_preferred BOOLEAN DEFAULT FALSE,
    estimated_hours INT,
    recommendation TEXT,
    learning_resources TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Learning Paths Table
CREATE TABLE IF NOT EXISTS learning_paths (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES growth_goals(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    focus VARCHAR(100),
    difficulty VARCHAR(50) DEFAULT 'INTERMEDIATE',
    estimated_hours INT,
    target_date DATE,
    status VARCHAR(50) DEFAULT 'NOT_STARTED',
    sort_order INT DEFAULT 0,
    completion_percent INT DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Learning Milestones Table
CREATE TABLE IF NOT EXISTS learning_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    learning_path_id UUID NOT NULL REFERENCES learning_paths(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(100) DEFAULT 'TASK',
    estimated_hours INT,
    resource_url VARCHAR(1024),
    sort_order INT DEFAULT 0,
    is_completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for Growth Goals
CREATE INDEX IF NOT EXISTS idx_growth_goals_user_id ON growth_goals(user_id);
CREATE INDEX IF NOT EXISTS idx_growth_goals_user_active ON growth_goals(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_growth_goals_status ON growth_goals(status);
CREATE INDEX IF NOT EXISTS idx_growth_goals_target_role ON growth_goals(target_role);

-- Indexes for Skill Gaps
CREATE INDEX IF NOT EXISTS idx_skill_gaps_goal_id ON skill_gaps(goal_id);
CREATE INDEX IF NOT EXISTS idx_skill_gaps_priority ON skill_gaps(priority);
CREATE INDEX IF NOT EXISTS idx_skill_gaps_status ON skill_gaps(status);
CREATE INDEX IF NOT EXISTS idx_skill_gaps_category ON skill_gaps(category);

-- Indexes for Learning Paths
CREATE INDEX IF NOT EXISTS idx_learning_paths_goal_id ON learning_paths(goal_id);
CREATE INDEX IF NOT EXISTS idx_learning_paths_status ON learning_paths(status);

-- Indexes for Learning Milestones
CREATE INDEX IF NOT EXISTS idx_learning_milestones_path_id ON learning_milestones(learning_path_id);
CREATE INDEX IF NOT EXISTS idx_learning_milestones_completed ON learning_milestones(is_completed);

-- Sample Data: Common skill gap templates for quick start
INSERT INTO growth_goals (id, user_id, target_role, target_company, target_level, job_requirements, is_active, status, progress_percent)
SELECT 
    gen_random_uuid(),
    (SELECT id FROM users LIMIT 1),
    'Senior Software Engineer',
    'Google',
    'SENIOR',
    'Required: Java, Python, System Design, Distributed Systems, Kubernetes, AWS, CI/CD. Preferred: Machine Learning, Kafka.',
    TRUE,
    'ACTIVE',
    0
WHERE EXISTS (SELECT 1 FROM users LIMIT 1);
