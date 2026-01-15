package com.cvibe.biz.interview.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Interview Answer Entity
 * Stores user's answer to each interview question
 */
@Entity
@Table(name = "interview_answers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuestionTemplate question;  // Reference to template question (if used)

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;  // Actual question asked (may be AI-generated follow-up)

    @Column(name = "question_category", length = 50)
    private String questionCategory;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "is_follow_up")
    @Builder.Default
    private Boolean isFollowUp = false;  // AI-generated follow-up question

    @Column(name = "follow_up_depth")
    @Builder.Default
    private Integer followUpDepth = 0;  // How many levels deep this follow-up is

    @Column(name = "parent_answer_id")
    private UUID parentAnswerId;  // Reference to the answer this follow-up relates to

    // AI Analysis
    @Column(name = "ai_analysis", columnDefinition = "TEXT")
    private String aiAnalysis;  // AI's analysis of the answer

    @Column(name = "extracted_entities", columnDefinition = "TEXT")
    private String extractedEntities;  // JSON of entities extracted from answer

    @Column(name = "confidence_score")
    private Double confidenceScore;  // AI's confidence in extraction (0-1)

    @Column(name = "needs_clarification")
    @Builder.Default
    private Boolean needsClarification = false;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Mark as answered
     */
    public void markAnswered(String answer) {
        this.answerText = answer;
        this.answeredAt = Instant.now();
    }
}
