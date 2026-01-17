package com.cvibe.job.entity;

import com.cvibe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JobMatch entity representing a match between a user and a job
 */
@Entity
@Table(name = "job_matches", indexes = {
    @Index(name = "idx_job_matches_user", columnList = "user_id"),
    @Index(name = "idx_job_matches_job", columnList = "job_id"),
    @Index(name = "idx_job_matches_score", columnList = "match_score"),
    @Index(name = "idx_job_matches_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_job_matches_user_job", columnNames = {"user_id", "job_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "match_score")
    private Integer matchScore;

    /**
     * JSON containing match reasons
     */
    @Column(name = "match_reasons_json", columnDefinition = "TEXT")
    private String matchReasonsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MatchStatus status = MatchStatus.NEW;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
