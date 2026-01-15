-- V7: Initialize Mock Interview Tables
-- Mock interview sessions, rounds, questions, and answers

-- Mock Interviews Table
CREATE TABLE IF NOT EXISTS mock_interviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_position VARCHAR(255) NOT NULL,
    target_company VARCHAR(255),
    interview_type VARCHAR(50) NOT NULL,
    difficulty VARCHAR(50) DEFAULT 'MEDIUM',
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    total_questions INT DEFAULT 5,
    answered_questions INT DEFAULT 0,
    overall_score INT,
    technical_score INT,
    communication_score INT,
    problem_solving_score INT,
    feedback_summary TEXT,
    strengths TEXT,
    improvements TEXT,
    skills TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_seconds INT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Interview Rounds Table
CREATE TABLE IF NOT EXISTS interview_rounds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id UUID NOT NULL REFERENCES mock_interviews(id) ON DELETE CASCADE,
    round_number INT NOT NULL,
    round_name VARCHAR(255),
    round_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    question_count INT DEFAULT 0,
    score INT,
    feedback TEXT,
    duration_seconds INT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Mock Questions Table
CREATE TABLE IF NOT EXISTS mock_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id UUID NOT NULL REFERENCES mock_interviews(id) ON DELETE CASCADE,
    round_id UUID REFERENCES interview_rounds(id) ON DELETE SET NULL,
    question_number INT NOT NULL,
    category VARCHAR(100) NOT NULL,
    difficulty VARCHAR(50) DEFAULT 'MEDIUM',
    question_text TEXT NOT NULL,
    follow_up_question TEXT,
    expected_points TEXT,
    sample_answer TEXT,
    related_skill VARCHAR(255),
    time_limit_seconds INT,
    is_answered BOOLEAN DEFAULT FALSE,
    is_skipped BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Mock Answers Table
CREATE TABLE IF NOT EXISTS mock_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL UNIQUE REFERENCES mock_questions(id) ON DELETE CASCADE,
    answer_text TEXT NOT NULL,
    code_answer TEXT,
    programming_language VARCHAR(50),
    time_taken_seconds INT,
    started_at TIMESTAMP,
    submitted_at TIMESTAMP,
    is_evaluated BOOLEAN DEFAULT FALSE,
    score INT,
    accuracy_score INT,
    completeness_score INT,
    clarity_score INT,
    relevance_score INT,
    feedback TEXT,
    strengths TEXT,
    improvements TEXT,
    covered_points TEXT,
    missed_points TEXT,
    suggested_answer TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for Mock Interviews
CREATE INDEX IF NOT EXISTS idx_mock_interviews_user_id ON mock_interviews(user_id);
CREATE INDEX IF NOT EXISTS idx_mock_interviews_status ON mock_interviews(status);
CREATE INDEX IF NOT EXISTS idx_mock_interviews_type ON mock_interviews(interview_type);
CREATE INDEX IF NOT EXISTS idx_mock_interviews_created_at ON mock_interviews(created_at);

-- Indexes for Interview Rounds
CREATE INDEX IF NOT EXISTS idx_interview_rounds_interview_id ON interview_rounds(interview_id);
CREATE INDEX IF NOT EXISTS idx_interview_rounds_type ON interview_rounds(round_type);

-- Indexes for Mock Questions
CREATE INDEX IF NOT EXISTS idx_mock_questions_interview_id ON mock_questions(interview_id);
CREATE INDEX IF NOT EXISTS idx_mock_questions_round_id ON mock_questions(round_id);
CREATE INDEX IF NOT EXISTS idx_mock_questions_category ON mock_questions(category);
CREATE INDEX IF NOT EXISTS idx_mock_questions_difficulty ON mock_questions(difficulty);

-- Indexes for Mock Answers
CREATE INDEX IF NOT EXISTS idx_mock_answers_question_id ON mock_answers(question_id);
CREATE INDEX IF NOT EXISTS idx_mock_answers_score ON mock_answers(score);
