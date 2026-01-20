-- V13: Add Profile Languages table

-- Languages (语言能力)
CREATE TABLE profile_languages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    language VARCHAR(50) NOT NULL,
    proficiency VARCHAR(30),  -- Native, Fluent, Professional, Basic
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profile_languages_profile_id ON profile_languages(profile_id);
CREATE UNIQUE INDEX idx_profile_languages_unique ON profile_languages(profile_id, language);
