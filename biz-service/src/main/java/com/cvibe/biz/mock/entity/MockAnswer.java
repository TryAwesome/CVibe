package com.cvibe.biz.mock.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * MockAnswer Entity
 * 
 * Represents a user's answer to a mock interview question, along with AI evaluation.
 */
@Entity
@Table(name = "mock_answers", indexes = {
        @Index(name = "idx_mock_answers_question_id", columnList = "question_id"),
        @Index(name = "idx_mock_answers_score", columnList = "score")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class MockAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    @ToString.Exclude
    private MockQuestion question;

    /**
     * User's answer text
     */
    @Column(name = "answer_text", columnDefinition = "TEXT", nullable = false)
    private String answerText;

    /**
     * User's code answer (for coding questions)
     */
    @Column(name = "code_answer", columnDefinition = "TEXT")
    private String codeAnswer;

    /**
     * Programming language used (for coding questions)
     */
    @Column(name = "programming_language")
    private String programmingLanguage;

    /**
     * Time taken to answer in seconds
     */
    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    /**
     * Answer started at
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * Answer submitted at
     */
    @Column(name = "submitted_at")
    private Instant submittedAt;

    // ================== AI Evaluation ==================

    /**
     * Is the answer evaluated by AI
     */
    @Column(name = "is_evaluated")
    @Builder.Default
    private Boolean isEvaluated = false;

    /**
     * Overall score (0-100)
     */
    @Column(name = "score")
    private Integer score;

    /**
     * Technical accuracy score (0-100)
     */
    @Column(name = "accuracy_score")
    private Integer accuracyScore;

    /**
     * Completeness score (0-100)
     */
    @Column(name = "completeness_score")
    private Integer completenessScore;

    /**
     * Communication clarity score (0-100)
     */
    @Column(name = "clarity_score")
    private Integer clarityScore;

    /**
     * Relevance score (0-100)
     */
    @Column(name = "relevance_score")
    private Integer relevanceScore;

    /**
     * AI feedback text
     */
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    /**
     * Strengths in the answer
     */
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    /**
     * Areas for improvement
     */
    @Column(name = "improvements", columnDefinition = "TEXT")
    private String improvements;

    /**
     * Key points covered from expected answer
     */
    @Column(name = "covered_points", columnDefinition = "TEXT")
    private String coveredPoints;

    /**
     * Key points missed from expected answer
     */
    @Column(name = "missed_points", columnDefinition = "TEXT")
    private String missedPoints;

    /**
     * Suggested improved answer
     */
    @Column(name = "suggested_answer", columnDefinition = "TEXT")
    private String suggestedAnswer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ================== Helper Methods ==================

    public void recordSubmission(Instant startTime) {
        this.submittedAt = Instant.now();
        this.startedAt = startTime;
        if (startTime != null) {
            this.timeTakenSeconds = (int) (submittedAt.getEpochSecond() - startTime.getEpochSecond());
        }
    }

    public void evaluate(int accuracy, int completeness, int clarity, int relevance,
                         String feedback, String strengths, String improvements) {
        this.isEvaluated = true;
        this.accuracyScore = accuracy;
        this.completenessScore = completeness;
        this.clarityScore = clarity;
        this.relevanceScore = relevance;
        this.score = (accuracy + completeness + clarity + relevance) / 4;
        this.feedback = feedback;
        this.strengths = strengths;
        this.improvements = improvements;
    }

    public void setCoverage(String covered, String missed) {
        this.coveredPoints = covered;
        this.missedPoints = missed;
    }

    public String getScoreGrade() {
        if (score == null) return "N/A";
        if (score >= 90) return "Excellent";
        if (score >= 80) return "Good";
        if (score >= 70) return "Satisfactory";
        if (score >= 60) return "Needs Improvement";
        return "Poor";
    }

    public boolean isPassing() {
        return score != null && score >= 60;
    }
}
