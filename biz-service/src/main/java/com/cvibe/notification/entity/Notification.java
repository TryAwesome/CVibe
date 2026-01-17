package com.cvibe.notification.entity;

import com.cvibe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification entity for user notifications
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user", columnList = "user_id"),
    @Index(name = "idx_notification_user_read", columnList = "user_id, is_read"),
    @Index(name = "idx_notification_created", columnList = "created_at DESC")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Notification type
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    /**
     * Notification priority
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    /**
     * Category for grouping (jobs, community, interview, etc.)
     */
    @Column(length = 50)
    private String category;

    /**
     * Notification title
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * Notification content
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Optional action URL
     */
    @Column(name = "action_url", length = 500)
    private String actionUrl;

    /**
     * Optional action button text
     */
    @Column(name = "action_text", length = 100)
    private String actionText;

    /**
     * Additional data as JSON
     */
    @Column(name = "data_json", columnDefinition = "TEXT")
    private String dataJson;

    /**
     * Read status
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
