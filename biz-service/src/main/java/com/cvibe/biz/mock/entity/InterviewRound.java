package com.cvibe.biz.mock.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * InterviewRound Entity
 * 
 * Represents a specific round within a mock interview (e.g., technical round, behavioral round).
 */
@Entity
@Table(name = "interview_rounds", indexes = {
        @Index(name = "idx_interview_rounds_interview_id", columnList = "interview_id"),
        @Index(name = "idx_interview_rounds_type", columnList = "round_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class InterviewRound {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    @ToString.Exclude
    private MockInterview interview;

    /**
     * Round number (1, 2, 3...)
     */
    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    /**
     * Round name
     */
    @Column(name = "round_name")
    private String roundName;

    /**
     * Round type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "round_type", nullable = false)
    private RoundType roundType;

    /**
     * Round status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private RoundStatus status = RoundStatus.PENDING;

    /**
     * Number of questions in this round
     */
    @Column(name = "question_count")
    @Builder.Default
    private Integer questionCount = 0;

    /**
     * Round score (0-100)
     */
    @Column(name = "score")
    private Integer score;

    /**
     * Round feedback
     */
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    /**
     * Duration in seconds
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Started at
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * Completed at
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ================== Relationships ==================

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<MockQuestion> questions = new ArrayList<>();

    // ================== Enums ==================

    public enum RoundType {
        SCREENING,       // Initial screening
        TECHNICAL,       // Technical interview
        CODING,          // Live coding round
        SYSTEM_DESIGN,   // System design round
        BEHAVIORAL,      // Behavioral round
        CULTURAL_FIT,    // Cultural fit
        FINAL            // Final round
    }

    public enum RoundStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        SKIPPED
    }

    // ================== Helper Methods ==================

    public void addQuestion(MockQuestion question) {
        questions.add(question);
        question.setRound(this);
        this.questionCount++;
    }

    public void start() {
        this.status = RoundStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public void complete(int score, String feedback) {
        this.status = RoundStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.score = score;
        this.feedback = feedback;
        if (startedAt != null) {
            this.durationSeconds = (int) (completedAt.getEpochSecond() - startedAt.getEpochSecond());
        }
    }

    public void skip() {
        this.status = RoundStatus.SKIPPED;
    }
}
