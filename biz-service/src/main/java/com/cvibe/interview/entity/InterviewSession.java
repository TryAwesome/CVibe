package com.cvibe.interview.entity;

import com.cvibe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Interview session entity for profile collection interviews
 */
@Entity
@Table(name = "interview_sessions", indexes = {
    @Index(name = "idx_interview_session_user", columnList = "user_id"),
    @Index(name = "idx_interview_session_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Session type: INITIAL_PROFILE or DEEP_DIVE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 30)
    private SessionType sessionType;

    /**
     * Session status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.IN_PROGRESS;

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
    private Integer totalQuestions = 10;

    /**
     * Focus area for the session
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "focus_area", length = 30)
    private FocusArea focusArea;

    /**
     * Target role/position
     */
    @Column(name = "target_role", length = 100)
    private String targetRole;

    /**
     * Language for the interview
     */
    @Column(length = 10)
    @Builder.Default
    private String language = "en";

    /**
     * Extraction status for profile data
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", length = 20)
    @Builder.Default
    private ExtractionStatus extractionStatus = ExtractionStatus.PENDING;

    /**
     * Questions for this session (JSON array)
     */
    @Column(name = "questions_json", columnDefinition = "TEXT")
    private String questionsJson;

    /**
     * Extracted profile data (JSON)
     */
    @Column(name = "extracted_data", columnDefinition = "TEXT")
    private String extractedData;

    /**
     * Answers for this session
     */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InterviewSessionAnswer> answers = new ArrayList<>();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Get the count of answered questions
     */
    public int getAnsweredCount() {
        return answers != null ? answers.size() : 0;
    }

    /**
     * Get progress percentage
     */
    public int getProgressPercentage() {
        if (totalQuestions == null || totalQuestions == 0) {
            return 0;
        }
        return (getAnsweredCount() * 100) / totalQuestions;
    }

    /**
     * Check if session has more questions
     */
    public boolean hasMoreQuestions() {
        return currentQuestionIndex < totalQuestions - 1;
    }

    /**
     * Check if session is completed
     */
    public boolean isCompleted() {
        return status == SessionStatus.COMPLETED;
    }
}
