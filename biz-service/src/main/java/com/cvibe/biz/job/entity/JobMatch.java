package com.cvibe.biz.job.entity;

import com.cvibe.biz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Job Match Entity - Stores matching results between user profiles and jobs
 */
@Entity
@Table(name = "job_matches", indexes = {
        @Index(name = "idx_job_match_user", columnList = "user_id"),
        @Index(name = "idx_job_match_score", columnList = "match_score DESC"),
        @Index(name = "idx_job_match_date", columnList = "matched_at DESC")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "match_score", nullable = false)
    private Double matchScore;  // 0-100 percentage

    @Column(name = "match_reason", columnDefinition = "TEXT")
    private String matchReason;  // AI-generated explanation of why this is a good match

    @Column(name = "matched_skills", columnDefinition = "TEXT")
    private String matchedSkills;  // JSON array of matching skills

    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills;  // JSON array of skills user lacks

    @Column(name = "is_viewed")
    @Builder.Default
    private Boolean isViewed = false;

    @Column(name = "is_saved")
    @Builder.Default
    private Boolean isSaved = false;

    @Column(name = "is_applied")
    @Builder.Default
    private Boolean isApplied = false;

    @Column(name = "user_rating")
    private Integer userRating;  // 1-5 stars user feedback on match quality

    @Column(name = "user_feedback", columnDefinition = "TEXT")
    private String userFeedback;

    @CreatedDate
    @Column(name = "matched_at", nullable = false, updatable = false)
    private Instant matchedAt;

    @Column(name = "viewed_at")
    private Instant viewedAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    // ================== Helper Methods ==================

    public void markViewed() {
        this.isViewed = true;
        this.viewedAt = Instant.now();
    }

    public void markApplied() {
        this.isApplied = true;
        this.appliedAt = Instant.now();
    }

    public void toggleSaved() {
        this.isSaved = !this.isSaved;
    }
}
