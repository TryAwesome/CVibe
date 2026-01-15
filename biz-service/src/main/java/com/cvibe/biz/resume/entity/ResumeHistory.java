package com.cvibe.biz.resume.entity;

import com.cvibe.biz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Resume History Entity - Stores generated/uploaded resume versions
 */
@Entity
@Table(name = "resume_history")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "file_path", nullable = false)
    private String filePath;  // MinIO object path

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "source")
    @Enumerated(EnumType.STRING)
    private ResumeSource source;

    @Column(name = "template_id")
    private UUID templateId;  // If generated, which template was used

    @Column(name = "target_job_title", length = 100)
    private String targetJobTitle;  // If tailored for specific job

    @Column(name = "target_company", length = 100)
    private String targetCompany;

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;  // Primary/default resume

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum ResumeSource {
        UPLOADED,       // User uploaded PDF
        GENERATED,      // Generated from profile using template
        IMPORTED        // Imported/parsed from external source
    }
}
