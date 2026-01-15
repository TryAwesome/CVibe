package com.cvibe.biz.mock.entity;

import com.cvibe.biz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MockInterview Entity
 * 
 * Represents a mock interview session with multiple rounds.
 */
@Entity
@Table(name = "mock_interviews", indexes = {
        @Index(name = "idx_mock_interviews_user_id", columnList = "user_id"),
        @Index(name = "idx_mock_interviews_status", columnList = "status"),
        @Index(name = "idx_mock_interviews_type", columnList = "interview_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class MockInterview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Target position for the interview
     */
    @Column(name = "target_position", nullable = false)
    private String targetPosition;

    /**
     * Target company (optional)
     */
    @Column(name = "target_company")
    private String targetCompany;

    /**
     * Interview type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false)
    private InterviewType interviewType;

    /**
     * Difficulty level
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    /**
     * Interview status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private InterviewStatus status = InterviewStatus.CREATED;

    /**
     * Total number of questions planned
     */
    @Column(name = "total_questions")
    @Builder.Default
    private Integer totalQuestions = 5;

    /**
     * Number of questions answered
     */
    @Column(name = "answered_questions")
    @Builder.Default
    private Integer answeredQuestions = 0;

    /**
     * Overall score (0-100)
     */
    @Column(name = "overall_score")
    private Integer overallScore;

    /**
     * Technical score (0-100)
     */
    @Column(name = "technical_score")
    private Integer technicalScore;

    /**
     * Communication score (0-100)
     */
    @Column(name = "communication_score")
    private Integer communicationScore;

    /**
     * Problem solving score (0-100)
     */
    @Column(name = "problem_solving_score")
    private Integer problemSolvingScore;

    /**
     * Overall feedback summary
     */
    @Column(name = "feedback_summary", columnDefinition = "TEXT")
    private String feedbackSummary;

    /**
     * Strengths identified
     */
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    /**
     * Areas for improvement
     */
    @Column(name = "improvements", columnDefinition = "TEXT")
    private String improvements;

    /**
     * Skills being assessed
     */
    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    /**
     * Interview started at
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * Interview completed at
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Total duration in seconds
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Notes
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ================== Relationships ==================

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<InterviewRound> rounds = new ArrayList<>();

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<MockQuestion> questions = new ArrayList<>();

    // ================== Enums ==================

    public enum InterviewType {
        BEHAVIORAL,      // Behavioral interview
        TECHNICAL,       // Technical questions
        CODING,          // Live coding
        SYSTEM_DESIGN,   // System design
        CASE_STUDY,      // Case study / Product
        MIXED            // Mixed format
    }

    public enum DifficultyLevel {
        EASY,
        MEDIUM,
        HARD,
        EXPERT
    }

    public enum InterviewStatus {
        CREATED,         // Just created, not started
        IN_PROGRESS,     // Currently in progress
        PAUSED,          // Paused by user
        COMPLETED,       // All questions answered
        EVALUATED,       // Feedback generated
        CANCELLED        // Cancelled
    }

    // ================== Helper Methods ==================

    public void addRound(InterviewRound round) {
        rounds.add(round);
        round.setInterview(this);
    }

    public void addQuestion(MockQuestion question) {
        questions.add(question);
        question.setInterview(this);
    }

    public void start() {
        this.status = InterviewStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public void pause() {
        this.status = InterviewStatus.PAUSED;
    }

    public void resume() {
        this.status = InterviewStatus.IN_PROGRESS;
    }

    public void complete() {
        this.status = InterviewStatus.COMPLETED;
        this.completedAt = Instant.now();
        if (startedAt != null) {
            this.durationSeconds = (int) (completedAt.getEpochSecond() - startedAt.getEpochSecond());
        }
    }

    public void markEvaluated(String feedback, int overallScore) {
        this.status = InterviewStatus.EVALUATED;
        this.feedbackSummary = feedback;
        this.overallScore = overallScore;
    }

    public void incrementAnswered() {
        this.answeredQuestions++;
        if (this.answeredQuestions >= this.totalQuestions) {
            complete();
        }
    }

    public void recordScores(int technical, int communication, int problemSolving) {
        this.technicalScore = technical;
        this.communicationScore = communication;
        this.problemSolvingScore = problemSolving;
        this.overallScore = (technical + communication + problemSolving) / 3;
    }

    public boolean belongsToUser(UUID userId) {
        return user != null && user.getId().equals(userId);
    }

    public boolean isInProgress() {
        return status == InterviewStatus.IN_PROGRESS;
    }

    public boolean canBeResumed() {
        return status == InterviewStatus.PAUSED || status == InterviewStatus.CREATED;
    }

    public int getProgressPercent() {
        if (totalQuestions == null || totalQuestions == 0) return 0;
        return (answeredQuestions * 100) / totalQuestions;
    }
}
