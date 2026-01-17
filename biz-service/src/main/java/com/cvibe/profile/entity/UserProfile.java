package com.cvibe.profile.entity;

import com.cvibe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * User profile entity containing professional information.
 */
@Entity
@Table(name = "user_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 200)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 100)
    private String location;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("startDate DESC")
    @Builder.Default
    private List<ProfileExperience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProfileSkill> skills = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Helper methods for bidirectional relationship management
    public void addExperience(ProfileExperience experience) {
        experiences.add(experience);
        experience.setProfile(this);
    }

    public void removeExperience(ProfileExperience experience) {
        experiences.remove(experience);
        experience.setProfile(null);
    }

    public void addSkill(ProfileSkill skill) {
        skills.add(skill);
        skill.setProfile(this);
    }

    public void removeSkill(ProfileSkill skill) {
        skills.remove(skill);
        skill.setProfile(null);
    }
}
