package com.cvibe.biz.notification.entity;

import com.cvibe.biz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * NotificationPreference Entity
 * 
 * User preferences for notification delivery.
 */
@Entity
@Table(name = "notification_preferences", indexes = {
        @Index(name = "idx_notification_prefs_user_id", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User who owns these preferences
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    private User user;

    // ================== In-App Notifications ==================

    @Column(name = "in_app_enabled")
    @Builder.Default
    private Boolean inAppEnabled = true;

    @Column(name = "in_app_system")
    @Builder.Default
    private Boolean inAppSystem = true;

    @Column(name = "in_app_account")
    @Builder.Default
    private Boolean inAppAccount = true;

    @Column(name = "in_app_resume")
    @Builder.Default
    private Boolean inAppResume = true;

    @Column(name = "in_app_interview")
    @Builder.Default
    private Boolean inAppInterview = true;

    @Column(name = "in_app_job")
    @Builder.Default
    private Boolean inAppJob = true;

    @Column(name = "in_app_community")
    @Builder.Default
    private Boolean inAppCommunity = true;

    @Column(name = "in_app_growth")
    @Builder.Default
    private Boolean inAppGrowth = true;

    // ================== Email Notifications ==================

    @Column(name = "email_enabled")
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(name = "email_system")
    @Builder.Default
    private Boolean emailSystem = true;

    @Column(name = "email_account")
    @Builder.Default
    private Boolean emailAccount = true;

    @Column(name = "email_resume")
    @Builder.Default
    private Boolean emailResume = false;

    @Column(name = "email_interview")
    @Builder.Default
    private Boolean emailInterview = true;

    @Column(name = "email_job")
    @Builder.Default
    private Boolean emailJob = true;

    @Column(name = "email_community")
    @Builder.Default
    private Boolean emailCommunity = false;

    @Column(name = "email_growth")
    @Builder.Default
    private Boolean emailGrowth = false;

    @Column(name = "email_marketing")
    @Builder.Default
    private Boolean emailMarketing = false;

    @Column(name = "email_weekly_digest")
    @Builder.Default
    private Boolean emailWeeklyDigest = true;

    // ================== Push Notifications ==================

    @Column(name = "push_enabled")
    @Builder.Default
    private Boolean pushEnabled = true;

    @Column(name = "push_system")
    @Builder.Default
    private Boolean pushSystem = true;

    @Column(name = "push_account")
    @Builder.Default
    private Boolean pushAccount = true;

    @Column(name = "push_resume")
    @Builder.Default
    private Boolean pushResume = false;

    @Column(name = "push_interview")
    @Builder.Default
    private Boolean pushInterview = true;

    @Column(name = "push_job")
    @Builder.Default
    private Boolean pushJob = true;

    @Column(name = "push_community")
    @Builder.Default
    private Boolean pushCommunity = true;

    @Column(name = "push_growth")
    @Builder.Default
    private Boolean pushGrowth = false;

    // ================== Quiet Hours ==================

    @Column(name = "quiet_hours_enabled")
    @Builder.Default
    private Boolean quietHoursEnabled = false;

    @Column(name = "quiet_hours_start")
    private String quietHoursStart; // HH:mm format

    @Column(name = "quiet_hours_end")
    private String quietHoursEnd; // HH:mm format

    @Column(name = "timezone")
    @Builder.Default
    private String timezone = "UTC";

    // ================== Frequency Settings ==================

    @Enumerated(EnumType.STRING)
    @Column(name = "email_frequency")
    @Builder.Default
    private EmailFrequency emailFrequency = EmailFrequency.IMMEDIATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "digest_frequency")
    @Builder.Default
    private DigestFrequency digestFrequency = DigestFrequency.WEEKLY;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ================== Enums ==================

    public enum EmailFrequency {
        IMMEDIATE,
        HOURLY,
        DAILY,
        WEEKLY,
        NEVER
    }

    public enum DigestFrequency {
        DAILY,
        WEEKLY,
        MONTHLY,
        NEVER
    }

    // ================== Business Methods ==================

    public boolean shouldSendInApp(Notification.NotificationCategory category) {
        if (!inAppEnabled) return false;
        return switch (category) {
            case GENERAL -> inAppSystem;
            case ACCOUNT -> inAppAccount;
            case RESUME -> inAppResume;
            case INTERVIEW -> inAppInterview;
            case JOB -> inAppJob;
            case COMMUNITY -> inAppCommunity;
            case GROWTH -> inAppGrowth;
            case ADMIN -> true;
        };
    }

    public boolean shouldSendEmail(Notification.NotificationCategory category) {
        if (!emailEnabled) return false;
        return switch (category) {
            case GENERAL -> emailSystem;
            case ACCOUNT -> emailAccount;
            case RESUME -> emailResume;
            case INTERVIEW -> emailInterview;
            case JOB -> emailJob;
            case COMMUNITY -> emailCommunity;
            case GROWTH -> emailGrowth;
            case ADMIN -> true;
        };
    }

    public boolean shouldSendPush(Notification.NotificationCategory category) {
        if (!pushEnabled) return false;
        return switch (category) {
            case GENERAL -> pushSystem;
            case ACCOUNT -> pushAccount;
            case RESUME -> pushResume;
            case INTERVIEW -> pushInterview;
            case JOB -> pushJob;
            case COMMUNITY -> pushCommunity;
            case GROWTH -> pushGrowth;
            case ADMIN -> true;
        };
    }

    // ================== Static Factory ==================

    public static NotificationPreference defaultPreferences(User user) {
        return NotificationPreference.builder()
                .user(user)
                .build();
    }
}
