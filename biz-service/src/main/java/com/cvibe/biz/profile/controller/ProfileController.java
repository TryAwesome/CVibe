package com.cvibe.biz.profile.controller;

import com.cvibe.biz.profile.dto.*;
import com.cvibe.biz.profile.service.ProfileService;
import com.cvibe.common.response.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Profile API Controller
 */
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Get current user's profile
     */
    @GetMapping
    public ApiResponse<ProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        ProfileResponse profile = profileService.getOrCreateProfile(principal.getId());
        return ApiResponse.success(profile);
    }

    /**
     * Update current user's profile
     */
    @PutMapping
    public ApiResponse<ProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileRequest request) {
        ProfileResponse profile = profileService.updateProfile(principal.getId(), request);
        return ApiResponse.success(profile);
    }

    // ==================== Experience Management ====================

    /**
     * Add experience
     */
    @PostMapping("/experiences")
    public ApiResponse<ExperienceDto> addExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExperienceDto request) {
        ExperienceDto experience = profileService.addExperience(principal.getId(), request);
        return ApiResponse.success(experience);
    }

    /**
     * Update experience
     */
    @PutMapping("/experiences/{experienceId}")
    public ApiResponse<ExperienceDto> updateExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID experienceId,
            @Valid @RequestBody ExperienceDto request) {
        ExperienceDto experience = profileService.updateExperience(
                principal.getId(), experienceId, request);
        return ApiResponse.success(experience);
    }

    /**
     * Delete experience
     */
    @DeleteMapping("/experiences/{experienceId}")
    public ApiResponse<Void> deleteExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID experienceId) {
        profileService.deleteExperience(principal.getId(), experienceId);
        return ApiResponse.success();
    }

    // ==================== Skill Management ====================

    /**
     * Add skill
     */
    @PostMapping("/skills")
    public ApiResponse<SkillDto> addSkill(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SkillDto request) {
        SkillDto skill = profileService.addSkill(principal.getId(), request);
        return ApiResponse.success(skill);
    }

    /**
     * Delete skill
     */
    @DeleteMapping("/skills/{skillId}")
    public ApiResponse<Void> deleteSkill(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID skillId) {
        profileService.deleteSkill(principal.getId(), skillId);
        return ApiResponse.success();
    }
}
