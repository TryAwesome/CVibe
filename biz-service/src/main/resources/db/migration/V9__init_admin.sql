-- V9: Admin Module Tables
-- Audit logs, system configuration, and announcements

-- ================== Audit Logs ==================

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100),
    entity_id UUID,
    description TEXT,
    old_values TEXT,
    new_values TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_path VARCHAR(500),
    request_method VARCHAR(10),
    status VARCHAR(20) DEFAULT 'SUCCESS',
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_status ON audit_logs(status);
CREATE INDEX idx_audit_logs_ip_address ON audit_logs(ip_address);

-- ================== System Configs ==================

CREATE TABLE system_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(50) NOT NULL,
    config_key VARCHAR(255) NOT NULL UNIQUE,
    config_value TEXT,
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    default_value TEXT,
    description VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    is_sensitive BOOLEAN DEFAULT FALSE,
    is_editable BOOLEAN DEFAULT TRUE,
    modified_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_system_configs_category ON system_configs(category);
CREATE UNIQUE INDEX idx_system_configs_key ON system_configs(config_key);

-- Insert default configurations
INSERT INTO system_configs (category, config_key, config_value, value_type, description, is_editable) VALUES
    ('GENERAL', 'app.name', 'CVibe', 'STRING', 'Application name', FALSE),
    ('GENERAL', 'app.version', '1.0.0', 'STRING', 'Application version', FALSE),
    ('GENERAL', 'app.maintenance_mode', 'false', 'BOOLEAN', 'Enable maintenance mode', TRUE),
    
    ('SECURITY', 'security.max_login_attempts', '5', 'INTEGER', 'Maximum login attempts before lockout', TRUE),
    ('SECURITY', 'security.lockout_duration_minutes', '30', 'INTEGER', 'Account lockout duration in minutes', TRUE),
    ('SECURITY', 'security.password_min_length', '8', 'INTEGER', 'Minimum password length', TRUE),
    ('SECURITY', 'security.session_timeout_minutes', '60', 'INTEGER', 'Session timeout in minutes', TRUE),
    
    ('AI', 'ai.interview_max_questions', '10', 'INTEGER', 'Maximum questions per AI interview', TRUE),
    ('AI', 'ai.resume_analysis_enabled', 'true', 'BOOLEAN', 'Enable AI resume analysis', TRUE),
    ('AI', 'ai.mock_interview_enabled', 'true', 'BOOLEAN', 'Enable AI mock interview', TRUE),
    
    ('RATE_LIMIT', 'rate_limit.api_calls_per_minute', '60', 'INTEGER', 'API calls allowed per minute', TRUE),
    ('RATE_LIMIT', 'rate_limit.ai_calls_per_day', '50', 'INTEGER', 'AI API calls allowed per day', TRUE),
    
    ('FEATURE_FLAG', 'feature.community_enabled', 'true', 'BOOLEAN', 'Enable community features', TRUE),
    ('FEATURE_FLAG', 'feature.job_matching_enabled', 'true', 'BOOLEAN', 'Enable job matching features', TRUE),
    ('FEATURE_FLAG', 'feature.growth_tracking_enabled', 'true', 'BOOLEAN', 'Enable growth tracking features', TRUE),
    
    ('NOTIFICATION', 'notification.email_enabled', 'true', 'BOOLEAN', 'Enable email notifications', TRUE),
    ('NOTIFICATION', 'notification.push_enabled', 'false', 'BOOLEAN', 'Enable push notifications', TRUE);

-- ================== Announcements ==================

CREATE TABLE announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    announcement_type VARCHAR(30) NOT NULL DEFAULT 'INFO',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    target_audience VARCHAR(30) NOT NULL DEFAULT 'ALL',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    is_pinned BOOLEAN DEFAULT FALSE,
    is_dismissible BOOLEAN DEFAULT TRUE,
    link_url VARCHAR(500),
    link_text VARCHAR(100),
    view_count INTEGER DEFAULT 0,
    dismiss_count INTEGER DEFAULT 0,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_announcements_status ON announcements(status);
CREATE INDEX idx_announcements_priority ON announcements(priority);
CREATE INDEX idx_announcements_start_time ON announcements(start_time);
CREATE INDEX idx_announcements_end_time ON announcements(end_time);
CREATE INDEX idx_announcements_type ON announcements(announcement_type);
CREATE INDEX idx_announcements_target ON announcements(target_audience);

-- ================== User Announcement Dismissals ==================
-- Track which users have dismissed which announcements

CREATE TABLE user_announcement_dismissals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    announcement_id UUID NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    dismissed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    UNIQUE(user_id, announcement_id)
);

CREATE INDEX idx_user_announcement_user ON user_announcement_dismissals(user_id);
CREATE INDEX idx_user_announcement_announcement ON user_announcement_dismissals(announcement_id);

-- ================== Comments ==================
-- Add trigger for updated_at on system_configs

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_system_configs_updated_at
    BEFORE UPDATE ON system_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_announcements_updated_at
    BEFORE UPDATE ON announcements
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
