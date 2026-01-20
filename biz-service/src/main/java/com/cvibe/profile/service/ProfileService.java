package com.cvibe.profile.service;

import com.cvibe.auth.entity.User;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import com.cvibe.profile.dto.CertificationDto;
import com.cvibe.profile.dto.EducationDto;
import com.cvibe.profile.dto.ExperienceDto;
import com.cvibe.profile.dto.LanguageDto;
import com.cvibe.profile.dto.ProfileDto;
import com.cvibe.profile.dto.ProfileRequest;
import com.cvibe.profile.dto.ProjectDto;
import com.cvibe.profile.dto.SkillDto;
import com.cvibe.profile.entity.EmploymentType;
import com.cvibe.profile.entity.ProfileCertification;
import com.cvibe.profile.entity.ProfileEducation;
import com.cvibe.profile.entity.ProfileExperience;
import com.cvibe.profile.entity.ProfileLanguage;
import com.cvibe.profile.entity.ProfileProject;
import com.cvibe.profile.entity.ProfileSkill;
import com.cvibe.profile.entity.UserProfile;
import com.cvibe.profile.repository.ProfileCertificationRepository;
import com.cvibe.profile.repository.ProfileEducationRepository;
import com.cvibe.profile.repository.ProfileExperienceRepository;
import com.cvibe.profile.repository.ProfileLanguageRepository;
import com.cvibe.profile.repository.ProfileProjectRepository;
import com.cvibe.profile.repository.ProfileSkillRepository;
import com.cvibe.profile.repository.UserProfileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user profiles.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProfileService {

    private final UserProfileRepository profileRepository;
    private final ProfileExperienceRepository experienceRepository;
    private final ProfileEducationRepository educationRepository;
    private final ProfileProjectRepository projectRepository;
    private final ProfileSkillRepository skillRepository;
    private final ProfileLanguageRepository languageRepository;
    private final ProfileCertificationRepository certificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Get or create a profile for the user.
     */
    @Transactional
    public ProfileDto getOrCreateProfile(UUID userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyProfile(userId));
        
        return toProfileDto(profile);
    }

    /**
     * Update basic profile information.
     */
    @Transactional
    public ProfileDto updateProfile(UUID userId, ProfileRequest request) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        if (request.getHeadline() != null) {
            profile.setHeadline(request.getHeadline());
        }
        if (request.getSummary() != null) {
            profile.setSummary(request.getSummary());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation());
        }
        
        profile = profileRepository.save(profile);
        return toProfileDto(profile);
    }

    /**
     * Get all experiences for a user.
     */
    @Transactional(readOnly = true)
    public List<ExperienceDto> getExperiences(UUID userId) {
        UserProfile profile = profileRepository.findByUserIdWithExperiences(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
        
        return profile.getExperiences().stream()
                .map(this::toExperienceDto)
                .collect(Collectors.toList());
    }

    /**
     * Add a new experience.
     */
    @Transactional
    public ExperienceDto addExperience(UUID userId, ExperienceDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        ProfileExperience experience = ProfileExperience.builder()
                .company(dto.getCompany())
                .title(dto.getTitle())
                .location(dto.getLocation())
                .employmentType(parseEmploymentType(dto.getEmploymentType()))
                .startDate(parseDate(dto.getStartDate()))
                .endDate(dto.getEndDate() != null ? parseDate(dto.getEndDate()) : null)
                .isCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false)
                .description(dto.getDescription())
                .achievements(toJsonArray(dto.getAchievements()))
                .technologies(toJsonArray(dto.getTechnologies()))
                .build();
        
        profile.addExperience(experience);
        profileRepository.save(profile);
        
        return toExperienceDto(experience);
    }

    /**
     * Update an existing experience.
     */
    @Transactional
    public ExperienceDto updateExperience(UUID userId, UUID experienceId, ExperienceDto dto) {
        UserProfile profile = profileRepository.findByUserIdWithExperiences(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
        
        ProfileExperience experience = profile.getExperiences().stream()
                .filter(e -> e.getId().equals(experienceId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPERIENCE_NOT_FOUND));
        
        experience.setCompany(dto.getCompany());
        experience.setTitle(dto.getTitle());
        experience.setLocation(dto.getLocation());
        experience.setEmploymentType(parseEmploymentType(dto.getEmploymentType()));
        experience.setStartDate(parseDate(dto.getStartDate()));
        experience.setEndDate(dto.getEndDate() != null ? parseDate(dto.getEndDate()) : null);
        experience.setIsCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false);
        experience.setDescription(dto.getDescription());
        experience.setAchievements(toJsonArray(dto.getAchievements()));
        experience.setTechnologies(toJsonArray(dto.getTechnologies()));
        
        experienceRepository.save(experience);
        return toExperienceDto(experience);
    }

    /**
     * Delete an experience.
     */
    @Transactional
    public void deleteExperience(UUID userId, UUID experienceId) {
        UserProfile profile = profileRepository.findByUserIdWithExperiences(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
        
        ProfileExperience experience = profile.getExperiences().stream()
                .filter(e -> e.getId().equals(experienceId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPERIENCE_NOT_FOUND));
        
        profile.removeExperience(experience);
        profileRepository.save(profile);
    }

    /**
     * Get all skills for a user.
     */
    @Transactional(readOnly = true)
    public List<SkillDto> getSkills(UUID userId) {
        UserProfile profile = profileRepository.findByUserIdWithSkills(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
        
        return profile.getSkills().stream()
                .map(this::toSkillDto)
                .collect(Collectors.toList());
    }

    /**
     * Add a new skill.
     */
    @Transactional
    public SkillDto addSkill(UUID userId, SkillDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        // Check for duplicate skill
        if (skillRepository.existsByProfileIdAndNameIgnoreCase(profile.getId(), dto.getName())) {
            throw new BusinessException(ErrorCode.SKILL_ALREADY_EXISTS);
        }
        
        ProfileSkill skill = ProfileSkill.builder()
                .name(dto.getName())
                .level(dto.getLevel())
                .build();
        
        profile.addSkill(skill);
        profileRepository.save(profile);
        
        return toSkillDto(skill);
    }

    /**
     * Delete a skill.
     */
    @Transactional
    public void deleteSkill(UUID userId, UUID skillId) {
        UserProfile profile = profileRepository.findByUserIdWithSkills(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
        
        ProfileSkill skill = profile.getSkills().stream()
                .filter(s -> s.getId().equals(skillId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SKILL_NOT_FOUND));
        
        profile.removeSkill(skill);
        profileRepository.save(profile);
    }

    // ==================== Education Methods ====================

    /**
     * Get all educations for a user.
     */
    @Transactional(readOnly = true)
    public List<EducationDto> getEducations(UUID userId) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        List<ProfileEducation> educations = educationRepository.findByProfileOrderByStartDateDesc(profile);
        return educations.stream()
                .map(this::toEducationDto)
                .collect(Collectors.toList());
    }

    /**
     * Add a new education.
     */
    @Transactional
    public EducationDto addEducation(UUID userId, EducationDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        ProfileEducation education = ProfileEducation.builder()
                .profile(profile)
                .school(dto.getSchool())
                .degree(dto.getDegree())
                .fieldOfStudy(dto.getFieldOfStudy())
                .location(dto.getLocation())
                .startDate(parseDate(dto.getStartDate()))
                .endDate(dto.getEndDate() != null ? parseDate(dto.getEndDate()) : null)
                .isCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false)
                .gpa(dto.getGpa())
                .description(dto.getDescription())
                .activities(toJsonArray(dto.getActivities()))
                .build();
        
        education = educationRepository.save(education);
        return toEducationDto(education);
    }

    /**
     * Update an existing education.
     */
    @Transactional
    public EducationDto updateEducation(UUID userId, UUID educationId, EducationDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        ProfileEducation education = educationRepository.findById(educationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EDUCATION_NOT_FOUND));
        
        // Verify ownership
        if (!education.getProfile().getId().equals(profile.getId())) {
            throw new BusinessException(ErrorCode.EDUCATION_NOT_FOUND);
        }
        
        education.setSchool(dto.getSchool());
        education.setDegree(dto.getDegree());
        education.setFieldOfStudy(dto.getFieldOfStudy());
        education.setLocation(dto.getLocation());
        education.setStartDate(parseDate(dto.getStartDate()));
        education.setEndDate(dto.getEndDate() != null ? parseDate(dto.getEndDate()) : null);
        education.setIsCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false);
        education.setGpa(dto.getGpa());
        education.setDescription(dto.getDescription());
        education.setActivities(toJsonArray(dto.getActivities()));
        
        education = educationRepository.save(education);
        return toEducationDto(education);
    }

    /**
     * Delete an education.
     */
    @Transactional
    public void deleteEducation(UUID userId, UUID educationId) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        ProfileEducation education = educationRepository.findById(educationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EDUCATION_NOT_FOUND));
        
        // Verify ownership
        if (!education.getProfile().getId().equals(profile.getId())) {
            throw new BusinessException(ErrorCode.EDUCATION_NOT_FOUND);
        }
        
        educationRepository.delete(education);
    }

    // ==================== Project Methods ====================

    /**
     * Get all projects for a user.
     */
    @Transactional(readOnly = true)
    public List<ProjectDto> getProjects(UUID userId) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        List<ProfileProject> projects = projectRepository.findByProfileOrderByStartDateDesc(profile);
        return projects.stream()
                .map(this::toProjectDto)
                .collect(Collectors.toList());
    }

    /**
     * Add a new project.
     */
    @Transactional
    public ProjectDto addProject(UUID userId, ProjectDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        ProfileProject project = ProfileProject.builder()
                .profile(profile)
                .name(dto.getName())
                .description(dto.getDescription())
                .url(dto.getUrl())
                .repoUrl(dto.getRepoUrl())
                .technologies(toJsonArray(dto.getTechnologies()))
                .startDate(parseDate(dto.getStartDate()))
                .endDate(dto.getEndDate() != null ? parseDate(dto.getEndDate()) : null)
                .isCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false)
                .highlights(toJsonArray(dto.getHighlights()))
                .build();
        
        project = projectRepository.save(project);
        return toProjectDto(project);
    }

    /**
     * Update an existing project.
     */
    @Transactional
    public ProjectDto updateProject(UUID userId, UUID projectId, ProjectDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        ProfileProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        
        // Verify ownership
        if (!project.getProfile().getId().equals(profile.getId())) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setUrl(dto.getUrl());
        project.setRepoUrl(dto.getRepoUrl());
        project.setTechnologies(toJsonArray(dto.getTechnologies()));
        project.setStartDate(parseDate(dto.getStartDate()));
        project.setEndDate(dto.getEndDate() != null ? parseDate(dto.getEndDate()) : null);
        project.setIsCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false);
        project.setHighlights(toJsonArray(dto.getHighlights()));
        
        project = projectRepository.save(project);
        return toProjectDto(project);
    }

    /**
     * Delete a project.
     */
    @Transactional
    public void deleteProject(UUID userId, UUID projectId) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        ProfileProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        
        // Verify ownership
        if (!project.getProfile().getId().equals(profile.getId())) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        
        projectRepository.delete(project);
    }

    // ==================== Helper Methods ====================

    private UserProfile getOrCreateProfileEntity(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyProfile(userId));
    }

    private UserProfile createEmptyProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        UserProfile profile = UserProfile.builder()
                .user(user)
                .build();
        
        return profileRepository.save(profile);
    }

    private ProfileDto toProfileDto(UserProfile profile) {
        // Fetch related entities via repositories since they're not mapped in UserProfile
        List<ProfileEducation> educations = educationRepository.findByProfileOrderByStartDateDesc(profile);
        List<ProfileProject> projects = projectRepository.findByProfileOrderByStartDateDesc(profile);
        List<ProfileLanguage> languages = languageRepository.findByProfileIdOrderByLanguage(profile.getId());
        List<ProfileCertification> certifications = certificationRepository.findByProfileIdOrderByIssueDateDesc(profile.getId());

        return ProfileDto.builder()
                .id(profile.getId().toString())
                .userId(profile.getUser().getId().toString())
                .headline(profile.getHeadline())
                .summary(profile.getSummary())
                .location(profile.getLocation())
                .experiences(profile.getExperiences().stream()
                        .map(this::toExperienceDto)
                        .collect(Collectors.toList()))
                .educations(educations.stream()
                        .map(this::toEducationDto)
                        .collect(Collectors.toList()))
                .skills(profile.getSkills().stream()
                        .map(this::toSkillDto)
                        .collect(Collectors.toList()))
                .projects(projects.stream()
                        .map(this::toProjectDto)
                        .collect(Collectors.toList()))
                .languages(languages.stream()
                        .map(this::toLanguageDto)
                        .collect(Collectors.toList()))
                .certifications(certifications.stream()
                        .map(this::toCertificationDto)
                        .collect(Collectors.toList()))
                .createdAt(profile.getCreatedAt() != null ? profile.getCreatedAt().toString() : null)
                .updatedAt(profile.getUpdatedAt() != null ? profile.getUpdatedAt().toString() : null)
                .build();
    }

    private ExperienceDto toExperienceDto(ProfileExperience experience) {
        return ExperienceDto.builder()
                .id(experience.getId().toString())
                .company(experience.getCompany())
                .title(experience.getTitle())
                .location(experience.getLocation())
                .employmentType(experience.getEmploymentType() != null 
                        ? experience.getEmploymentType().name() : null)
                .startDate(experience.getStartDate().format(DATE_FORMATTER))
                .endDate(experience.getEndDate() != null 
                        ? experience.getEndDate().format(DATE_FORMATTER) : null)
                .isCurrent(experience.getIsCurrent())
                .description(experience.getDescription())
                .achievements(parseJsonArray(experience.getAchievements()))
                .technologies(parseJsonArray(experience.getTechnologies()))
                .build();
    }

    private SkillDto toSkillDto(ProfileSkill skill) {
        return SkillDto.builder()
                .id(skill.getId().toString())
                .name(skill.getName())
                .level(skill.getLevel())
                .build();
    }

    private EducationDto toEducationDto(ProfileEducation education) {
        return EducationDto.builder()
                .id(education.getId().toString())
                .school(education.getSchool())
                .degree(education.getDegree())
                .fieldOfStudy(education.getFieldOfStudy())
                .location(education.getLocation())
                .startDate(education.getStartDate() != null 
                        ? education.getStartDate().format(DATE_FORMATTER) : null)
                .endDate(education.getEndDate() != null 
                        ? education.getEndDate().format(DATE_FORMATTER) : null)
                .isCurrent(education.getIsCurrent())
                .gpa(education.getGpa())
                .description(education.getDescription())
                .activities(parseJsonArray(education.getActivities()))
                .build();
    }

    private ProjectDto toProjectDto(ProfileProject project) {
        return ProjectDto.builder()
                .id(project.getId().toString())
                .name(project.getName())
                .description(project.getDescription())
                .url(project.getUrl())
                .repoUrl(project.getRepoUrl())
                .technologies(parseJsonArray(project.getTechnologies()))
                .startDate(project.getStartDate() != null 
                        ? project.getStartDate().format(DATE_FORMATTER) : null)
                .endDate(project.getEndDate() != null 
                        ? project.getEndDate().format(DATE_FORMATTER) : null)
                .isCurrent(project.getIsCurrent())
                .highlights(parseJsonArray(project.getHighlights()))
                .build();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    private EmploymentType parseEmploymentType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return EmploymentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize list to JSON", e);
            return null;
        }
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON array", e);
            return Collections.emptyList();
        }
    }

    // ==================== Language Methods ====================

    /**
     * Get all languages for a user.
     */
    @Transactional(readOnly = true)
    public List<LanguageDto> getLanguages(UUID userId) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        List<ProfileLanguage> languages = languageRepository.findByProfileIdOrderByLanguage(profile.getId());
        return languages.stream()
                .map(this::toLanguageDto)
                .collect(Collectors.toList());
    }

    /**
     * Add a new language.
     */
    @Transactional
    public LanguageDto addLanguage(UUID userId, LanguageDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        // Check for duplicate language
        if (languageRepository.existsByProfileIdAndLanguage(profile.getId(), dto.getLanguage())) {
            throw new BusinessException(ErrorCode.LANGUAGE_ALREADY_EXISTS);
        }
        
        ProfileLanguage language = ProfileLanguage.builder()
                .profile(profile)
                .language(dto.getLanguage())
                .proficiency(dto.getProficiency())
                .build();
        
        language = languageRepository.save(language);
        return toLanguageDto(language);
    }

    /**
     * Update an existing language.
     */
    @Transactional
    public LanguageDto updateLanguage(UUID userId, UUID languageId, LanguageDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        ProfileLanguage language = languageRepository.findById(languageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LANGUAGE_NOT_FOUND));
        
        // Verify ownership
        if (!language.getProfile().getId().equals(profile.getId())) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_FOUND);
        }
        
        language.setLanguage(dto.getLanguage());
        language.setProficiency(dto.getProficiency());
        
        language = languageRepository.save(language);
        return toLanguageDto(language);
    }

    /**
     * Delete a language.
     */
    @Transactional
    public void deleteLanguage(UUID userId, UUID languageId) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        ProfileLanguage language = languageRepository.findById(languageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LANGUAGE_NOT_FOUND));
        
        // Verify ownership
        if (!language.getProfile().getId().equals(profile.getId())) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_FOUND);
        }
        
        languageRepository.delete(language);
    }

    private LanguageDto toLanguageDto(ProfileLanguage language) {
        return LanguageDto.builder()
                .id(language.getId())
                .language(language.getLanguage())
                .proficiency(language.getProficiency())
                .build();
    }

    // ==================== Certification Methods ====================

    /**
     * Get all certifications for a user.
     */
    @Transactional(readOnly = true)
    public List<CertificationDto> getCertifications(UUID userId) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        List<ProfileCertification> certifications = certificationRepository.findByProfileIdOrderByIssueDateDesc(profile.getId());
        return certifications.stream()
                .map(this::toCertificationDto)
                .collect(Collectors.toList());
    }

    /**
     * Add a new certification.
     */
    @Transactional
    public CertificationDto addCertification(UUID userId, CertificationDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        ProfileCertification certification = ProfileCertification.builder()
                .profile(profile)
                .name(dto.getName())
                .issuer(dto.getIssuer())
                .issueDate(dto.getIssueDate())
                .expirationDate(dto.getExpirationDate())
                .credentialId(dto.getCredentialId())
                .credentialUrl(dto.getCredentialUrl())
                .build();
        
        certification = certificationRepository.save(certification);
        return toCertificationDto(certification);
    }

    /**
     * Update an existing certification.
     */
    @Transactional
    public CertificationDto updateCertification(UUID userId, UUID certificationId, CertificationDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        ProfileCertification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CERTIFICATION_NOT_FOUND));
        
        // Verify ownership
        if (!certification.getProfile().getId().equals(profile.getId())) {
            throw new BusinessException(ErrorCode.CERTIFICATION_NOT_FOUND);
        }
        
        certification.setName(dto.getName());
        certification.setIssuer(dto.getIssuer());
        certification.setIssueDate(dto.getIssueDate());
        certification.setExpirationDate(dto.getExpirationDate());
        certification.setCredentialId(dto.getCredentialId());
        certification.setCredentialUrl(dto.getCredentialUrl());
        
        certification = certificationRepository.save(certification);
        return toCertificationDto(certification);
    }

    /**
     * Delete a certification.
     */
    @Transactional
    public void deleteCertification(UUID userId, UUID certificationId) {
        UserProfile profile = getOrCreateProfileEntity(userId);
        
        ProfileCertification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CERTIFICATION_NOT_FOUND));
        
        // Verify ownership
        if (!certification.getProfile().getId().equals(profile.getId())) {
            throw new BusinessException(ErrorCode.CERTIFICATION_NOT_FOUND);
        }
        
        certificationRepository.delete(certification);
    }

    private CertificationDto toCertificationDto(ProfileCertification certification) {
        return CertificationDto.builder()
                .id(certification.getId())
                .name(certification.getName())
                .issuer(certification.getIssuer())
                .issueDate(certification.getIssueDate())
                .expirationDate(certification.getExpirationDate())
                .credentialId(certification.getCredentialId())
                .credentialUrl(certification.getCredentialUrl())
                .build();
    }

    // ==================== Interview Sync Methods ====================

    /**
     * Get profile for a user (returns null if not exists).
     */
    @Transactional(readOnly = true)
    public ProfileDto getProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(this::toProfileDto)
                .orElse(null);
    }

    /**
     * Sync profile data from interview extraction.
     * This method merges extracted data into the user's profile.
     *
     * @param userId User ID
     * @param extractedProfile Map containing extracted profile data from AI interview
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public void syncFromInterview(UUID userId, Map<String, Object> extractedProfile) {
        log.info("Syncing profile from interview for user {}", userId);

        UserProfile profile = getOrCreateProfileEntity(userId);

        // Update basic info
        if (extractedProfile.containsKey("headline")) {
            String headline = (String) extractedProfile.get("headline");
            if (headline != null && !headline.isBlank()) {
                profile.setHeadline(headline);
            }
        }
        if (extractedProfile.containsKey("summary")) {
            String summary = (String) extractedProfile.get("summary");
            if (summary != null && !summary.isBlank()) {
                profile.setSummary(summary);
            }
        }
        if (extractedProfile.containsKey("location")) {
            String location = (String) extractedProfile.get("location");
            if (location != null && !location.isBlank()) {
                profile.setLocation(location);
            }
        }

        profileRepository.save(profile);

        // Sync experiences
        List<Map<String, Object>> experiences = (List<Map<String, Object>>) extractedProfile.get("experiences");
        if (experiences != null && !experiences.isEmpty()) {
            syncExperiences(profile, experiences);
        }

        // Sync education
        List<Map<String, Object>> educations = (List<Map<String, Object>>) extractedProfile.get("education");
        if (educations != null && !educations.isEmpty()) {
            syncEducations(profile, educations);
        }

        // Sync projects
        List<Map<String, Object>> projects = (List<Map<String, Object>>) extractedProfile.get("projects");
        if (projects != null && !projects.isEmpty()) {
            syncProjects(profile, projects);
        }

        // Sync skills
        List<Map<String, Object>> skills = (List<Map<String, Object>>) extractedProfile.get("skills");
        if (skills != null && !skills.isEmpty()) {
            syncSkills(profile, skills);
        }

        // Sync languages
        List<Map<String, Object>> languages = (List<Map<String, Object>>) extractedProfile.get("languages");
        if (languages != null && !languages.isEmpty()) {
            syncLanguages(profile, languages);
        }

        // Sync certifications
        List<Map<String, Object>> certifications = (List<Map<String, Object>>) extractedProfile.get("certifications");
        if (certifications != null && !certifications.isEmpty()) {
            syncCertifications(profile, certifications);
        }

        log.info("Profile sync completed for user {}", userId);
    }

    @SuppressWarnings("unchecked")
    private void syncExperiences(UserProfile profile, List<Map<String, Object>> experiences) {
        for (Map<String, Object> exp : experiences) {
            String company = (String) exp.get("company");
            String title = (String) exp.get("title");

            if (company == null || company.isBlank()) continue;

            // Check if experience already exists
            boolean exists = profile.getExperiences().stream()
                    .anyMatch(e -> e.getCompany().equalsIgnoreCase(company) &&
                            (title == null || e.getTitle().equalsIgnoreCase(title)));

            if (!exists) {
                ProfileExperience experience = ProfileExperience.builder()
                        .company(company)
                        .title(title != null ? title : "")
                        .location((String) exp.get("location"))
                        .employmentType(parseEmploymentType((String) exp.get("employment_type")))
                        .startDate(parseDateFromInterview((String) exp.get("start_date")))
                        .endDate(parseDateFromInterview((String) exp.get("end_date")))
                        .isCurrent(Boolean.TRUE.equals(exp.get("is_current")))
                        .description((String) exp.get("description"))
                        .achievements(toJsonArray((List<String>) exp.get("achievements")))
                        .technologies(toJsonArray((List<String>) exp.get("technologies")))
                        .build();

                profile.addExperience(experience);
                log.debug("Added experience: {} at {}", title, company);
            }
        }
        profileRepository.save(profile);
    }

    @SuppressWarnings("unchecked")
    private void syncEducations(UserProfile profile, List<Map<String, Object>> educations) {
        for (Map<String, Object> edu : educations) {
            String school = (String) edu.get("school");
            if (school == null || school.isBlank()) continue;

            // Check if education already exists
            boolean exists = educationRepository.findByProfileOrderByStartDateDesc(profile).stream()
                    .anyMatch(e -> e.getSchool().equalsIgnoreCase(school));

            if (!exists) {
                ProfileEducation education = ProfileEducation.builder()
                        .profile(profile)
                        .school(school)
                        .degree((String) edu.get("degree"))
                        .fieldOfStudy((String) edu.get("field_of_study"))
                        .startDate(parseDateFromInterview((String) edu.get("start_date")))
                        .endDate(parseDateFromInterview((String) edu.get("end_date")))
                        .gpa((String) edu.get("gpa"))
                        .description((String) edu.get("description"))
                        .activities(toJsonArray((List<String>) edu.get("activities")))
                        .build();

                educationRepository.save(education);
                log.debug("Added education: {}", school);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void syncProjects(UserProfile profile, List<Map<String, Object>> projects) {
        for (Map<String, Object> proj : projects) {
            String name = (String) proj.get("name");
            if (name == null || name.isBlank()) continue;

            // Check if project already exists
            boolean exists = projectRepository.findByProfileOrderByStartDateDesc(profile).stream()
                    .anyMatch(p -> p.getName().equalsIgnoreCase(name));

            if (!exists) {
                ProfileProject project = ProfileProject.builder()
                        .profile(profile)
                        .name(name)
                        .description((String) proj.get("description"))
                        .url((String) proj.get("url"))
                        .repoUrl((String) proj.get("repo_url"))
                        .technologies(toJsonArray((List<String>) proj.get("technologies")))
                        .startDate(parseDateFromInterview((String) proj.get("start_date")))
                        .endDate(parseDateFromInterview((String) proj.get("end_date")))
                        .highlights(toJsonArray((List<String>) proj.get("highlights")))
                        .build();

                projectRepository.save(project);
                log.debug("Added project: {}", name);
            }
        }
    }

    private void syncSkills(UserProfile profile, List<Map<String, Object>> skills) {
        for (Map<String, Object> skill : skills) {
            String name = (String) skill.get("name");
            if (name == null || name.isBlank()) continue;

            // Check if skill already exists
            boolean exists = skillRepository.existsByProfileIdAndNameIgnoreCase(profile.getId(), name);

            if (!exists) {
                ProfileSkill profileSkill = ProfileSkill.builder()
                        .name(name)
                        .level((String) skill.get("level"))
                        .build();

                profile.addSkill(profileSkill);
                log.debug("Added skill: {}", name);
            }
        }
        profileRepository.save(profile);
    }

    private void syncLanguages(UserProfile profile, List<Map<String, Object>> languages) {
        for (Map<String, Object> lang : languages) {
            String language = (String) lang.get("language");
            if (language == null || language.isBlank()) continue;

            // Check if language already exists
            boolean exists = languageRepository.existsByProfileIdAndLanguage(profile.getId(), language);

            if (!exists) {
                ProfileLanguage profileLanguage = ProfileLanguage.builder()
                        .profile(profile)
                        .language(language)
                        .proficiency((String) lang.get("proficiency"))
                        .build();

                languageRepository.save(profileLanguage);
                log.debug("Added language: {}", language);
            }
        }
    }

    private void syncCertifications(UserProfile profile, List<Map<String, Object>> certifications) {
        for (Map<String, Object> cert : certifications) {
            String name = (String) cert.get("name");
            if (name == null || name.isBlank()) continue;

            // Check if certification already exists
            boolean exists = certificationRepository.findByProfileIdOrderByIssueDateDesc(profile.getId()).stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(name));

            if (!exists) {
                ProfileCertification certification = ProfileCertification.builder()
                        .profile(profile)
                        .name(name)
                        .issuer((String) cert.get("issuer"))
                        .issueDate(parseDateFromInterview((String) cert.get("issue_date")))
                        .credentialUrl((String) cert.get("credential_url"))
                        .build();

                certificationRepository.save(certification);
                log.debug("Added certification: {}", name);
            }
        }
    }

    /**
     * Parse date from interview format (YYYY-MM) to LocalDate.
     */
    private LocalDate parseDateFromInterview(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        try {
            // Handle YYYY-MM format
            if (dateStr.matches("\\d{4}-\\d{2}")) {
                return LocalDate.parse(dateStr + "-01", DATE_FORMATTER);
            }
            // Handle full date format
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }
}
