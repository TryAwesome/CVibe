package com.cvibe.biz.notification.entity;

import com.cvibe.biz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification Entity
 * 
 * Stores user notifications for various events.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user_id", columnList = "user_id"),
        @Index(name = "idx_notifications_type", columnList = "notification_type"),
        @Index(name = "idx_notifications_is_read", columnList = "is_read"),
        @Index(name = "idx_notifications_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User who receives the notification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    /**
     * Notification type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    /**
     * Notification category for grouping
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @Builder.Default
    private NotificationCategory category = NotificationCategory.GENERAL;

    /**
     * Notification title
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Notification content/message
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * Priority level
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    /**
     * Action URL (where to navigate when clicked)
     */
    @Column(name = "action_url")
    private String actionUrl;

    /**
     * Action text (button label)
     */
    @Column(name = "action_text")
    private String actionText;

    /**
     * Related entity type (e.g., "resume", "interview", "post")
     */
    @Column(name = "entity_type")
    private String entityType;

    /**
     * Related entity ID
     */
    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * Sender user (for social notifications)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    @ToString.Exclude
    private User sender;

    /**
     * Image/avatar URL for the notification
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * Additional metadata (JSON)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Whether the notification has been read
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * When the notification was read
     */
    @Column(name = "read_at")
    private Instant readAt;

    /**
     * Whether the notification has been clicked
     */
    @Column(name = "is_clicked")
    @Builder.Default
    private Boolean isClicked = false;

    /**
     * When the notification was clicked
     */
    @Column(name = "clicked_at")
    private Instant clickedAt;

    /**
     * Whether the notification has been dismissed
     */
    @Column(name = "is_dismissed")
    @Builder.Default
    private Boolean isDismissed = false;

    /**
     * Delivery channel used
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    @Builder.Default
    private DeliveryChannel channel = DeliveryChannel.IN_APP;

    /**
     * Whether email was sent
     */
    @Column(name = "email_sent")
    @Builder.Default
    private Boolean emailSent = false;

    /**
     * Email sent at
     */
    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    /**
     * Whether push notification was sent
     */
    @Column(name = "push_sent")
    @Builder.Default
    private Boolean pushSent = false;

    /**
     * Push sent at
     */
    @Column(name = "push_sent_at")
    private Instant pushSentAt;

    /**
     * Expiration time (null = never expires)
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ================== Enums ==================

    public enum NotificationType {
        // System
        SYSTEM_ANNOUNCEMENT,
        SYSTEM_MAINTENANCE,
        SYSTEM_UPDATE,
        
        // Account
        WELCOME,
        ACCOUNT_VERIFIED,
        PASSWORD_CHANGED,
        PROFILE_UPDATED,
        SECURITY_ALERT,
        
        // Resume
        RESUME_CREATED,
        RESUME_ANALYZED,
        RESUME_FEEDBACK,
        RESUME_EXPORTED,
        
        // Interview
        INTERVIEW_STARTED,
        INTERVIEW_COMPLETED,
        INTERVIEW_FEEDBACK,
        INTERVIEW_REMINDER,
        
        // Mock Interview
        MOCK_INTERVIEW_INVITED,
        MOCK_INTERVIEW_STARTED,
        MOCK_INTERVIEW_ENDED,
        MOCK_INTERVIEW_REVIEW,
        
        // Job
        JOB_MATCH,
        JOB_RECOMMENDATION,
        JOB_APPLICATION_STATUS,
        JOB_SAVED_EXPIRING,
        
        // Community
        POST_LIKED,
        POST_COMMENTED,
        COMMENT_REPLIED,
        NEW_FOLLOWER,
        MENTION,
        
        // Growth
        ACHIEVEMENT_UNLOCKED,
        STREAK_MILESTONE,
        WEEKLY_SUMMARY,
        MONTHLY_REPORT,
        
        // Admin
        ADMIN_MESSAGE,
        CONTENT_MODERATED,
        ACCOUNT_WARNING
    }

    public enum NotificationCategory {
        GENERAL,
        ACCOUNT,
        RESUME,
        INTERVIEW,
        JOB,
        COMMUNITY,
        GROWTH,
        ADMIN
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    public enum DeliveryChannel {
        IN_APP,
        EMAIL,
        PUSH,
        SMS,
        ALL
    }

    // ================== Business Methods ==================

    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = Instant.now();
        }
    }

    public void markAsClicked() {
        if (!this.isClicked) {
            this.isClicked = true;
            this.clickedAt = Instant.now();
            markAsRead();
        }
    }

    public void dismiss() {
        this.isDismissed = true;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    // ================== Static Factory Methods ==================

    public static Notification system(User user, String title, String content) {
        return Notification.builder()
                .user(user)
                .notificationType(NotificationType.SYSTEM_ANNOUNCEMENT)
                .category(NotificationCategory.GENERAL)
                .title(title)
                .content(content)
                .priority(Priority.NORMAL)
                .build();
    }

    public static Notification welcome(User user) {
        return Notification.builder()
                .user(user)
                .notificationType(NotificationType.WELCOME)
                .category(NotificationCategory.ACCOUNT)
                .title("Welcome to CVibe!")
                .content("Start your journey by creating your first resume or practicing for interviews.")
                .actionUrl("/dashboard")
                .actionText("Get Started")
                .priority(Priority.HIGH)
                .build();
    }

    public static Notification social(User user, User sender, NotificationType type, String title, String content, String entityType, UUID entityId) {
        return Notification.builder()
                .user(user)
                .sender(sender)
                .notificationType(type)
                .category(NotificationCategory.COMMUNITY)
                .title(title)
                .content(content)
                .entityType(entityType)
                .entityId(entityId)
                .imageUrl(sender.getAvatarUrl())
                .build();
    }

    public static Notification achievement(User user, String title, String content, String actionUrl) {
        return Notification.builder()
                .user(user)
                .notificationType(NotificationType.ACHIEVEMENT_UNLOCKED)
                .category(NotificationCategory.GROWTH)
                .title(title)
                .content(content)
                .actionUrl(actionUrl)
                .priority(Priority.HIGH)
                .build();
    }
}
