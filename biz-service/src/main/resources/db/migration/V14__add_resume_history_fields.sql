-- V14: Add missing fields to resume_history table
-- These fields were added in the entity but missing from the original migration

-- Add status column for tracking parse state
ALTER TABLE resume_history
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING';

-- Add parsed_data column for storing AI-parsed JSON content
ALTER TABLE resume_history
ADD COLUMN IF NOT EXISTS parsed_data TEXT;

-- Add skills column for storing extracted skills as JSON array
ALTER TABLE resume_history
ADD COLUMN IF NOT EXISTS skills TEXT;

-- Add error_message column for storing parse failures
ALTER TABLE resume_history
ADD COLUMN IF NOT EXISTS error_message VARCHAR(500);

-- Add updated_at column for tracking modifications
ALTER TABLE resume_history
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

-- Create index on status for efficient filtering
CREATE INDEX IF NOT EXISTS idx_resume_history_status ON resume_history(status);
