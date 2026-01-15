package com.cvibe.biz.growth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * LearningMilestone Entity
 * 
 * Represents a specific action item or checkpoint within a learning path.
 */
@Entity
@Table(name = "learning_milestones", indexes = {
        @Index(name = "idx_milestone_path", columnList = "learning_path_id"),
        @Index(name = "idx_milestone_completed", columnList = "isCompleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Parent learning path
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_path_id", nullable = false)
    private LearningPath learningPath;

    /**
     * Milestone title
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Detailed description/instructions
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Type of milestone
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MilestoneType type;

    /**
     * Estimated hours to complete
     */
    private Integer estimatedHours;

    /**
     * Resource URL (course, tutorial, etc.)
     */
    @Column(length = 500)
    private String resourceUrl;

    /**
     * Order within the learning path
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer sortOrder = 0;

    /**
     * Whether this milestone is completed
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isCompleted = false;

    /**
     * Completion timestamp
     */
    private Instant completedAt;

    /**
     * User's notes about this milestone
     */
    @Column(columnDefinition = "TEXT")
    private String userNotes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // ==================== Enums ====================

    public enum MilestoneType {
        COURSE,         // Online course to complete
        TUTORIAL,       // Tutorial to follow
        PROJECT,        // Project to build
        READING,        // Documentation/book to read
        PRACTICE,       // Practice exercises (LeetCode, etc.)
        CERTIFICATION,  // Exam to pass
        REVIEW,         // Review/recap session
        ASSESSMENT      // Self-assessment checkpoint
    }

    // ==================== Business Methods ====================

    public void markCompleted() {
        this.isCompleted = true;
        this.completedAt = Instant.now();
        // Trigger parent path recalculation
        if (this.learningPath != null) {
            this.learningPath.recalculateProgress();
        }
    }

    public void markIncomplete() {
        this.isCompleted = false;
        this.completedAt = null;
        if (this.learningPath != null) {
            this.learningPath.recalculateProgress();
        }
    }

    public boolean isCompleted() {
        return Boolean.TRUE.equals(this.isCompleted);
    }
}
