package com.cvibe.biz.interview.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Question Template Entity
 * Admin-managed question bank for interviews
 */
@Entity
@Table(name = "question_templates")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private QuestionCategory category;

    @Column(name = "subcategory", length = 50)
    private String subcategory;  // e.g., "technical_skills", "leadership"

    @Column(name = "question_type", length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QuestionType questionType = QuestionType.OPEN_ENDED;

    @Column(name = "difficulty_level")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DifficultyLevel difficultyLevel = DifficultyLevel.STANDARD;

    @Column(name = "expected_response_type", length = 50)
    private String expectedResponseType;  // e.g., "date_range", "company_name", "skill_list"

    @Column(name = "follow_up_prompts", columnDefinition = "TEXT")
    private String followUpPrompts;  // JSON array of follow-up prompts

    @Column(name = "extraction_hints", columnDefinition = "TEXT")
    private String extractionHints;  // JSON hints for AI extraction

    @Column(name = "example_answer", columnDefinition = "TEXT")
    private String exampleAnswer;  // Example of a good answer

    @Column(name = "order_weight")
    @Builder.Default
    private Integer orderWeight = 100;  // For ordering questions

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = false;  // Must be asked in every session

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "target_roles", columnDefinition = "TEXT")
    private String targetRoles;  // JSON array of roles this question applies to

    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "en";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum QuestionCategory {
        PERSONAL_INFO,
        WORK_EXPERIENCE,
        EDUCATION,
        SKILLS,
        CERTIFICATIONS,
        PROJECTS,
        CAREER_GOALS,
        ACHIEVEMENTS,
        SOFT_SKILLS
    }

    public enum QuestionType {
        OPEN_ENDED,
        STRUCTURED,
        MULTI_PART,
        CLARIFICATION
    }

    public enum DifficultyLevel {
        BASIC,
        STANDARD,
        DETAILED,
        DEEP_DIVE
    }
}
