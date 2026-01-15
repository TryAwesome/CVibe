package com.cvibe.biz.settings.controller;

import com.cvibe.biz.settings.dto.AiConfigDto;
import com.cvibe.biz.settings.dto.ChangePasswordRequest;
import com.cvibe.biz.settings.dto.UpdateProfileRequest;
import com.cvibe.biz.settings.service.SettingsService;
import com.cvibe.biz.user.dto.UserResponse;
import com.cvibe.common.response.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Settings Controller
 * Handles user settings, password management, and AI configuration
 */
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * Change password
     * PUT /api/settings/password
     */
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        settingsService.changePassword(principal.getId(), request);
        return ApiResponse.success();
    }

    /**
     * Set password (for Google-only users)
     * POST /api/settings/password
     */
    @PostMapping("/password")
    public ApiResponse<Void> setPassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody SetPasswordRequest request) {
        
        settingsService.setPassword(principal.getId(), request.getPassword());
        return ApiResponse.success();
    }

    /**
     * Update profile
     * PUT /api/settings/profile
     */
    @PutMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        UserResponse user = settingsService.updateProfile(principal.getId(), request);
        return ApiResponse.success(user);
    }

    /**
     * Get AI configuration
     * GET /api/settings/ai-config
     */
    @GetMapping("/ai-config")
    public ApiResponse<AiConfigDto> getAiConfig(@AuthenticationPrincipal UserPrincipal principal) {
        AiConfigDto config = settingsService.getAiConfig(principal.getId());
        return ApiResponse.success(config);
    }

    /**
     * Update AI configuration
     * PUT /api/settings/ai-config
     */
    @PutMapping("/ai-config")
    public ApiResponse<AiConfigDto> updateAiConfig(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AiConfigDto request) {
        
        AiConfigDto config = settingsService.updateAiConfig(principal.getId(), request);
        return ApiResponse.success(config);
    }

    /**
     * Delete AI configuration
     * DELETE /api/settings/ai-config
     */
    @DeleteMapping("/ai-config")
    public ApiResponse<Void> deleteAiConfig(@AuthenticationPrincipal UserPrincipal principal) {
        settingsService.deleteAiConfig(principal.getId());
        return ApiResponse.success();
    }

    /**
     * Set Password Request DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SetPasswordRequest {
        private String password;
    }
}
