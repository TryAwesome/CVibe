package com.cvibe.biz.resume.controller;

import com.cvibe.biz.resume.dto.ResumeHistoryDto;
import com.cvibe.biz.resume.service.ResumeService;
import com.cvibe.common.response.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Resume Management API Controller
 */
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * Upload a resume file
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResumeHistoryDto> uploadResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes) {
        ResumeHistoryDto resume = resumeService.uploadResume(principal.getId(), file, notes);
        return ApiResponse.success(resume);
    }

    /**
     * Get all resume history
     */
    @GetMapping
    public ApiResponse<List<ResumeHistoryDto>> getResumeHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ResumeHistoryDto> resumes = resumeService.getAllResumes(principal.getId());
        return ApiResponse.success(resumes);
    }

    /**
     * Get resume history with pagination
     */
    @GetMapping("/paged")
    public ApiResponse<Page<ResumeHistoryDto>> getResumeHistoryPaged(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        Page<ResumeHistoryDto> resumes = resumeService.getResumeHistory(
                principal.getId(), pageable);
        return ApiResponse.success(resumes);
    }

    /**
     * Get single resume details
     */
    @GetMapping("/{resumeId}")
    public ApiResponse<ResumeHistoryDto> getResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId) {
        ResumeHistoryDto resume = resumeService.getResume(principal.getId(), resumeId);
        return ApiResponse.success(resume);
    }

    /**
     * Get primary resume
     */
    @GetMapping("/primary")
    public ApiResponse<ResumeHistoryDto> getPrimaryResume(
            @AuthenticationPrincipal UserPrincipal principal) {
        ResumeHistoryDto resume = resumeService.getPrimaryResume(principal.getId());
        return ApiResponse.success(resume);
    }

    /**
     * Download resume file
     */
    @GetMapping("/{resumeId}/download")
    public ResponseEntity<InputStreamResource> downloadResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId) {
        
        ResumeHistoryDto resumeInfo = resumeService.getResume(principal.getId(), resumeId);
        InputStream inputStream = resumeService.downloadResume(principal.getId(), resumeId);

        String filename = resumeInfo.getOriginalName() != null 
                ? resumeInfo.getOriginalName() 
                : resumeInfo.getFileName();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        resumeInfo.getContentType() != null 
                                ? resumeInfo.getContentType() 
                                : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(new InputStreamResource(inputStream));
    }

    /**
     * Set resume as primary
     */
    @PutMapping("/{resumeId}/primary")
    public ApiResponse<ResumeHistoryDto> setPrimary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId) {
        ResumeHistoryDto resume = resumeService.setPrimary(principal.getId(), resumeId);
        return ApiResponse.success(resume);
    }

    /**
     * Delete resume
     */
    @DeleteMapping("/{resumeId}")
    public ApiResponse<Void> deleteResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId) {
        resumeService.deleteResume(principal.getId(), resumeId);
        return ApiResponse.success();
    }
}
