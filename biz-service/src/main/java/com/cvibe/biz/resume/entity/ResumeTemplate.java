package com.cvibe.biz.resume.entity;

import com.cvibe.biz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * ResumeTemplate Entity
 * 
 * Stores LaTeX resume templates - both system-provided and user-uploaded.
 * Users can select templates when generating tailored resumes.
 */
@Entity
@Table(name = "resume_templates", indexes = {
        @Index(name = "idx_template_type", columnList = "templateType"),
        @Index(name = "idx_template_category", columnList = "category"),
        @Index(name = "idx_template_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Template name for display
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Brief description of the template style
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Template type: SYSTEM (pre-built) or USER (uploaded by user)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TemplateType templateType;

    /**
     * Category for filtering (e.g., TECH, ACADEMIC, CREATIVE, MINIMAL)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TemplateCategory category;

    /**
     * The LaTeX source code of the template
     * Contains placeholders like {{NAME}}, {{EXPERIENCE}}, etc.
     */
    @Column(name = "latex_content", columnDefinition = "TEXT", nullable = false)
    private String latexContent;

    /**
     * Preview thumbnail URL (stored in MinIO/S3)
     */
    @Column(length = 500)
    private String thumbnailUrl;

    /**
     * Owner user (null for system templates)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Whether this template is active/available
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    /**
     * Whether this is a featured/popular template (for system templates)
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isFeatured = false;

    /**
     * Usage count for popularity ranking
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer usageCount = 0;

    /**
     * Version for optimistic locking
     */
    @Version
    private Integer version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // ==================== Enums ====================

    public enum TemplateType {
        SYSTEM,     // Pre-built by platform
        USER        // Uploaded by user
    }

    public enum TemplateCategory {
        TECH,       // Software engineering, IT
        ACADEMIC,   // Research, PhD, academic positions
        CREATIVE,   // Design, marketing
        MINIMAL,    // Clean, simple layouts
        PROFESSIONAL, // Executive, senior roles
        MODERN      // Contemporary designs
    }

    // ==================== Business Methods ====================

    public void incrementUsage() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }

    public boolean isSystemTemplate() {
        return this.templateType == TemplateType.SYSTEM;
    }

    public boolean isUserTemplate() {
        return this.templateType == TemplateType.USER;
    }

    public boolean belongsToUser(UUID userId) {
        return this.user != null && this.user.getId().equals(userId);
    }
}
