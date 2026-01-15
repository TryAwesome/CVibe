package com.cvibe.biz.profile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Education History Entity
 */
@Entity
@Table(name = "profile_educations")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileEducation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    @Column(name = "institution", nullable = false, length = 150)
    private String institution;

    @Column(name = "degree", length = 100)
    private String degree;  // e.g., "Bachelor of Science"

    @Column(name = "field_of_study", length = 100)
    private String fieldOfStudy;  // e.g., "Computer Science"

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = false;

    @Column(name = "gpa", length = 20)
    private String gpa;  // e.g., "3.8/4.0"

    @Column(name = "activities", columnDefinition = "TEXT")
    private String activities;  // JSON array of activities

    @Column(name = "honors", columnDefinition = "TEXT")
    private String honors;  // JSON array of honors/awards

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
