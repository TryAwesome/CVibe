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
 * 处理简历上传、查询、删除等 API 请求
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
