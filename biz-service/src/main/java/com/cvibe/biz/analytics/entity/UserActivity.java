package com.cvibe.biz.analytics.entity;

import com.cvibe.biz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * UserActivity Entity
 * 
 * Tracks individual user actions for analytics and insights.
 */
@Entity
@Table(name = "user_activities", indexes = {
        @Index(name = "idx_user_activities_user_id", columnList = "user_id"),
        @Index(name = "idx_user_activities_type", columnList = "activity_type"),
        @Index(name = "idx_user_activities_created_at", columnList = "created_at"),
        @Index(name = "idx_user_activities_session", columnList = "session_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    /**
     * Activity type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    /**
     * Activity category for grouping
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ActivityCategory category;

    /**
     * Related entity type (e.g., "Resume", "Interview", "Post")
     */
    @Column(name = "entity_type")
    private String entityType;

    /**
     * Related entity ID
     */
    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * Additional metadata (JSON)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Session ID for session-based tracking
     */
    @Column(name = "session_id")
    private String sessionId;

    /**
     * Duration in seconds (for timed activities)
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * IP address
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * User agent
     */
    @Column(name = "user_agent")
    private String userAgent;

    /**
     * Device type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type")
    private DeviceType deviceType;

    /**
     * Page/Screen viewed
     */
    @Column(name = "page_path")
    private String pagePath;

    /**
     * Referrer URL
     */
    @Column(name = "referrer")
    private String referrer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ================== Enums ==================

    public enum ActivityType {
        // Auth
        LOGIN,
        LOGOUT,
        REGISTER,
        
        // Resume
        RESUME_VIEW,
        RESUME_CREATE,
        RESUME_UPDATE,
        RESUME_DOWNLOAD,
        RESUME_SHARE,
        
        // Interview
        INTERVIEW_START,
        INTERVIEW_COMPLETE,
        INTERVIEW_ANSWER,
        MOCK_INTERVIEW_START,
        MOCK_INTERVIEW_COMPLETE,
        
        // Job
        JOB_VIEW,
        JOB_APPLY,
        JOB_SAVE,
        JOB_SEARCH,
        
        // Community
        POST_VIEW,
        POST_CREATE,
        POST_LIKE,
        COMMENT_CREATE,
        USER_FOLLOW,
        
        // Growth
        GOAL_CREATE,
        GOAL_COMPLETE,
        SKILL_ADD,
        
        // General
        PAGE_VIEW,
        FEATURE_USE,
        SEARCH,
        EXPORT,
        SETTINGS_CHANGE
    }

    public enum ActivityCategory {
        AUTH,
        RESUME,
        INTERVIEW,
        JOB,
        COMMUNITY,
        GROWTH,
        NAVIGATION,
        SETTINGS
    }

    public enum DeviceType {
        DESKTOP,
        MOBILE,
        TABLET,
        UNKNOWN
    }

    // ================== Static Factory ==================

    public static UserActivity of(User user, ActivityType type, ActivityCategory category) {
        return UserActivity.builder()
                .user(user)
                .activityType(type)
                .category(category)
                .build();
    }

    public static UserActivity forEntity(User user, ActivityType type, ActivityCategory category, 
                                         String entityType, UUID entityId) {
        return UserActivity.builder()
                .user(user)
                .activityType(type)
                .category(category)
                .entityType(entityType)
                .entityId(entityId)
                .build();
    }
}
