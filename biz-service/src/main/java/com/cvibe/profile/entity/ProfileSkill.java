package com.cvibe.profile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Skill entity for user profiles.
 */
@Entity
@Table(name = "profile_skills", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"profile_id", "name"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    @Column(nullable = false, length = 50)
    private String name;

    /**
     * Skill level: BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
     */
    @Column(length = 20)
    private String level;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
