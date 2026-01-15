-- V11: Notification tables
-- 通知系统表结构

-- 通知表
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL,
    category VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    title VARCHAR(200) NOT NULL,
    content TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    
    -- 动作相关
    action_url VARCHAR(500),
    action_text VARCHAR(100),
    
    -- 关联实体
    entity_type VARCHAR(50),
    entity_id UUID,
    
    -- 发送者
    sender_id UUID REFERENCES users(id) ON DELETE SET NULL,
    
    -- 附加信息
    image_url VARCHAR(500),
    metadata JSONB,
    
    -- 状态
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    is_clicked BOOLEAN NOT NULL DEFAULT FALSE,
    clicked_at TIMESTAMP WITH TIME ZONE,
    is_dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    dismissed_at TIMESTAMP WITH TIME ZONE,
    
    -- 投递渠道
    channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    is_sent BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMP WITH TIME ZONE,
    
    -- 过期时间
    expires_at TIMESTAMP WITH TIME ZONE,
    
    -- 时间戳
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 通知偏好设置表
CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    
    -- 全局开关
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- 应用内通知分类开关
    in_app_system BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_account BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_resume BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_interview BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_job BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_community BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_growth BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- 邮件通知分类开关
    email_system BOOLEAN NOT NULL DEFAULT TRUE,
    email_account BOOLEAN NOT NULL DEFAULT TRUE,
    email_resume BOOLEAN NOT NULL DEFAULT TRUE,
    email_interview BOOLEAN NOT NULL DEFAULT TRUE,
    email_job BOOLEAN NOT NULL DEFAULT TRUE,
    email_community BOOLEAN NOT NULL DEFAULT FALSE,
    email_growth BOOLEAN NOT NULL DEFAULT FALSE,
    email_marketing BOOLEAN NOT NULL DEFAULT FALSE,
    email_weekly_digest BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- 推送通知分类开关
    push_system BOOLEAN NOT NULL DEFAULT TRUE,
    push_account BOOLEAN NOT NULL DEFAULT TRUE,
    push_resume BOOLEAN NOT NULL DEFAULT FALSE,
    push_interview BOOLEAN NOT NULL DEFAULT TRUE,
    push_job BOOLEAN NOT NULL DEFAULT TRUE,
    push_community BOOLEAN NOT NULL DEFAULT FALSE,
    push_growth BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- 免打扰设置
    quiet_hours_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_hours_start VARCHAR(5) DEFAULT '22:00',
    quiet_hours_end VARCHAR(5) DEFAULT '08:00',
    timezone VARCHAR(50) DEFAULT 'Asia/Shanghai',
    
    -- 邮件频率
    email_frequency VARCHAR(20) NOT NULL DEFAULT 'IMMEDIATE',
    digest_frequency VARCHAR(20) NOT NULL DEFAULT 'WEEKLY',
    
    -- 时间戳
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_user_category ON notifications(user_id, category);
CREATE INDEX idx_notifications_entity ON notifications(entity_type, entity_id);
CREATE INDEX idx_notifications_expires ON notifications(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_notifications_channel_unsent ON notifications(channel, is_sent) WHERE is_sent = FALSE;
CREATE INDEX idx_notification_preferences_user ON notification_preferences(user_id);
