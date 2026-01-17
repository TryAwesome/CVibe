package com.cvibe.settings.controller;

import com.cvibe.common.dto.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import com.cvibe.settings.dto.AiConfigDto;
import com.cvibe.settings.dto.ChangePasswordRequest;
import com.cvibe.settings.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 设置控制器
 * 处理密码修改和 AI 配置相关的 API 请求
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * 修改密码
     * 
     * PUT /api/settings/password
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        settingsService.changePassword(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "密码修改成功"));
    }

    /**
     * 获取 AI 配置
     * 如果用户没有配置，返回默认配置
     * 
     * GET /api/settings/ai-config
     */
    @GetMapping("/ai-config")
    public ResponseEntity<ApiResponse<AiConfigDto>> getAiConfig(
            @AuthenticationPrincipal UserPrincipal principal) {
        AiConfigDto config = settingsService.getAiConfig(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    /**
     * 更新 AI 配置
     * 
     * PUT /api/settings/ai-config
     */
    @PutMapping("/ai-config")
    public ResponseEntity<ApiResponse<AiConfigDto>> updateAiConfig(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AiConfigDto request) {
        AiConfigDto config = settingsService.updateAiConfig(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    /**
     * 删除 AI 配置（重置为默认）
     * 
     * DELETE /api/settings/ai-config
     */
    @DeleteMapping("/ai-config")
    public ResponseEntity<ApiResponse<Void>> deleteAiConfig(
            @AuthenticationPrincipal UserPrincipal principal) {
        settingsService.deleteAiConfig(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "AI 配置已重置"));
    }
}
