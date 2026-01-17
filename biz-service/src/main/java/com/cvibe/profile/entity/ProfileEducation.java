package com.cvibe.profile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Education entity for user profiles.
 */
@Entity
@Table(name = "profile_educations")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileEducation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    @Column(nullable = false, length = 200)
    private String school;

    @Column(length = 100)
    private String degree;

    @Column(name = "field_of_study", length = 100)
    private String fieldOfStudy;

    @Column(length = 100)
    private String location;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = false;

    @Column(length = 20)
    private String gpa;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Activities and achievements stored as JSON array string.
     */
    @Column(columnDefinition = "TEXT")
    private String activities;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
