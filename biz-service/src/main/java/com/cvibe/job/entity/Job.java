package com.cvibe.job.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Job entity representing a job posting
 */
@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_jobs_company", columnList = "company"),
    @Index(name = "idx_jobs_location", columnList = "location"),
    @Index(name = "idx_jobs_type", columnList = "type"),
    @Index(name = "idx_jobs_is_remote", columnList = "is_remote"),
    @Index(name = "idx_jobs_posted_at", columnList = "posted_at"),
    @Index(name = "idx_jobs_source_id", columnList = "source_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String company;

    @Column(name = "company_logo", length = 500)
    private String companyLogo;

    @Column(length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobType type;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "salary_currency", length = 10)
    private String salaryCurrency;

    @Column(name = "salary_period", length = 20)
    private String salaryPeriod;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * JSON array of requirements
     */
    @Column(columnDefinition = "TEXT")
    private String requirements;

    /**
     * JSON array of responsibilities
     */
    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    /**
     * JSON array of benefits
     */
    @Column(columnDefinition = "TEXT")
    private String benefits;

    /**
     * JSON array of skills
     */
    @Column(columnDefinition = "TEXT")
    private String skills;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", length = 20)
    private ExperienceLevel experienceLevel;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(length = 50)
    private String source;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_id", length = 100)
    private String sourceId;

    @Column(name = "is_remote", nullable = false)
    @Builder.Default
    private Boolean isRemote = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
