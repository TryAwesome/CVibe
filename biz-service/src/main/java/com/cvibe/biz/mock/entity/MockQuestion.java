package com.cvibe.biz.mock.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * MockQuestion Entity
 * 
 * Represents a question in a mock interview.
 */
@Entity
@Table(name = "mock_questions", indexes = {
        @Index(name = "idx_mock_questions_interview_id", columnList = "interview_id"),
        @Index(name = "idx_mock_questions_round_id", columnList = "round_id"),
        @Index(name = "idx_mock_questions_category", columnList = "category"),
        @Index(name = "idx_mock_questions_difficulty", columnList = "difficulty")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class MockQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    @ToString.Exclude
    private MockInterview interview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
    @ToString.Exclude
    private InterviewRound round;

    /**
     * Question number in sequence
     */
    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    /**
     * Question category
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private QuestionCategory category;

    /**
     * Question difficulty
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    @Builder.Default
    private QuestionDifficulty difficulty = QuestionDifficulty.MEDIUM;

    /**
     * The question text
     */
    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    /**
     * Follow-up question (if any)
     */
    @Column(name = "follow_up_question", columnDefinition = "TEXT")
    private String followUpQuestion;

    /**
     * Expected answer points / Key points to cover
     */
    @Column(name = "expected_points", columnDefinition = "TEXT")
    private String expectedPoints;

    /**
     * Sample answer for reference
     */
    @Column(name = "sample_answer", columnDefinition = "TEXT")
    private String sampleAnswer;

    /**
     * Related skill being assessed
     */
    @Column(name = "related_skill")
    private String relatedSkill;

    /**
     * Time limit in seconds (optional)
     */
    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    /**
     * Is the question answered
     */
    @Column(name = "is_answered")
    @Builder.Default
    private Boolean isAnswered = false;

    /**
     * Is the question skipped
     */
    @Column(name = "is_skipped")
    @Builder.Default
    private Boolean isSkipped = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ================== Relationship ==================

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private MockAnswer answer;

    // ================== Enums ==================

    public enum QuestionCategory {
        // Technical
        DATA_STRUCTURES,
        ALGORITHMS,
        SYSTEM_DESIGN,
        DATABASE,
        NETWORKING,
        SECURITY,
        PROGRAMMING,
        
        // Behavioral
        LEADERSHIP,
        TEAMWORK,
        CONFLICT_RESOLUTION,
        PROBLEM_SOLVING,
        COMMUNICATION,
        ADAPTABILITY,
        
        // Situational
        SITUATIONAL,
        CASE_STUDY,
        
        // Other
        GENERAL,
        COMPANY_SPECIFIC
    }

    public enum QuestionDifficulty {
        EASY,
        MEDIUM,
        HARD,
        EXPERT
    }

    // ================== Helper Methods ==================

    public void markAnswered() {
        this.isAnswered = true;
        this.isSkipped = false;
    }

    public void markSkipped() {
        this.isSkipped = true;
        this.isAnswered = false;
    }

    public void setAnswerEntity(MockAnswer answer) {
        this.answer = answer;
        answer.setQuestion(this);
    }

    public boolean isTechnical() {
        return category == QuestionCategory.DATA_STRUCTURES
                || category == QuestionCategory.ALGORITHMS
                || category == QuestionCategory.SYSTEM_DESIGN
                || category == QuestionCategory.DATABASE
                || category == QuestionCategory.NETWORKING
                || category == QuestionCategory.SECURITY
                || category == QuestionCategory.PROGRAMMING;
    }

    public boolean isBehavioral() {
        return category == QuestionCategory.LEADERSHIP
                || category == QuestionCategory.TEAMWORK
                || category == QuestionCategory.CONFLICT_RESOLUTION
                || category == QuestionCategory.PROBLEM_SOLVING
                || category == QuestionCategory.COMMUNICATION
                || category == QuestionCategory.ADAPTABILITY;
    }
}
