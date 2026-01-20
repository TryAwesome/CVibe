package com.cvibe.profile.controller;

import com.cvibe.common.dto.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import com.cvibe.profile.dto.CertificationDto;
import com.cvibe.profile.dto.EducationDto;
import com.cvibe.profile.dto.ExperienceDto;
import com.cvibe.profile.dto.LanguageDto;
import com.cvibe.profile.dto.ProfileDto;
import com.cvibe.profile.dto.ProfileRequest;
import com.cvibe.profile.dto.ProjectDto;
import com.cvibe.profile.dto.SkillDto;
import com.cvibe.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for profile management.
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Get the current user's profile.
     * Auto-creates an empty profile if it doesn't exist.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileDto>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        ProfileDto profile = profileService.getOrCreateProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * Update the current user's basic profile information.
     */
    @PutMapping
    public ResponseEntity<ApiResponse<ProfileDto>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileRequest request) {
        ProfileDto profile = profileService.updateProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // ==================== Experience Endpoints ====================

    /**
     * Get all work experiences for the current user.
     */
    @GetMapping("/experiences")
    public ResponseEntity<ApiResponse<List<ExperienceDto>>> getExperiences(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ExperienceDto> experiences = profileService.getExperiences(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(experiences));
    }

    /**
     * Add a new work experience.
     */
    @PostMapping("/experiences")
    public ResponseEntity<ApiResponse<ExperienceDto>> addExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExperienceDto request) {
        ExperienceDto experience = profileService.addExperience(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(experience));
    }

    /**
     * Update an existing work experience.
     */
    @PutMapping("/experiences/{experienceId}")
    public ResponseEntity<ApiResponse<ExperienceDto>> updateExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID experienceId,
            @Valid @RequestBody ExperienceDto request) {
        ExperienceDto experience = profileService.updateExperience(
                principal.getUserId(), experienceId, request);
        return ResponseEntity.ok(ApiResponse.success(experience));
    }

    /**
     * Delete a work experience.
     */
    @DeleteMapping("/experiences/{experienceId}")
    public ResponseEntity<ApiResponse<Void>> deleteExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID experienceId) {
        profileService.deleteExperience(principal.getUserId(), experienceId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== Skill Endpoints ====================

    /**
     * Get all skills for the current user.
     */
    @GetMapping("/skills")
    public ResponseEntity<ApiResponse<List<SkillDto>>> getSkills(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<SkillDto> skills = profileService.getSkills(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(skills));
    }

    /**
     * Add a new skill.
     */
    @PostMapping("/skills")
    public ResponseEntity<ApiResponse<SkillDto>> addSkill(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SkillDto request) {
        SkillDto skill = profileService.addSkill(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(skill));
    }

    /**
     * Delete a skill.
     */
    @DeleteMapping("/skills/{skillId}")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID skillId) {
        profileService.deleteSkill(principal.getUserId(), skillId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== Education Endpoints ====================

    /**
     * Get all educations for the current user.
     */
    @GetMapping("/educations")
    public ResponseEntity<ApiResponse<List<EducationDto>>> getEducations(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<EducationDto> educations = profileService.getEducations(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(educations));
    }

    /**
     * Add a new education.
     */
    @PostMapping("/educations")
    public ResponseEntity<ApiResponse<EducationDto>> addEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EducationDto request) {
        EducationDto education = profileService.addEducation(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(education));
    }

    /**
     * Update an existing education.
     */
    @PutMapping("/educations/{educationId}")
    public ResponseEntity<ApiResponse<EducationDto>> updateEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID educationId,
            @Valid @RequestBody EducationDto request) {
        EducationDto education = profileService.updateEducation(
                principal.getUserId(), educationId, request);
        return ResponseEntity.ok(ApiResponse.success(education));
    }

    /**
     * Delete an education.
     */
    @DeleteMapping("/educations/{educationId}")
    public ResponseEntity<ApiResponse<Void>> deleteEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID educationId) {
        profileService.deleteEducation(principal.getUserId(), educationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== Project Endpoints ====================

    /**
     * Get all projects for the current user.
     */
    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getProjects(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ProjectDto> projects = profileService.getProjects(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    /**
     * Add a new project.
     */
    @PostMapping("/projects")
    public ResponseEntity<ApiResponse<ProjectDto>> addProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProjectDto request) {
        ProjectDto project = profileService.addProject(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    /**
     * Update an existing project.
     */
    @PutMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDto>> updateProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectDto request) {
        ProjectDto project = profileService.updateProject(
                principal.getUserId(), projectId, request);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    /**
     * Delete a project.
     */
    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID projectId) {
        profileService.deleteProject(principal.getUserId(), projectId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== Language Endpoints ====================

    /**
     * Get all languages for the current user.
     */
    @GetMapping("/languages")
    public ResponseEntity<ApiResponse<List<LanguageDto>>> getLanguages(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<LanguageDto> languages = profileService.getLanguages(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(languages));
    }

    /**
     * Add a new language.
     */
    @PostMapping("/languages")
    public ResponseEntity<ApiResponse<LanguageDto>> addLanguage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LanguageDto request) {
        LanguageDto language = profileService.addLanguage(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(language));
    }

    /**
     * Update an existing language.
     */
    @PutMapping("/languages/{languageId}")
    public ResponseEntity<ApiResponse<LanguageDto>> updateLanguage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID languageId,
            @Valid @RequestBody LanguageDto request) {
        LanguageDto language = profileService.updateLanguage(
                principal.getUserId(), languageId, request);
        return ResponseEntity.ok(ApiResponse.success(language));
    }

    /**
     * Delete a language.
     */
    @DeleteMapping("/languages/{languageId}")
    public ResponseEntity<ApiResponse<Void>> deleteLanguage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID languageId) {
        profileService.deleteLanguage(principal.getUserId(), languageId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== Certification Endpoints ====================

    /**
     * Get all certifications for the current user.
     */
    @GetMapping("/certifications")
    public ResponseEntity<ApiResponse<List<CertificationDto>>> getCertifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<CertificationDto> certifications = profileService.getCertifications(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(certifications));
    }

    /**
     * Add a new certification.
     */
    @PostMapping("/certifications")
    public ResponseEntity<ApiResponse<CertificationDto>> addCertification(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CertificationDto request) {
        CertificationDto certification = profileService.addCertification(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(certification));
    }

    /**
     * Update an existing certification.
     */
    @PutMapping("/certifications/{certificationId}")
    public ResponseEntity<ApiResponse<CertificationDto>> updateCertification(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID certificationId,
            @Valid @RequestBody CertificationDto request) {
        CertificationDto certification = profileService.updateCertification(
                principal.getUserId(), certificationId, request);
        return ResponseEntity.ok(ApiResponse.success(certification));
    }

    /**
     * Delete a certification.
     */
    @DeleteMapping("/certifications/{certificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteCertification(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID certificationId) {
        profileService.deleteCertification(principal.getUserId(), certificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
