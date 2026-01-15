package com.cvibe.biz.profile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating/updating profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {

    @Size(max = 200)
    private String headline;

    @Size(max = 5000)
    private String summary;

    @Size(max = 100)
    private String currentTitle;

    @Size(max = 100)
    private String currentCompany;

    @Size(max = 100)
    private String location;

    private Integer yearsOfExperience;

    @Size(max = 20)
    private String phone;

    private String linkedinUrl;

    private String githubUrl;

    private String portfolioUrl;

    @Valid
    private List<ExperienceDto> experiences;

    @Valid
    private List<EducationDto> educations;

    @Valid
    private List<SkillDto> skills;

    @Valid
    private List<CertificationDto> certifications;

    @Valid
    private List<ProjectDto> projects;
}
