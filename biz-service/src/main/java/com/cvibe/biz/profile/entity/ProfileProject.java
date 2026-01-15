package com.cvibe.biz.profile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Project Entity - Portfolio projects
 */
@Entity
@Table(name = "profile_projects")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "role", length = 100)
    private String role;  // e.g., "Lead Developer"

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "project_url")
    private String projectUrl;

    @Column(name = "source_url")
    private String sourceUrl;  // e.g., GitHub link

    @Column(name = "technologies", columnDefinition = "TEXT")
    private String technologies;  // JSON array

    @Column(name = "highlights", columnDefinition = "TEXT")
    private String highlights;  // JSON array of key achievements

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
