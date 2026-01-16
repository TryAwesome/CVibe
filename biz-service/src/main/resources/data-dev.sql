-- Development seed data for H2
-- This file runs after schema creation when using dev profile
-- Note: Admin user is created by DevDataInitializer.java with proper BCrypt encoding

-- Clear existing question templates data (in case of restart)
DELETE FROM question_templates;

-- Insert default question templates
INSERT INTO question_templates (id, question_text, category, subcategory, question_type, difficulty_level, order_weight, is_required, extraction_hints, is_active, language, created_at) VALUES
-- Personal Info
(RANDOM_UUID(), 'What is your current job title and how long have you been in this role?', 'PERSONAL_INFO', 'current_position', 'OPEN_ENDED', 'BASIC', 10, true, '{"extract": ["job_title", "tenure"]}', true, 'en', NOW()),
(RANDOM_UUID(), 'What city and country are you currently based in?', 'PERSONAL_INFO', 'location', 'OPEN_ENDED', 'BASIC', 15, true, '{"extract": ["city", "country"]}', true, 'en', NOW()),

-- Work Experience
(RANDOM_UUID(), 'Tell me about your most recent work experience. What company did you work for and what were your main responsibilities?', 'WORK_EXPERIENCE', 'recent_role', 'OPEN_ENDED', 'STANDARD', 20, true, '{"extract": ["company", "role", "responsibilities", "dates"]}', true, 'en', NOW()),
(RANDOM_UUID(), 'What was your biggest achievement in this role? Can you quantify the impact?', 'WORK_EXPERIENCE', 'achievements', 'OPEN_ENDED', 'DETAILED', 25, false, '{"extract": ["achievement", "metrics", "impact"]}', true, 'en', NOW()),
(RANDOM_UUID(), 'What technologies, tools, or methodologies did you use in your day-to-day work?', 'WORK_EXPERIENCE', 'tech_stack', 'OPEN_ENDED', 'STANDARD', 30, false, '{"extract": ["technologies", "tools", "methodologies"]}', true, 'en', NOW()),
(RANDOM_UUID(), 'Can you describe a challenging project you worked on? What obstacles did you face and how did you overcome them?', 'WORK_EXPERIENCE', 'challenges', 'OPEN_ENDED', 'DEEP_DIVE', 35, false, '{"extract": ["project", "challenges", "solutions", "outcome"]}', true, 'en', NOW()),

-- Education
(RANDOM_UUID(), 'What is your highest level of education and where did you study?', 'EDUCATION', 'degree', 'OPEN_ENDED', 'BASIC', 50, true, '{"extract": ["degree", "institution", "graduation_year"]}', true, 'en', NOW()),
(RANDOM_UUID(), 'What was your major or field of study? Were there any relevant courses or projects?', 'EDUCATION', 'major', 'OPEN_ENDED', 'STANDARD', 55, false, '{"extract": ["major", "courses", "projects"]}', true, 'en', NOW()),

-- Skills
(RANDOM_UUID(), 'What are your top technical skills? Rate your proficiency level for each.', 'SKILLS', 'technical', 'OPEN_ENDED', 'STANDARD', 60, true, '{"extract": ["skills", "proficiency_levels"]}', true, 'en', NOW()),
(RANDOM_UUID(), 'What soft skills do you consider your strengths? Can you give examples?', 'SKILLS', 'soft_skills', 'OPEN_ENDED', 'STANDARD', 65, false, '{"extract": ["soft_skills", "examples"]}', true, 'en', NOW()),
(RANDOM_UUID(), 'Are there any skills you are currently learning or want to develop?', 'SKILLS', 'learning', 'OPEN_ENDED', 'BASIC', 70, false, '{"extract": ["learning_goals", "skills_to_develop"]}', true, 'en', NOW()),

-- Certifications
(RANDOM_UUID(), 'Do you have any professional certifications? When did you obtain them?', 'CERTIFICATIONS', 'professional', 'OPEN_ENDED', 'BASIC', 80, false, '{"extract": ["certifications", "dates", "issuing_organizations"]}', true, 'en', NOW()),

-- Projects
(RANDOM_UUID(), 'Tell me about a personal or side project you are proud of. What did you build and why?', 'PROJECTS', 'personal', 'OPEN_ENDED', 'STANDARD', 90, false, '{"extract": ["project_name", "description", "technologies", "outcome"]}', true, 'en', NOW()),

-- Career Goals
(RANDOM_UUID(), 'What type of role are you looking for in your next position?', 'CAREER_GOALS', 'target_role', 'OPEN_ENDED', 'BASIC', 100, true, '{"extract": ["target_role", "job_type", "industry"]}', true, 'en', NOW()),
(RANDOM_UUID(), 'What are your salary expectations?', 'CAREER_GOALS', 'compensation', 'OPEN_ENDED', 'BASIC', 105, false, '{"extract": ["salary_range", "currency"]}', true, 'en', NOW()),
(RANDOM_UUID(), 'Are you open to remote work, relocation, or specific locations?', 'CAREER_GOALS', 'location_preference', 'OPEN_ENDED', 'BASIC', 110, false, '{"extract": ["work_mode", "relocation", "preferred_locations"]}', true, 'en', NOW()),
(RANDOM_UUID(), 'What are your long-term career goals? Where do you see yourself in 5 years?', 'CAREER_GOALS', 'long_term', 'OPEN_ENDED', 'DETAILED', 115, false, '{"extract": ["career_goals", "timeline"]}', true, 'en', NOW());
