package com.cvibe.biz.growth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * SkillGap Entity
 * 
 * Represents a specific skill gap identified during gap analysis.
 * Tracks missing skills, proficiency gaps, and remediation status.
 */
@Entity
@Table(name = "skill_gaps", indexes = {
        @Index(name = "idx_skill_gap_goal", columnList = "goal_id"),
        @Index(name = "idx_skill_gap_priority", columnList = "priority"),
        @Index(name = "idx_skill_gap_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillGap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Associated growth goal
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private GrowthGoal goal;

    /**
     * Name of the skill/technology
     */
    @Column(nullable = false, length = 100)
    private String skillName;

    /**
     * Category of the skill
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SkillCategory category;

    /**
     * Current proficiency level (0-100)
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer currentLevel = 0;

    /**
     * Required proficiency level (0-100)
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer requiredLevel = 70;

    /**
     * Gap severity/importance
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GapPriority priority = GapPriority.MEDIUM;

    /**
     * Gap status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GapStatus status = GapStatus.IDENTIFIED;

    /**
     * Whether this skill is explicitly mentioned in JD
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isRequired = false;

    /**
     * Whether this is a "nice to have" skill
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isPreferred = false;

    /**
     * Estimated time to close the gap (in hours)
     */
    private Integer estimatedHours;

    /**
     * AI recommendation for closing this gap
     */
    @Column(columnDefinition = "TEXT")
    private String recommendation;

    /**
     * Resources/links for learning
     */
    @Column(name = "learning_resources", columnDefinition = "TEXT")
    private String learningResources;

    /**
     * User's notes about this gap
     */
    @Column(columnDefinition = "TEXT")
    private String userNotes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // ==================== Enums ====================

    public enum SkillCategory {
        PROGRAMMING_LANGUAGE,
        FRAMEWORK,
        DATABASE,
        CLOUD,
        DEVOPS,
        SYSTEM_DESIGN,
        DATA_STRUCTURE,
        ALGORITHM,
        SOFT_SKILL,
        DOMAIN_KNOWLEDGE,
        TOOL,
        OTHER
    }

    public enum GapPriority {
        CRITICAL,   // Must have, blocking
        HIGH,       // Required skill
        MEDIUM,     // Preferred skill
        LOW         // Nice to have
    }

    public enum GapStatus {
        IDENTIFIED, // Gap found
        IN_PROGRESS,// Working on it
        RESOLVED,   // Gap closed
        DEFERRED    // Postponed
    }

    // ==================== Business Methods ====================

    public int getGapSize() {
        return Math.max(0, requiredLevel - currentLevel);
    }

    public double getGapPercentage() {
        if (requiredLevel == 0) return 0;
        return (double) getGapSize() / requiredLevel * 100;
    }

    public void updateProgress(int newLevel) {
        this.currentLevel = Math.max(0, Math.min(100, newLevel));
        if (this.currentLevel >= this.requiredLevel) {
            this.status = GapStatus.RESOLVED;
        } else if (this.currentLevel > 0) {
            this.status = GapStatus.IN_PROGRESS;
        }
    }

    public void markResolved() {
        this.status = GapStatus.RESOLVED;
        this.currentLevel = this.requiredLevel;
    }

    public void defer() {
        this.status = GapStatus.DEFERRED;
    }

    public boolean isResolved() {
        return this.status == GapStatus.RESOLVED || this.currentLevel >= this.requiredLevel;
    }
}
