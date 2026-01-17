package com.cvibe.profile.controller;

import com.cvibe.common.dto.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import com.cvibe.profile.dto.ExperienceDto;
import com.cvibe.profile.dto.ProfileDto;
import com.cvibe.profile.dto.ProfileRequest;
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
}
