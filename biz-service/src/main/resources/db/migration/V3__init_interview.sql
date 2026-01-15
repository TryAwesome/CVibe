-- V3__init_interview.sql
-- Interview system tables

-- Question templates (admin-managed question bank)
CREATE TABLE question_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_text TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    subcategory VARCHAR(50),
    question_type VARCHAR(30) DEFAULT 'OPEN_ENDED',
    difficulty_level VARCHAR(20) DEFAULT 'STANDARD',
    expected_response_type VARCHAR(50),
    follow_up_prompts TEXT,
    extraction_hints TEXT,
    example_answer TEXT,
    order_weight INTEGER DEFAULT 100,
    is_required BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    target_roles TEXT,
    language VARCHAR(10) DEFAULT 'en',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Interview sessions
CREATE TABLE interview_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    current_question_index INTEGER DEFAULT 0,
    total_questions INTEGER DEFAULT 0,
    focus_area VARCHAR(50),
    target_role VARCHAR(100),
    session_metadata JSONB,
    extracted_data JSONB,
    extraction_status VARCHAR(20) DEFAULT 'PENDING',
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_interaction_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Interview answers
CREATE TABLE interview_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    question_template_id UUID REFERENCES question_templates(id),
    question_order INTEGER NOT NULL,
    question_text TEXT NOT NULL,
    answer_text TEXT,
    is_follow_up BOOLEAN DEFAULT false,
    follow_up_depth INTEGER DEFAULT 0,
    parent_answer_id UUID REFERENCES interview_answers(id),
    ai_analysis TEXT,
    extracted_entities JSONB,
    confidence_score DOUBLE PRECISION,
    needs_clarification BOOLEAN DEFAULT false,
    clarification_reason VARCHAR(255),
    answered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for performance
CREATE INDEX idx_question_templates_category ON question_templates(category);
CREATE INDEX idx_question_templates_active ON question_templates(is_active);
CREATE INDEX idx_question_templates_language ON question_templates(language);

CREATE INDEX idx_interview_sessions_user ON interview_sessions(user_id);
CREATE INDEX idx_interview_sessions_status ON interview_sessions(status);
CREATE INDEX idx_interview_sessions_type ON interview_sessions(session_type);

CREATE INDEX idx_interview_answers_session ON interview_answers(session_id);
CREATE INDEX idx_interview_answers_question_order ON interview_answers(session_id, question_order);

-- Insert default question templates
INSERT INTO question_templates (question_text, category, subcategory, question_type, difficulty_level, order_weight, is_required, extraction_hints) VALUES
-- Personal Info
('What is your current job title and how long have you been in this role?', 'PERSONAL_INFO', 'current_position', 'OPEN_ENDED', 'BASIC', 10, true, '{"extract": ["job_title", "tenure"]}'),
('What city and country are you currently based in?', 'PERSONAL_INFO', 'location', 'OPEN_ENDED', 'BASIC', 15, true, '{"extract": ["city", "country"]}'),

-- Work Experience
('Tell me about your most recent work experience. What company did you work for and what were your main responsibilities?', 'WORK_EXPERIENCE', 'recent_role', 'OPEN_ENDED', 'STANDARD', 20, true, '{"extract": ["company", "role", "responsibilities", "dates"]}'),
('What was your biggest achievement in this role? Can you quantify the impact?', 'WORK_EXPERIENCE', 'achievements', 'OPEN_ENDED', 'DETAILED', 25, false, '{"extract": ["achievement", "metrics", "impact"]}'),
('What technologies, tools, or methodologies did you use in your day-to-day work?', 'WORK_EXPERIENCE', 'tech_stack', 'OPEN_ENDED', 'STANDARD', 30, false, '{"extract": ["technologies", "tools", "methodologies"]}'),
('Can you describe a challenging project you worked on? What obstacles did you face and how did you overcome them?', 'WORK_EXPERIENCE', 'challenges', 'OPEN_ENDED', 'DEEP_DIVE', 35, false, '{"extract": ["project", "challenges", "solutions", "outcome"]}'),

-- Education
('What is your highest level of education and where did you study?', 'EDUCATION', 'degree', 'OPEN_ENDED', 'BASIC', 50, true, '{"extract": ["degree", "institution", "graduation_year"]}'),
('What was your major or field of study? Were there any relevant courses or projects?', 'EDUCATION', 'major', 'OPEN_ENDED', 'STANDARD', 55, false, '{"extract": ["major", "courses", "projects"]}'),

-- Skills
('What are your top technical skills? Rate your proficiency level for each.', 'SKILLS', 'technical', 'OPEN_ENDED', 'STANDARD', 60, true, '{"extract": ["skills", "proficiency_levels"]}'),
('What soft skills do you consider your strengths? Can you give examples?', 'SKILLS', 'soft_skills', 'OPEN_ENDED', 'STANDARD', 65, false, '{"extract": ["soft_skills", "examples"]}'),
('Are there any skills you are currently learning or want to develop?', 'SKILLS', 'learning', 'OPEN_ENDED', 'BASIC', 70, false, '{"extract": ["learning_goals", "skills_to_develop"]}'),

-- Certifications
('Do you have any professional certifications? When did you obtain them?', 'CERTIFICATIONS', 'professional', 'OPEN_ENDED', 'BASIC', 80, false, '{"extract": ["certifications", "dates", "issuing_organizations"]}'),

-- Projects
('Tell me about a personal or side project you are proud of. What did you build and why?', 'PROJECTS', 'personal', 'OPEN_ENDED', 'STANDARD', 90, false, '{"extract": ["project_name", "description", "technologies", "outcome"]}'),

-- Career Goals
('What type of role are you looking for in your next position?', 'CAREER_GOALS', 'target_role', 'OPEN_ENDED', 'BASIC', 100, true, '{"extract": ["target_role", "job_type", "industry"]}'),
('What are your salary expectations?', 'CAREER_GOALS', 'compensation', 'OPEN_ENDED', 'BASIC', 105, false, '{"extract": ["salary_range", "currency"]}'),
('Are you open to remote work, relocation, or specific locations?', 'CAREER_GOALS', 'location_preference', 'OPEN_ENDED', 'BASIC', 110, false, '{"extract": ["work_mode", "relocation", "preferred_locations"]}'),
('What are your long-term career goals? Where do you see yourself in 5 years?', 'CAREER_GOALS', 'long_term', 'OPEN_ENDED', 'DETAILED', 115, false, '{"extract": ["career_goals", "timeline"]}');
