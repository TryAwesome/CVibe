package com.cvibe.job.entity;

import com.cvibe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JobSave entity representing a saved job by a user
 */
@Entity
@Table(name = "job_saves", indexes = {
    @Index(name = "idx_job_saves_user", columnList = "user_id"),
    @Index(name = "idx_job_saves_job", columnList = "job_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_job_saves_user_job", columnNames = {"user_id", "job_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSave {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
