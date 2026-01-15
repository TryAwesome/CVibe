package com.cvibe.biz.growth.entity;

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
 * LearningPath Entity
 * 
 * Represents a structured learning roadmap to achieve a growth goal.
 * Contains milestones with specific action items.
 */
@Entity
@Table(name = "learning_paths", indexes = {
        @Index(name = "idx_learning_path_goal", columnList = "goal_id"),
        @Index(name = "idx_learning_path_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningPath {

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
     * Path title/name
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Brief description of what this path covers
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Focus area of this path
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PathFocus focus;

    /**
     * Difficulty level
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DifficultyLevel difficulty;

    /**
     * Estimated total hours to complete
     */
    private Integer estimatedHours;

    /**
     * Target completion date
     */
    private LocalDate targetDate;

    /**
     * Completion percentage (0-100)
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer completionPercent = 0;

    /**
     * Path status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PathStatus status = PathStatus.NOT_STARTED;

    /**
     * Order/priority of this path within the goal
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer sortOrder = 0;

    /**
     * Milestones within this path
     */
    @OneToMany(mappedBy = "learningPath", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<LearningMilestone> milestones = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // ==================== Enums ====================

    public enum PathFocus {
        TECHNICAL_SKILLS,
        SYSTEM_DESIGN,
        CODING_PRACTICE,
        PROJECT_BUILDING,
        CERTIFICATION,
        SOFT_SKILLS,
        INTERVIEW_PREP,
        DOMAIN_KNOWLEDGE
    }

    public enum DifficultyLevel {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED,
        EXPERT
    }

    public enum PathStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        PAUSED,
        ABANDONED
    }

    // ==================== Business Methods ====================

    public void recalculateProgress() {
        if (milestones.isEmpty()) {
            this.completionPercent = 0;
            return;
        }

        long completed = milestones.stream()
                .filter(LearningMilestone::isCompleted)
                .count();
        this.completionPercent = (int) (completed * 100 / milestones.size());

        // Update status based on progress
        if (this.completionPercent == 100) {
            this.status = PathStatus.COMPLETED;
        } else if (this.completionPercent > 0) {
            this.status = PathStatus.IN_PROGRESS;
        }
    }

    public void start() {
        this.status = PathStatus.IN_PROGRESS;
    }

    public void pause() {
        this.status = PathStatus.PAUSED;
    }

    public void resume() {
        this.status = PathStatus.IN_PROGRESS;
    }

    public void complete() {
        this.status = PathStatus.COMPLETED;
        this.completionPercent = 100;
    }

    public void addMilestone(LearningMilestone milestone) {
        milestone.setSortOrder(milestones.size());
        milestones.add(milestone);
        milestone.setLearningPath(this);
    }
}
