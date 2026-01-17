package com.cvibe.mockinterview.entity;

import com.cvibe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Mock interview session entity for practice interviews
 */
@Entity
@Table(name = "mock_interview_sessions", indexes = {
    @Index(name = "idx_mock_interview_user", columnList = "user_id"),
    @Index(name = "idx_mock_interview_status", columnList = "status"),
    @Index(name = "idx_mock_interview_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockInterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Interview type: VIDEO, AUDIO, or TEXT
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MockInterviewType type;

    /**
     * Session status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MockInterviewStatus status = MockInterviewStatus.SETUP;

    /**
     * Current question index (0-based)
     */
    @Column(name = "current_question_index")
    @Builder.Default
    private Integer currentQuestionIndex = 0;

    /**
     * Total number of questions
     */
    @Column(name = "total_questions")
    @Builder.Default
    private Integer totalQuestions = 5;

    /**
     * Interview settings (JSON)
     * Contains: targetPosition, targetCompany, difficulty, duration, etc.
     */
    @Column(name = "settings_json", columnDefinition = "TEXT")
    private String settingsJson;

    /**
     * Questions for this session (JSON array)
     */
    @Column(name = "questions_json", columnDefinition = "TEXT")
    private String questionsJson;

    /**
     * Overall feedback for the session (JSON)
     */
    @Column(name = "feedback_json", columnDefinition = "TEXT")
    private String feedbackJson;

    /**
     * Overall score (0-100)
     */
    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Get progress percentage
     */
    public int getProgressPercentage() {
        if (totalQuestions == null || totalQuestions == 0) {
            return 0;
        }
        return (currentQuestionIndex * 100) / totalQuestions;
    }

    /**
     * Check if session has more questions
     */
    public boolean hasMoreQuestions() {
        return currentQuestionIndex < totalQuestions;
    }

    /**
     * Check if session is completed
     */
    public boolean isCompleted() {
        return status == MockInterviewStatus.COMPLETED;
    }

    /**
     * Check if session is active (can accept answers)
     */
    public boolean isActive() {
        return status == MockInterviewStatus.IN_PROGRESS;
    }
}
