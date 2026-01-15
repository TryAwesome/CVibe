package com.cvibe.biz.growth.entity;

import com.cvibe.biz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GrowthGoal Entity
 * 
 * Represents a user's career growth target (target role/company).
 * Used for gap analysis and learning path generation.
 */
@Entity
@Table(name = "growth_goals", indexes = {
        @Index(name = "idx_growth_goal_user", columnList = "user_id"),
        @Index(name = "idx_growth_goal_status", columnList = "status"),
        @Index(name = "idx_growth_goal_active", columnList = "isActive")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrowthGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User who owns this goal
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Target role/position title
     */
    @Column(nullable = false, length = 200)
    private String targetRole;

    /**
     * Target company (optional)
     */
    @Column(length = 200)
    private String targetCompany;

    /**
     * Target experience level
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TargetLevel targetLevel;

    /**
     * Job description text (pasted or extracted from image)
     */
    @Column(name = "job_requirements", columnDefinition = "TEXT")
    private String jobRequirements;

    /**
     * Path to uploaded JD file (if image/PDF was uploaded)
     */
    @Column(length = 500)
    private String jdFilePath;

    /**
     * Target deadline for achieving this goal
     */
    private LocalDate targetDate;

    /**
     * Overall progress percentage (0-100)
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer progressPercent = 0;

    /**
     * Goal status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GoalStatus status = GoalStatus.ACTIVE;

    /**
     * Whether this is the user's primary/active goal
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    /**
     * AI-generated analysis summary
     */
    @Column(name = "analysis_summary", columnDefinition = "TEXT")
    private String analysisSummary;

    /**
     * Match score between user profile and target (0-100)
     */
    private Double matchScore;

    /**
     * Last time gap analysis was run
     */
    private Instant lastAnalyzedAt;

    /**
     * Skill gaps identified for this goal
     */
    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SkillGap> skillGaps = new ArrayList<>();

    /**
     * Learning paths for this goal
     */
    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LearningPath> learningPaths = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // ==================== Enums ====================

    public enum TargetLevel {
        ENTRY,
        JUNIOR,
        MID,
        SENIOR,
        LEAD,
        PRINCIPAL,
        MANAGER,
        DIRECTOR,
        VP,
        EXECUTIVE
    }

    public enum GoalStatus {
        ACTIVE,         // Currently pursuing
        PAUSED,         // Temporarily paused
        ACHIEVED,       // Goal reached
        ABANDONED       // No longer pursuing
    }

    // ==================== Business Methods ====================

    public void updateProgress(int percent) {
        this.progressPercent = Math.max(0, Math.min(100, percent));
        if (this.progressPercent == 100) {
            this.status = GoalStatus.ACHIEVED;
        }
    }

    public void markAsAchieved() {
        this.status = GoalStatus.ACHIEVED;
        this.progressPercent = 100;
    }

    public void pause() {
        this.status = GoalStatus.PAUSED;
    }

    public void resume() {
        this.status = GoalStatus.ACTIVE;
    }

    public void abandon() {
        this.status = GoalStatus.ABANDONED;
        this.isActive = false;
    }

    public void setAsActive() {
        this.isActive = true;
        this.status = GoalStatus.ACTIVE;
    }

    public void recordAnalysis(String summary, Double score) {
        this.analysisSummary = summary;
        this.matchScore = score;
        this.lastAnalyzedAt = Instant.now();
    }

    public boolean belongsToUser(UUID userId) {
        return this.user != null && this.user.getId().equals(userId);
    }

    public void addSkillGap(SkillGap gap) {
        skillGaps.add(gap);
        gap.setGoal(this);
    }

    public void addLearningPath(LearningPath path) {
        learningPaths.add(path);
        path.setGoal(this);
    }
}
