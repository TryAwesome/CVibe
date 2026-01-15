package com.cvibe.biz.job.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Job Entity - Crawled job postings from various job boards
 */
@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_job_url_hash", columnList = "url_hash", unique = true),
        @Index(name = "idx_job_company", columnList = "company"),
        @Index(name = "idx_job_location", columnList = "location"),
        @Index(name = "idx_job_first_seen", columnList = "first_seen_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "url_hash", nullable = false, unique = true, length = 64)
    private String urlHash;  // SHA256 hash of source URL for deduplication

    @Column(name = "source_url", nullable = false, columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "source", length = 50)
    @Enumerated(EnumType.STRING)
    private JobSource source;

    // Basic Info
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "company", nullable = false)
    private String company;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    @Column(name = "employment_type", length = 50)
    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Column(name = "experience_level", length = 50)
    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    // Content
    @Column(name = "description_markdown", columnDefinition = "TEXT")
    private String descriptionMarkdown;  // Cleaned description for matching

    @Column(name = "requirements_json", columnDefinition = "TEXT")
    private String requirementsJson;  // JSON: { "years": 3, "tech": ["Go", "Kafka"], "education": "BS" }

    @Column(name = "raw_html_s3_url", columnDefinition = "TEXT")
    private String rawHtmlS3Url;  // Offload heavy HTML to S3

    // Metadata
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_remote")
    @Builder.Default
    private Boolean isRemote = false;

    @CreatedDate
    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @LastModifiedDate
    @Column(name = "last_crawled_at")
    private Instant lastCrawledAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    // ================== Enums ==================

    public enum JobSource {
        LINKEDIN,
        INDEED,
        GLASSDOOR,
        COMPANY_WEBSITE,
        OTHER
    }

    public enum EmploymentType {
        FULL_TIME,
        PART_TIME,
        CONTRACT,
        INTERNSHIP,
        FREELANCE,
        TEMPORARY
    }

    public enum ExperienceLevel {
        ENTRY,
        JUNIOR,
        MID,
        SENIOR,
        LEAD,
        PRINCIPAL,
        EXECUTIVE
    }
}
