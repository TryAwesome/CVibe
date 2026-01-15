-- V10__init_analytics.sql
-- Analytics & Insights 数据分析模块

-- 用户活动记录表
CREATE TABLE user_activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    category VARCHAR(30) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    session_id VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    device_type VARCHAR(20),
    page_path VARCHAR(255),
    referrer VARCHAR(500),
    duration_seconds BIGINT,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 分析事件表
CREATE TABLE analytics_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_name VARCHAR(100) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    event_source VARCHAR(30) DEFAULT 'WEB',
    event_value DECIMAL(15, 4),
    user_id UUID,
    session_id VARCHAR(100),
    properties TEXT,
    dimensions TEXT,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 每日统计表
CREATE TABLE daily_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stat_date DATE NOT NULL,
    stat_type VARCHAR(30) NOT NULL,
    stat_key VARCHAR(100) NOT NULL,
    count_value BIGINT DEFAULT 0,
    sum_value DECIMAL(15, 4),
    avg_value DECIMAL(15, 4),
    min_value DECIMAL(15, 4),
    max_value DECIMAL(15, 4),
    change_percent DECIMAL(10, 2),
    dimensions TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_daily_stats UNIQUE (stat_date, stat_type, stat_key)
);

-- 索引优化
-- user_activity 索引
CREATE INDEX idx_activity_user ON user_activity(user_id);
CREATE INDEX idx_activity_type ON user_activity(activity_type);
CREATE INDEX idx_activity_created ON user_activity(created_at);
CREATE INDEX idx_activity_user_type ON user_activity(user_id, activity_type);
CREATE INDEX idx_activity_user_created ON user_activity(user_id, created_at);
CREATE INDEX idx_activity_session ON user_activity(session_id);
CREATE INDEX idx_activity_category ON user_activity(category);
CREATE INDEX idx_activity_page ON user_activity(page_path);
CREATE INDEX idx_activity_device ON user_activity(device_type);

-- analytics_event 索引
CREATE INDEX idx_event_name ON analytics_event(event_name);
CREATE INDEX idx_event_type ON analytics_event(event_type);
CREATE INDEX idx_event_created ON analytics_event(created_at);
CREATE INDEX idx_event_user ON analytics_event(user_id);
CREATE INDEX idx_event_processed ON analytics_event(processed);

-- daily_stats 索引
CREATE INDEX idx_stats_date ON daily_stats(stat_date);
CREATE INDEX idx_stats_type ON daily_stats(stat_type);
CREATE INDEX idx_stats_type_key ON daily_stats(stat_type, stat_key);
CREATE INDEX idx_stats_date_type ON daily_stats(stat_date, stat_type);

-- 触发器: 更新 daily_stats 的 updated_at
CREATE OR REPLACE FUNCTION update_daily_stats_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_daily_stats_update
    BEFORE UPDATE ON daily_stats
    FOR EACH ROW
    EXECUTE FUNCTION update_daily_stats_timestamp();

-- 初始化一些基础统计类型的说明 (作为注释)
COMMENT ON TABLE user_activity IS '用户活动追踪表，记录用户在平台上的所有行为';
COMMENT ON TABLE analytics_event IS '分析事件表，记录平台级别的事件数据';
COMMENT ON TABLE daily_stats IS '每日统计表，预聚合的每日数据用于快速报表生成';

COMMENT ON COLUMN user_activity.activity_type IS '活动类型: LOGIN, LOGOUT, RESUME_VIEW, INTERVIEW_START等';
COMMENT ON COLUMN user_activity.category IS '活动类别: AUTH, RESUME, INTERVIEW, JOB, COMMUNITY, GROWTH, ADMIN';
COMMENT ON COLUMN user_activity.device_type IS '设备类型: DESKTOP, MOBILE, TABLET, UNKNOWN';

COMMENT ON COLUMN analytics_event.event_type IS '事件类型: USER, SYSTEM, BUSINESS, PERFORMANCE, ERROR, CONVERSION';
COMMENT ON COLUMN analytics_event.event_source IS '事件来源: WEB, MOBILE_IOS, MOBILE_ANDROID, API, INTERNAL';

COMMENT ON COLUMN daily_stats.stat_type IS '统计类型: USER, ENGAGEMENT, CONTENT, BUSINESS, PERFORMANCE, RETENTION';
