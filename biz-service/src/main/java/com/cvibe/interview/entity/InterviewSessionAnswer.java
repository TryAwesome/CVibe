package com.cvibe.interview.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Answer entity for interview session questions
 */
@Entity
@Table(name = "interview_session_answers", indexes = {
    @Index(name = "idx_answer_session", columnList = "session_id"),
    @Index(name = "idx_answer_question", columnList = "question_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSessionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    /**
     * Question ID (reference to the question in questionsJson)
     */
    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    /**
     * The question text
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    /**
     * User's answer
     */
    @Column(columnDefinition = "TEXT")
    private String answer;

    /**
     * Question category
     */
    @Column(length = 50)
    private String category;

    /**
     * AI-generated score (0-100)
     */
    private Integer score;

    /**
     * AI-generated feedback (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String feedback;

    /**
     * Order of the question in the session
     */
    @Column(name = "question_order")
    private Integer questionOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
