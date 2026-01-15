-- V2: Profile and Resume related tables

-- User Profiles
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    headline VARCHAR(200),
    summary TEXT,
    current_title VARCHAR(100),
    current_company VARCHAR(100),
    location VARCHAR(100),
    years_of_experience INTEGER,
    phone VARCHAR(20),
    linkedin_url VARCHAR(255),
    github_url VARCHAR(255),
    portfolio_url VARCHAR(255),
    completeness_score INTEGER DEFAULT 0,
    last_interview_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);

-- Work Experience
CREATE TABLE profile_experiences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    company VARCHAR(100) NOT NULL,
    title VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    employment_type VARCHAR(50),
    start_date DATE NOT NULL,
    end_date DATE,
    is_current BOOLEAN DEFAULT FALSE,
    description TEXT,
    achievements TEXT,  -- JSON array
    technologies TEXT,  -- JSON array
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profile_experiences_profile_id ON profile_experiences(profile_id);
CREATE INDEX idx_profile_experiences_dates ON profile_experiences(start_date DESC, end_date);

-- Education
CREATE TABLE profile_educations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    institution VARCHAR(150) NOT NULL,
    degree VARCHAR(100),
    field_of_study VARCHAR(100),
    location VARCHAR(100),
    start_date DATE,
    end_date DATE,
    is_current BOOLEAN DEFAULT FALSE,
    gpa VARCHAR(20),
    activities TEXT,  -- JSON array
    honors TEXT,      -- JSON array
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profile_educations_profile_id ON profile_educations(profile_id);

-- Skills
CREATE TABLE profile_skills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    proficiency_level VARCHAR(20),
    years_of_experience INTEGER,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profile_skills_profile_id ON profile_skills(profile_id);
CREATE INDEX idx_profile_skills_category ON profile_skills(category);

-- Certifications
CREATE TABLE profile_certifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    issuing_organization VARCHAR(100),
    issue_date DATE,
    expiration_date DATE,
    credential_id VARCHAR(100),
    credential_url VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profile_certifications_profile_id ON profile_certifications(profile_id);

-- Projects
CREATE TABLE profile_projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    role VARCHAR(100),
    start_date DATE,
    end_date DATE,
    project_url VARCHAR(255),
    source_url VARCHAR(255),
    technologies TEXT,  -- JSON array
    highlights TEXT,    -- JSON array
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profile_projects_profile_id ON profile_projects(profile_id);

-- Resume History
CREATE TABLE resume_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255),
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    content_type VARCHAR(100),
    source VARCHAR(20),  -- UPLOADED, GENERATED, IMPORTED
    template_id UUID,
    target_job_title VARCHAR(100),
    target_company VARCHAR(100),
    version INTEGER DEFAULT 1,
    is_primary BOOLEAN DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resume_history_user_id ON resume_history(user_id);
CREATE INDEX idx_resume_history_created_at ON resume_history(created_at DESC);

-- Resume Templates (for admin to manage)
CREATE TABLE resume_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    preview_url VARCHAR(255),
    template_path VARCHAR(500) NOT NULL,  -- LaTeX template file path
    category VARCHAR(50),  -- PROFESSIONAL, CREATIVE, ACADEMIC, etc.
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_resume_templates_active ON resume_templates(is_active, sort_order);
