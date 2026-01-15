package com.cvibe.biz.resume.entity;

import com.cvibe.biz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * ResumeGeneration Entity
 * 
 * Records each resume generation/tailoring session.
 * Tracks the input JD, template used, generated content, and final output.
 */
@Entity
@Table(name = "resume_generations", indexes = {
        @Index(name = "idx_generation_user", columnList = "user_id"),
        @Index(name = "idx_generation_status", columnList = "status"),
        @Index(name = "idx_generation_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User who requested the generation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Template used for generation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ResumeTemplate template;

    /**
     * Target job title for tailoring
     */
    @Column(length = 200)
    private String targetJobTitle;

    /**
     * Target company name
     */
    @Column(length = 200)
    private String targetCompany;

    /**
     * Job description text (extracted from upload or pasted)
     */
    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    /**
     * Path to uploaded JD file (if image/PDF was uploaded)
     */
    @Column(length = 500)
    private String jdFilePath;

    /**
     * Generated LaTeX content (with profile data injected)
     */
    @Column(name = "generated_latex", columnDefinition = "TEXT")
    private String generatedLatex;

    /**
     * User-edited final LaTeX content (after manual edits in editor)
     */
    @Column(name = "final_latex", columnDefinition = "TEXT")
    private String finalLatex;

    /**
     * Path to generated PDF file (stored in MinIO/S3)
     */
    @Column(length = 500)
    private String pdfFilePath;

    /**
     * AI analysis of how the resume was tailored
     */
    @Column(name = "tailoring_notes", columnDefinition = "TEXT")
    private String tailoringNotes;

    /**
     * Keywords extracted from JD that were emphasized
     */
    @Column(name = "matched_keywords", columnDefinition = "TEXT")
    private String matchedKeywords;

    /**
     * Generation status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GenerationStatus status = GenerationStatus.PENDING;

    /**
     * Error message if generation failed
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Whether the user exported this as final PDF
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isExported = false;

    /**
     * Export timestamp
     */
    private Instant exportedAt;

    /**
     * User satisfaction rating (1-5)
     */
    @Column
    private Integer userRating;

    /**
     * User feedback on the generation
     */
    @Column(columnDefinition = "TEXT")
    private String userFeedback;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // ==================== Enums ====================

    public enum GenerationStatus {
        PENDING,        // Request received
        ANALYZING_JD,   // Parsing job description
        GENERATING,     // Creating tailored content
        COMPILING,      // Compiling LaTeX to PDF
        COMPLETED,      // Successfully generated
        FAILED          // Generation failed
    }

    // ==================== Business Methods ====================

    public void markAsAnalyzing() {
        this.status = GenerationStatus.ANALYZING_JD;
    }

    public void markAsGenerating() {
        this.status = GenerationStatus.GENERATING;
    }

    public void markAsCompiling() {
        this.status = GenerationStatus.COMPILING;
    }

    public void markAsCompleted(String pdfPath) {
        this.status = GenerationStatus.COMPLETED;
        this.pdfFilePath = pdfPath;
    }

    public void markAsFailed(String error) {
        this.status = GenerationStatus.FAILED;
        this.errorMessage = error;
    }

    public void markAsExported() {
        this.isExported = true;
        this.exportedAt = Instant.now();
    }

    public void updateFinalLatex(String latex) {
        this.finalLatex = latex;
    }

    public void submitRating(Integer rating, String feedback) {
        this.userRating = rating;
        this.userFeedback = feedback;
    }

    public boolean isCompleted() {
        return this.status == GenerationStatus.COMPLETED;
    }

    public boolean belongsToUser(UUID userId) {
        return this.user != null && this.user.getId().equals(userId);
    }
}
