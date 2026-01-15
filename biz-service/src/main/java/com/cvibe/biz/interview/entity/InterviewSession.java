package com.cvibe.biz.interview.entity;

import com.cvibe.biz.user.entity.User;
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
 * Interview Session Entity
 * Represents a complete AI interview session for building user profile
 */
@Entity
@Table(name = "interview_sessions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SessionType sessionType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(name = "current_question_index")
    @Builder.Default
    private Integer currentQuestionIndex = 0;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "focus_area", length = 100)
    private String focusArea;  // e.g., "work_experience", "education", "skills"

    @Column(name = "target_role", length = 100)
    private String targetRole;  // Target job role for tailored questions

    // Session Metadata
    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    // Profile extraction results
    @Column(name = "extracted_data", columnDefinition = "TEXT")
    private String extractedData;  // JSON of extracted profile data

    @Column(name = "extraction_status")
    @Enumerated(EnumType.STRING)
    private ExtractionStatus extractionStatus;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("questionOrder ASC")
    private List<InterviewAnswer> answers = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum SessionType {
        INITIAL_PROFILE,     // First-time profile building
        DEEP_DIVE,           // Deep dive into specific area
        UPDATE_EXPERIENCE,   // Update work experience
        UPDATE_EDUCATION,    // Update education
        UPDATE_SKILLS,       // Update skills
        CAREER_GOALS         // Career goals and aspirations
    }

    public enum SessionStatus {
        IN_PROGRESS,
        PAUSED,
        COMPLETED,
        ABANDONED
    }

    public enum ExtractionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    /**
     * Add answer to session
     */
    public void addAnswer(InterviewAnswer answer) {
        answers.add(answer);
        answer.setSession(this);
        this.lastActivityAt = Instant.now();
    }

    /**
     * Check if session is active
     */
    public boolean isActive() {
        return status == SessionStatus.IN_PROGRESS || status == SessionStatus.PAUSED;
    }

    /**
     * Get progress percentage
     */
    public int getProgressPercentage() {
        if (totalQuestions == null || totalQuestions == 0) return 0;
        return (currentQuestionIndex * 100) / totalQuestions;
    }
}
