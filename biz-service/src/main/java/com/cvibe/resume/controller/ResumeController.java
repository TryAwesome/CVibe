package com.cvibe.resume.controller;

import com.cvibe.common.dto.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import com.cvibe.resume.dto.ResumeDto;
import com.cvibe.resume.dto.ResumeUploadResponse;
import com.cvibe.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 简历控制器
 * 处理简历上传、查询、删除、AI解析等 API 请求
 */
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * 上传简历
     * 
     * POST /api/resumes
     * Content-Type: multipart/form-data
     * 
     * 上传后会自动调用 AI Engine 进行解析
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResumeUploadResponse>> uploadResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes) {
        ResumeUploadResponse response = resumeService.uploadResume(
                principal.getUserId(), file, notes);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取简历列表
     * 
     * GET /api/resumes
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResumeDto>>> getResumeList(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ResumeDto> resumes = resumeService.getResumeList(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(resumes));
    }

    /**
     * 获取单个简历
     * 
     * GET /api/resumes/{resumeId}
     */
    @GetMapping("/{resumeId}")
    public ResponseEntity<ApiResponse<ResumeDto>> getResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId) {
        ResumeDto resume = resumeService.getResume(principal.getUserId(), resumeId);
        return ResponseEntity.ok(ApiResponse.success(resume));
    }

    /**
     * 获取主简历
     * 
     * GET /api/resumes/primary
     */
    @GetMapping("/primary")
    public ResponseEntity<ApiResponse<ResumeDto>> getPrimaryResume(
            @AuthenticationPrincipal UserPrincipal principal) {
        ResumeDto resume = resumeService.getPrimaryResume(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(resume));
    }

    /**
     * 设置主简历
     * 
     * PUT /api/resumes/{resumeId}/primary
     */
    @PutMapping("/{resumeId}/primary")
    public ResponseEntity<ApiResponse<ResumeDto>> setPrimaryResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId) {
        ResumeDto resume = resumeService.setPrimaryResume(principal.getUserId(), resumeId);
        return ResponseEntity.ok(ApiResponse.success(resume, "已设置为主简历"));
    }

    /**
     * 重新解析简历
     * 
     * POST /api/resumes/{resumeId}/reparse
     * 
     * 重新调用 AI Engine 解析已上传的简历，用于：
     * - AI 服务恢复后重新解析失败的简历
     * - 使用新版本的 AI 模型重新解析
     */
    @PostMapping("/{resumeId}/reparse")
    public ResponseEntity<ApiResponse<ResumeDto>> reparseResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId) {
        ResumeDto resume = resumeService.reparseResume(principal.getUserId(), resumeId);
        return ResponseEntity.ok(ApiResponse.success(resume, "简历重新解析成功"));
    }

    /**
     * 将简历解析结果同步到用户 Profile
     * 
     * POST /api/resumes/{resumeId}/sync-to-profile
     * 
     * 请求体 (可选，默认全部同步):
     * {
     *   "syncBasicInfo": true,
     *   "syncExperiences": true,
     *   "syncEducations": true,
     *   "syncSkills": true,
     *   "syncProjects": true
     * }
     * 
     * 将简历中解析出的信息同步到用户个人资料，包括：
     * - 工作经历
     * - 教育经历
     * - 技能
     * - 项目经历
     */
    @PostMapping("/{resumeId}/sync-to-profile")
    public ResponseEntity<ApiResponse<ResumeService.SyncToProfileResult>> syncToProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId,
            @RequestBody(required = false) ResumeService.SyncOptions options) {
        
        // 默认同步所有内容
        if (options == null) {
            options = ResumeService.SyncOptions.builder()
                    .syncBasicInfo(true)
                    .syncExperiences(true)
                    .syncEducations(true)
                    .syncSkills(true)
                    .syncProjects(true)
                    .build();
        }
        
        ResumeService.SyncToProfileResult result = resumeService.syncToProfile(
                principal.getUserId(), resumeId, options);
        
        String message = String.format(
                "同步完成: %d 条工作经历, %d 条教育经历, %d 个技能, %d 个项目",
                result.getExperiencesSynced(),
                result.getEducationsSynced(),
                result.getSkillsSynced(),
                result.getProjectsSynced()
        );
        
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    /**
     * 删除简历
     * 
     * DELETE /api/resumes/{resumeId}
     */
    @DeleteMapping("/{resumeId}")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId) {
        resumeService.deleteResume(principal.getUserId(), resumeId);
        return ResponseEntity.ok(ApiResponse.success(null, "简历已删除"));
    }
}
