package com.cvibe.biz.admin.entity;

import com.cvibe.biz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Announcement Entity
 * 
 * System announcements displayed to users.
 */
@Entity
@Table(name = "announcements", indexes = {
        @Index(name = "idx_announcements_status", columnList = "status"),
        @Index(name = "idx_announcements_priority", columnList = "priority"),
        @Index(name = "idx_announcements_start_time", columnList = "start_time"),
        @Index(name = "idx_announcements_end_time", columnList = "end_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Announcement title
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Announcement content (supports Markdown)
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Announcement type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "announcement_type", nullable = false)
    @Builder.Default
    private AnnouncementType announcementType = AnnouncementType.INFO;

    /**
     * Priority (higher = more important)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    /**
     * Target audience
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", nullable = false)
    @Builder.Default
    private TargetAudience targetAudience = TargetAudience.ALL;

    /**
     * Announcement status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AnnouncementStatus status = AnnouncementStatus.DRAFT;

    /**
     * Start time (when to display)
     */
    @Column(name = "start_time")
    private Instant startTime;

    /**
     * End time (when to hide)
     */
    @Column(name = "end_time")
    private Instant endTime;

    /**
     * Whether to pin at top
     */
    @Column(name = "is_pinned")
    @Builder.Default
    private Boolean isPinned = false;

    /**
     * Whether dismissible by user
     */
    @Column(name = "is_dismissible")
    @Builder.Default
    private Boolean isDismissible = true;

    /**
     * Link URL (optional)
     */
    @Column(name = "link_url")
    private String linkUrl;

    /**
     * Link text (optional)
     */
    @Column(name = "link_text")
    private String linkText;

    /**
     * View count
     */
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * Dismiss count
     */
    @Column(name = "dismiss_count")
    @Builder.Default
    private Integer dismissCount = 0;

    /**
     * Created by admin
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @ToString.Exclude
    private User createdBy;

    /**
     * Last updated by
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    @ToString.Exclude
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ================== Enums ==================

    public enum AnnouncementType {
        INFO,           // General information
        UPDATE,         // New features/updates
        MAINTENANCE,    // Scheduled maintenance
        WARNING,        // Important warnings
        CRITICAL,       // Critical alerts
        PROMOTION       // Promotions/events
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    public enum TargetAudience {
        ALL,            // All users
        FREE_USERS,     // Free tier users
        PREMIUM_USERS,  // Premium users
        NEW_USERS,      // Users registered in last 7 days
        INACTIVE_USERS  // Users inactive for 30+ days
    }

    public enum AnnouncementStatus {
        DRAFT,          // Not published
        SCHEDULED,      // Scheduled for future
        ACTIVE,         // Currently active
        EXPIRED,        // Past end time
        ARCHIVED        // Manually archived
    }

    // ================== Business Methods ==================

    public boolean isCurrentlyActive() {
        if (status != AnnouncementStatus.ACTIVE && status != AnnouncementStatus.SCHEDULED) {
            return false;
        }
        Instant now = Instant.now();
        boolean afterStart = startTime == null || now.isAfter(startTime);
        boolean beforeEnd = endTime == null || now.isBefore(endTime);
        return afterStart && beforeEnd;
    }

    public void publish() {
        if (startTime != null && startTime.isAfter(Instant.now())) {
            this.status = AnnouncementStatus.SCHEDULED;
        } else {
            this.status = AnnouncementStatus.ACTIVE;
        }
    }

    public void archive() {
        this.status = AnnouncementStatus.ARCHIVED;
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }

    public void incrementDismissCount() {
        this.dismissCount = (this.dismissCount == null ? 0 : this.dismissCount) + 1;
    }

    // ================== Static Factory ==================

    public static Announcement info(String title, String content, User createdBy) {
        return Announcement.builder()
                .title(title)
                .content(content)
                .announcementType(AnnouncementType.INFO)
                .createdBy(createdBy)
                .build();
    }

    public static Announcement maintenance(String title, String content, Instant startTime, Instant endTime, User createdBy) {
        return Announcement.builder()
                .title(title)
                .content(content)
                .announcementType(AnnouncementType.MAINTENANCE)
                .priority(Priority.HIGH)
                .startTime(startTime)
                .endTime(endTime)
                .isDismissible(false)
                .createdBy(createdBy)
                .build();
    }

    public static Announcement critical(String title, String content, User createdBy) {
        return Announcement.builder()
                .title(title)
                .content(content)
                .announcementType(AnnouncementType.CRITICAL)
                .priority(Priority.URGENT)
                .isPinned(true)
                .isDismissible(false)
                .createdBy(createdBy)
                .status(AnnouncementStatus.ACTIVE)
                .build();
    }
}
