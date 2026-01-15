package com.cvibe.biz.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for profile data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private UUID id;
    private UUID userId;

    // Basic Info
    private String headline;
    private String summary;
    private String currentTitle;
    private String currentCompany;
    private String location;
    private Integer yearsOfExperience;

    // Contact
    private String phone;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;

    // Stats
    private Integer completenessScore;
    private Instant lastInterviewAt;

    // Collections
    private List<ExperienceDto> experiences;
    private List<EducationDto> educations;
    private List<SkillDto> skills;
    private List<CertificationDto> certifications;
    private List<ProjectDto> projects;

    // Audit
    private Instant createdAt;
    private Instant updatedAt;
}
