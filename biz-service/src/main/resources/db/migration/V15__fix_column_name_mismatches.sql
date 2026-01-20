-- V15: Fix column name mismatches between entities and database
-- These inconsistencies were discovered during code review

-- ==================== profile_educations ====================
-- Entity uses 'school', but V2 migration created 'institution'
-- Rename column to match entity (Hibernate expects 'school' by default)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'profile_educations' AND column_name = 'institution')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'profile_educations' AND column_name = 'school') THEN
        ALTER TABLE profile_educations RENAME COLUMN institution TO school;
    END IF;
END $$;

-- ==================== profile_projects ====================
-- Entity uses 'url', but V2 migration created 'project_url'
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'profile_projects' AND column_name = 'project_url')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'profile_projects' AND column_name = 'url') THEN
        ALTER TABLE profile_projects RENAME COLUMN project_url TO url;
    END IF;
END $$;

-- Entity uses 'repo_url', but V2 migration created 'source_url'
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'profile_projects' AND column_name = 'source_url')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'profile_projects' AND column_name = 'repo_url') THEN
        ALTER TABLE profile_projects RENAME COLUMN source_url TO repo_url;
    END IF;
END $$;

-- Entity has 'is_current' field, but V2 migration didn't create it
ALTER TABLE profile_projects
ADD COLUMN IF NOT EXISTS is_current BOOLEAN DEFAULT FALSE;

-- Entity has 'highlights' field, but V2 migration didn't create it
ALTER TABLE profile_projects
ADD COLUMN IF NOT EXISTS highlights TEXT;

-- ==================== profile_educations (additional) ====================
-- Entity has 'description' field, but V2 migration may not have it
ALTER TABLE profile_educations
ADD COLUMN IF NOT EXISTS description TEXT;

-- Entity has 'is_current' field
ALTER TABLE profile_educations
ADD COLUMN IF NOT EXISTS is_current BOOLEAN DEFAULT FALSE;

-- ==================== profile_skills ====================
-- Entity uses 'level', but V2 migration created 'proficiency_level'
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'profile_skills' AND column_name = 'proficiency_level')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'profile_skills' AND column_name = 'level') THEN
        ALTER TABLE profile_skills RENAME COLUMN proficiency_level TO level;
    END IF;
END $$;
