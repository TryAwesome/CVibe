package com.cvibe.biz.profile.service;

import com.cvibe.biz.profile.dto.*;
import com.cvibe.biz.profile.entity.*;
import com.cvibe.biz.profile.repository.*;
import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.response.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserProfileRepository profileRepository;
    private final ProfileExperienceRepository experienceRepository;
    private final ProfileEducationRepository educationRepository;
    private final ProfileSkillRepository skillRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get user profile
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        UserProfile profile = profileRepository.findByUserIdWithAllDetails(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
        return mapToResponse(profile);
    }

    /**
     * Get profile or create empty one
     */
    @Transactional
    public ProfileResponse getOrCreateProfile(UUID userId) {
        return profileRepository.findByUserIdWithAllDetails(userId)
                .map(this::mapToResponse)
                .orElseGet(() -> createEmptyProfile(userId));
    }

    /**
     * Create empty profile for user
     */
    @Transactional
    public ProfileResponse createEmptyProfile(UUID userId) {
        if (profileRepository.existsByUserId(userId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Profile already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserProfile profile = UserProfile.builder()
                .user(user)
                .completenessScore(0)
                .build();

        profile = profileRepository.save(profile);
        log.info("Created empty profile for user: {}", userId);
        return mapToResponse(profile);
    }

    /**
     * Update profile with full data
     */
    @Transactional
    public ProfileResponse updateProfile(UUID userId, ProfileRequest request) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    return UserProfile.builder().user(user).build();
                });

        // Update basic info
        profile.setHeadline(request.getHeadline());
        profile.setSummary(request.getSummary());
        profile.setCurrentTitle(request.getCurrentTitle());
        profile.setCurrentCompany(request.getCurrentCompany());
        profile.setLocation(request.getLocation());
        profile.setYearsOfExperience(request.getYearsOfExperience());
        profile.setPhone(request.getPhone());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setGithubUrl(request.getGithubUrl());
        profile.setPortfolioUrl(request.getPortfolioUrl());

        // Update experiences
        if (request.getExperiences() != null) {
            profile.getExperiences().clear();
            for (ExperienceDto dto : request.getExperiences()) {
                ProfileExperience exp = mapToExperience(dto);
                profile.addExperience(exp);
            }
        }

        // Update educations
        if (request.getEducations() != null) {
            profile.getEducations().clear();
            for (EducationDto dto : request.getEducations()) {
                ProfileEducation edu = mapToEducation(dto);
                profile.addEducation(edu);
            }
        }

        // Update skills
        if (request.getSkills() != null) {
            profile.getSkills().clear();
            for (SkillDto dto : request.getSkills()) {
                ProfileSkill skill = mapToSkill(dto);
                profile.addSkill(skill);
            }
        }

        // Update certifications
        if (request.getCertifications() != null) {
            profile.getCertifications().clear();
            for (CertificationDto dto : request.getCertifications()) {
                ProfileCertification cert = mapToCertification(dto);
                cert.setProfile(profile);
                profile.getCertifications().add(cert);
            }
        }

        // Update projects
        if (request.getProjects() != null) {
            profile.getProjects().clear();
            for (ProjectDto dto : request.getProjects()) {
                ProfileProject proj = mapToProject(dto);
                proj.setProfile(profile);
                profile.getProjects().add(proj);
            }
        }

        // Calculate completeness
        profile.calculateCompleteness();

        profile = profileRepository.save(profile);
        log.info("Updated profile for user: {}", userId);
        return mapToResponse(profile);
    }

    /**
     * Get all experiences for a user
     */
    @Transactional(readOnly = true)
    public List<ExperienceDto> getExperiences(UUID userId) {
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            return List.of();
        }
        return profile.getExperiences().stream()
                .map(this::mapToExperienceDto)
                .toList();
    }

    /**
     * Get all skills for a user
     */
    @Transactional(readOnly = true)
    public List<SkillDto> getSkills(UUID userId) {
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            return List.of();
        }
        return profile.getSkills().stream()
                .map(this::mapToSkillDto)
                .toList();
    }

    /**
     * Add experience to profile
     */
    @Transactional
    public ExperienceDto addExperience(UUID userId, ExperienceDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        ProfileExperience experience = mapToExperience(dto);
        profile.addExperience(experience);
        profile.calculateCompleteness();
        profileRepository.save(profile);
        log.info("Added experience to profile: userId={}", userId);
        return mapToExperienceDto(experience);
    }

    /**
     * Update experience
     */
    @Transactional
    public ExperienceDto updateExperience(UUID userId, UUID experienceId, ExperienceDto dto) {
        ProfileExperience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!experience.getProfile().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        experience.setCompany(dto.getCompany());
        experience.setTitle(dto.getTitle());
        experience.setLocation(dto.getLocation());
        experience.setEmploymentType(dto.getEmploymentType());
        experience.setStartDate(dto.getStartDate());
        experience.setEndDate(dto.getEndDate());
        experience.setIsCurrent(dto.getIsCurrent() != null && dto.getIsCurrent());
        experience.setDescription(dto.getDescription());
        experience.setAchievements(toJson(dto.getAchievements()));
        experience.setTechnologies(toJson(dto.getTechnologies()));

        experienceRepository.save(experience);
        experience.getProfile().calculateCompleteness();
        profileRepository.save(experience.getProfile());

        return mapToExperienceDto(experience);
    }

    /**
     * Delete experience
     */
    @Transactional
    public void deleteExperience(UUID userId, UUID experienceId) {
        ProfileExperience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!experience.getProfile().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        UserProfile profile = experience.getProfile();
        profile.getExperiences().remove(experience);
        profile.calculateCompleteness();
        profileRepository.save(profile);
        log.info("Deleted experience: {}", experienceId);
    }

    /**
     * Add skill to profile
     */
    @Transactional
    public SkillDto addSkill(UUID userId, SkillDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        ProfileSkill skill = mapToSkill(dto);
        profile.addSkill(skill);
        profile.calculateCompleteness();
        profileRepository.save(profile);
        return mapToSkillDto(skill);
    }

    /**
     * Delete skill
     */
    @Transactional
    public void deleteSkill(UUID userId, UUID skillId) {
        ProfileSkill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!skill.getProfile().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        UserProfile profile = skill.getProfile();
        profile.getSkills().remove(skill);
        profile.calculateCompleteness();
        profileRepository.save(profile);
    }

    // ==================== Helper Methods ====================

    private UserProfile getOrCreateProfileEntity(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    UserProfile newProfile = UserProfile.builder().user(user).build();
                    return profileRepository.save(newProfile);
                });
    }

    private ProfileResponse mapToResponse(UserProfile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .headline(profile.getHeadline())
                .summary(profile.getSummary())
                .currentTitle(profile.getCurrentTitle())
                .currentCompany(profile.getCurrentCompany())
                .location(profile.getLocation())
                .yearsOfExperience(profile.getYearsOfExperience())
                .phone(profile.getPhone())
                .linkedinUrl(profile.getLinkedinUrl())
                .githubUrl(profile.getGithubUrl())
                .portfolioUrl(profile.getPortfolioUrl())
                .completenessScore(profile.getCompletenessScore())
                .lastInterviewAt(profile.getLastInterviewAt())
                .experiences(profile.getExperiences().stream()
                        .map(this::mapToExperienceDto).collect(Collectors.toList()))
                .educations(profile.getEducations().stream()
                        .map(this::mapToEducationDto).collect(Collectors.toList()))
                .skills(profile.getSkills().stream()
                        .map(this::mapToSkillDto).collect(Collectors.toList()))
                .certifications(profile.getCertifications().stream()
                        .map(this::mapToCertificationDto).collect(Collectors.toList()))
                .projects(profile.getProjects().stream()
                        .map(this::mapToProjectDto).collect(Collectors.toList()))
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private ProfileExperience mapToExperience(ExperienceDto dto) {
        return ProfileExperience.builder()
                .company(dto.getCompany())
                .title(dto.getTitle())
                .location(dto.getLocation())
                .employmentType(dto.getEmploymentType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .isCurrent(dto.getIsCurrent() != null && dto.getIsCurrent())
                .description(dto.getDescription())
                .achievements(toJson(dto.getAchievements()))
                .technologies(toJson(dto.getTechnologies()))
                .build();
    }

    private ExperienceDto mapToExperienceDto(ProfileExperience exp) {
        return ExperienceDto.builder()
                .id(exp.getId())
                .company(exp.getCompany())
                .title(exp.getTitle())
                .location(exp.getLocation())
                .employmentType(exp.getEmploymentType())
                .startDate(exp.getStartDate())
                .endDate(exp.getEndDate())
                .isCurrent(exp.getIsCurrent())
                .description(exp.getDescription())
                .achievements(fromJson(exp.getAchievements()))
                .technologies(fromJson(exp.getTechnologies()))
                .build();
    }

    private ProfileEducation mapToEducation(EducationDto dto) {
        return ProfileEducation.builder()
                .institution(dto.getInstitution())
                .degree(dto.getDegree())
                .fieldOfStudy(dto.getFieldOfStudy())
                .location(dto.getLocation())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .isCurrent(dto.getIsCurrent() != null && dto.getIsCurrent())
                .gpa(dto.getGpa())
                .activities(toJson(dto.getActivities()))
                .honors(toJson(dto.getHonors()))
                .build();
    }

    private EducationDto mapToEducationDto(ProfileEducation edu) {
        return EducationDto.builder()
                .id(edu.getId())
                .institution(edu.getInstitution())
                .degree(edu.getDegree())
                .fieldOfStudy(edu.getFieldOfStudy())
                .location(edu.getLocation())
                .startDate(edu.getStartDate())
                .endDate(edu.getEndDate())
                .isCurrent(edu.getIsCurrent())
                .gpa(edu.getGpa())
                .activities(fromJson(edu.getActivities()))
                .honors(fromJson(edu.getHonors()))
                .build();
    }

    private ProfileSkill mapToSkill(SkillDto dto) {
        return ProfileSkill.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .proficiencyLevel(dto.getProficiencyLevel())
                .yearsOfExperience(dto.getYearsOfExperience())
                .isPrimary(dto.getIsPrimary() != null && dto.getIsPrimary())
                .build();
    }

    private SkillDto mapToSkillDto(ProfileSkill skill) {
        return SkillDto.builder()
                .id(skill.getId())
                .name(skill.getName())
                .category(skill.getCategory())
                .proficiencyLevel(skill.getProficiencyLevel())
                .yearsOfExperience(skill.getYearsOfExperience())
                .isPrimary(skill.getIsPrimary())
                .build();
    }

    private ProfileCertification mapToCertification(CertificationDto dto) {
        return ProfileCertification.builder()
                .name(dto.getName())
                .issuingOrganization(dto.getIssuingOrganization())
                .issueDate(dto.getIssueDate())
                .expirationDate(dto.getExpirationDate())
                .credentialId(dto.getCredentialId())
                .credentialUrl(dto.getCredentialUrl())
                .build();
    }

    private CertificationDto mapToCertificationDto(ProfileCertification cert) {
        return CertificationDto.builder()
                .id(cert.getId())
                .name(cert.getName())
                .issuingOrganization(cert.getIssuingOrganization())
                .issueDate(cert.getIssueDate())
                .expirationDate(cert.getExpirationDate())
                .credentialId(cert.getCredentialId())
                .credentialUrl(cert.getCredentialUrl())
                .build();
    }

    private ProfileProject mapToProject(ProjectDto dto) {
        return ProfileProject.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .role(dto.getRole())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .projectUrl(dto.getProjectUrl())
                .sourceUrl(dto.getSourceUrl())
                .technologies(toJson(dto.getTechnologies()))
                .highlights(toJson(dto.getHighlights()))
                .build();
    }

    private ProjectDto mapToProjectDto(ProfileProject proj) {
        return ProjectDto.builder()
                .id(proj.getId())
                .name(proj.getName())
                .description(proj.getDescription())
                .role(proj.getRole())
                .startDate(proj.getStartDate())
                .endDate(proj.getEndDate())
                .projectUrl(proj.getProjectUrl())
                .sourceUrl(proj.getSourceUrl())
                .technologies(fromJson(proj.getTechnologies()))
                .highlights(fromJson(proj.getHighlights()))
                .build();
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize list to JSON", e);
            return null;
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize JSON to list", e);
            return new ArrayList<>();
        }
    }
}
