-- V1__init_users.sql
-- CVibe Database Schema - User Management

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- =====================================================
-- Users Table
-- =====================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    google_sub VARCHAR(255) UNIQUE,
    full_name VARCHAR(100),
    avatar_url TEXT,
    role VARCHAR(20) DEFAULT 'ROLE_USER' NOT NULL,
    enabled BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN')),
    CONSTRAINT chk_auth_method CHECK (password_hash IS NOT NULL OR google_sub IS NOT NULL)
);

-- Indexes for users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_google_sub ON users(google_sub) WHERE google_sub IS NOT NULL;
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_created_at ON users(created_at);

-- =====================================================
-- User AI Configurations Table
-- =====================================================
CREATE TABLE user_ai_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    base_url VARCHAR(500),
    api_key_encrypted TEXT,
    model_name VARCHAR(100),
    provider VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for user_ai_configs
CREATE INDEX idx_user_ai_configs_user_id ON user_ai_configs(user_id);

-- =====================================================
-- User Backups Table (for data export)
-- =====================================================
CREATE TABLE user_backups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    backup_type VARCHAR(50) NOT NULL,
    s3_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- Index for user_backups
CREATE INDEX idx_user_backups_user_id ON user_backups(user_id);
CREATE INDEX idx_user_backups_created_at ON user_backups(created_at);

-- =====================================================
-- Functions for updated_at trigger
-- =====================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to tables
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_ai_configs_updated_at
    BEFORE UPDATE ON user_ai_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- Insert default admin user (optional, for development)
-- Password: Admin@123 (BCrypt encoded)
-- =====================================================
-- INSERT INTO users (email, password_hash, full_name, role)
-- VALUES ('admin@cvibe.com', '$2a$10$N.QoEqVQnKXKSJgWyXjzZ.KM7H5z5zGJGZ5TnXGl/jZKQ/QQqMb6i', 'System Admin', 'ROLE_ADMIN');

COMMENT ON TABLE users IS 'User accounts for CVibe platform';
COMMENT ON TABLE user_ai_configs IS 'User custom AI API configurations (API keys encrypted)';
COMMENT ON TABLE user_backups IS 'User data export backups stored in S3';
