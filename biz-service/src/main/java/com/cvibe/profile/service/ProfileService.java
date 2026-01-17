package com.cvibe.profile.service;

import com.cvibe.auth.entity.User;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import com.cvibe.profile.dto.ExperienceDto;
import com.cvibe.profile.dto.ProfileDto;
import com.cvibe.profile.dto.ProfileRequest;
import com.cvibe.profile.dto.SkillDto;
import com.cvibe.profile.entity.EmploymentType;
import com.cvibe.profile.entity.ProfileExperience;
import com.cvibe.profile.entity.ProfileSkill;
import com.cvibe.profile.entity.UserProfile;
import com.cvibe.profile.repository.ProfileExperienceRepository;
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
    private final ProfileSkillRepository skillRepository;
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
        return ProfileDto.builder()
                .id(profile.getId().toString())
                .userId(profile.getUser().getId().toString())
                .headline(profile.getHeadline())
                .summary(profile.getSummary())
                .location(profile.getLocation())
                .experiences(profile.getExperiences().stream()
                        .map(this::toExperienceDto)
                        .collect(Collectors.toList()))
                .skills(profile.getSkills().stream()
                        .map(this::toSkillDto)
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
}
