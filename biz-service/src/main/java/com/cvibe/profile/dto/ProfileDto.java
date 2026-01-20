package com.cvibe.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for user profile response.
 * 包含完整的用户资料信息，包括所有嵌套的子资源。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {

    private String id;
    private String userId;
    private String headline;
    private String summary;
    private String location;
    private List<ExperienceDto> experiences;
    private List<EducationDto> educations;
    private List<SkillDto> skills;
    private List<ProjectDto> projects;
    private List<LanguageDto> languages;
    private List<CertificationDto> certifications;
    private String createdAt;
    private String updatedAt;
}
