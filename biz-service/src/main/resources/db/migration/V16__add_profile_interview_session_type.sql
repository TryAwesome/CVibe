-- Add PROFILE_INTERVIEW to session_type constraint
-- This migration adds the new session type for AI-powered profile collection interviews

-- Drop existing constraint
ALTER TABLE interview_sessions DROP CONSTRAINT IF EXISTS interview_sessions_session_type_check;

-- Add new constraint with PROFILE_INTERVIEW included
ALTER TABLE interview_sessions ADD CONSTRAINT interview_sessions_session_type_check
    CHECK (session_type IN ('INITIAL_PROFILE', 'DEEP_DIVE', 'PROFILE_INTERVIEW', 'MOCK_INTERVIEW'));
