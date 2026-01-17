package com.cvibe.growth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a milestone within a learning path
 */
@Entity
@Table(name = "learning_milestones", indexes = {
    @Index(name = "idx_learning_milestone_path", columnList = "path_id"),
    @Index(name = "idx_learning_milestone_order", columnList = "order_index")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "path_id", nullable = false)
    private LearningPath path;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Type of milestone: LEARN, PRACTICE, PROJECT
     */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "resources_json", columnDefinition = "TEXT")
    private String resourcesJson;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "completed_at")
    private Instant completedAt;
}
