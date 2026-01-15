package com.cvibe.biz.profile.entity;

import com.cvibe.biz.user.entity.User;
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
 * User Profile Entity - Core professional profile information
 * Built from AI Interview or imported from resume PDF
 */
@Entity
@Table(name = "user_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Basic Info
    @Column(name = "headline", length = 200)
    private String headline;  // e.g., "Senior Software Engineer at Google"

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;  // Professional summary

    @Column(name = "current_title", length = 100)
    private String currentTitle;

    @Column(name = "current_company", length = 100)
    private String currentCompany;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    // Contact Info
    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "portfolio_url")
    private String portfolioUrl;

    // Profile Completeness
    @Column(name = "completeness_score")
    @Builder.Default
    private Integer completenessScore = 0;  // 0-100%

    @Column(name = "last_interview_at")
    private Instant lastInterviewAt;  // Last AI interview session

    // Related Collections
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("startDate DESC")
    private List<ProfileExperience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("startDate DESC")
    private List<ProfileEducation> educations = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProfileSkill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("issueDate DESC")
    private List<ProfileCertification> certifications = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProfileProject> projects = new ArrayList<>();

    // Audit
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Add experience and set bidirectional relationship
     */
    public void addExperience(ProfileExperience experience) {
        experiences.add(experience);
        experience.setProfile(this);
    }

    /**
     * Add education and set bidirectional relationship
     */
    public void addEducation(ProfileEducation education) {
        educations.add(education);
        education.setProfile(this);
    }

    /**
     * Add skill and set bidirectional relationship
     */
    public void addSkill(ProfileSkill skill) {
        skills.add(skill);
        skill.setProfile(this);
    }

    /**
     * Calculate and update profile completeness score
     */
    public void calculateCompleteness() {
        int score = 0;
        
        // Basic info (30%)
        if (headline != null && !headline.isEmpty()) score += 5;
        if (summary != null && !summary.isEmpty()) score += 10;
        if (currentTitle != null && !currentTitle.isEmpty()) score += 5;
        if (location != null && !location.isEmpty()) score += 5;
        if (yearsOfExperience != null) score += 5;
        
        // Experience (30%)
        if (!experiences.isEmpty()) {
            score += Math.min(experiences.size() * 10, 30);
        }
        
        // Education (15%)
        if (!educations.isEmpty()) {
            score += Math.min(educations.size() * 8, 15);
        }
        
        // Skills (15%)
        if (!skills.isEmpty()) {
            score += Math.min(skills.size() * 3, 15);
        }
        
        // Extras (10%)
        if (!certifications.isEmpty()) score += 5;
        if (!projects.isEmpty()) score += 5;
        
        this.completenessScore = Math.min(score, 100);
    }
}
