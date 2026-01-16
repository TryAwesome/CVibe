package com.cvibe.biz.resume.controller;

import com.cvibe.biz.resume.dto.*;
import com.cvibe.biz.resume.entity.ResumeTemplate.TemplateCategory;
import com.cvibe.biz.resume.service.ResumeBuilderService;
import com.cvibe.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ResumeBuilderController
 * 
 * REST API endpoints for resume template management and resume generation.
 */
@RestController
@RequestMapping("/v1/resume-builder")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Resume Builder", description = "Resume template and generation APIs")
public class ResumeBuilderController {

    private final ResumeBuilderService builderService;

    // ================== Template Endpoints ==================

    @GetMapping("/templates")
    @Operation(summary = "Get available templates for user")
    public ResponseEntity<ApiResponse<Page<ResumeTemplateDto>>> getTemplates(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = extractUserId(userDetails);
        Page<ResumeTemplateDto> templates = builderService.getAvailableTemplates(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @GetMapping("/templates/featured")
    @Operation(summary = "Get featured templates")
    public ResponseEntity<ApiResponse<List<ResumeTemplateDto>>> getFeaturedTemplates() {
        List<ResumeTemplateDto> templates = builderService.getFeaturedTemplates();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @GetMapping("/templates/category/{category}")
    @Operation(summary = "Get templates by category")
    public ResponseEntity<ApiResponse<List<ResumeTemplateDto>>> getTemplatesByCategory(
            @PathVariable TemplateCategory category) {

        List<ResumeTemplateDto> templates = builderService.getTemplatesByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @GetMapping("/templates/my")
    @Operation(summary = "Get user's custom templates")
    public ResponseEntity<ApiResponse<List<ResumeTemplateDto>>> getUserTemplates(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        List<ResumeTemplateDto> templates = builderService.getUserTemplates(userId);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @GetMapping("/templates/{templateId}/content")
    @Operation(summary = "Get template LaTeX content")
    public ResponseEntity<ApiResponse<String>> getTemplateContent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID templateId) {

        UUID userId = extractUserId(userDetails);
        String content = builderService.getTemplateContent(templateId, userId);
        return ResponseEntity.ok(ApiResponse.success(content));
    }

    @PostMapping("/templates")
    @Operation(summary = "Create custom template")
    public ResponseEntity<ApiResponse<ResumeTemplateDto>> createTemplate(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TemplateRequest request) {

        UUID userId = extractUserId(userDetails);
        ResumeTemplateDto template = builderService.createUserTemplate(userId, request);
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    @PutMapping("/templates/{templateId}")
    @Operation(summary = "Update custom template")
    public ResponseEntity<ApiResponse<ResumeTemplateDto>> updateTemplate(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID templateId,
            @Valid @RequestBody TemplateRequest request) {

        UUID userId = extractUserId(userDetails);
        ResumeTemplateDto template = builderService.updateUserTemplate(userId, templateId, request);
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    @DeleteMapping("/templates/{templateId}")
    @Operation(summary = "Delete custom template")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID templateId) {

        UUID userId = extractUserId(userDetails);
        builderService.deleteUserTemplate(userId, templateId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ================== Generation Endpoints ==================

    @PostMapping("/generate")
    @Operation(summary = "Generate tailored resume")
    public ResponseEntity<ApiResponse<GeneratedResumeDto>> generateResume(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GenerateResumeRequest request) {

        UUID userId = extractUserId(userDetails);
        GeneratedResumeDto result = builderService.generateResume(userId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/generations")
    @Operation(summary = "Get generation history")
    public ResponseEntity<ApiResponse<Page<GeneratedResumeDto>>> getGenerationHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = extractUserId(userDetails);
        Page<GeneratedResumeDto> history = builderService.getGenerationHistory(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/generations/{generationId}")
    @Operation(summary = "Get generation details")
    public ResponseEntity<ApiResponse<GeneratedResumeDto>> getGeneration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID generationId,
            @RequestParam(defaultValue = "false") boolean includeLatex) {

        UUID userId = extractUserId(userDetails);
        GeneratedResumeDto result = builderService.getGeneration(userId, generationId, includeLatex);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/generations/{generationId}/latex")
    @Operation(summary = "Update generated LaTeX content")
    public ResponseEntity<ApiResponse<GeneratedResumeDto>> updateLatex(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID generationId,
            @RequestBody Map<String, String> body) {

        UUID userId = extractUserId(userDetails);
        String latex = body.get("latex");
        GeneratedResumeDto result = builderService.updateGeneratedLatex(userId, generationId, latex);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/generations/{generationId}/export")
    @Operation(summary = "Export resume as PDF")
    public ResponseEntity<ApiResponse<GeneratedResumeDto>> exportResume(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID generationId) {

        UUID userId = extractUserId(userDetails);
        GeneratedResumeDto result = builderService.exportResume(userId, generationId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/generations/{generationId}/rating")
    @Operation(summary = "Submit rating for generation")
    public ResponseEntity<ApiResponse<GeneratedResumeDto>> submitRating(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID generationId,
            @RequestBody Map<String, Object> body) {

        UUID userId = extractUserId(userDetails);
        Integer rating = (Integer) body.get("rating");
        String feedback = (String) body.get("feedback");
        GeneratedResumeDto result = builderService.submitRating(userId, generationId, rating, feedback);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ================== Dashboard Endpoints ==================

    @GetMapping("/summary")
    @Operation(summary = "Get resume builder summary for dashboard")
    public ResponseEntity<ApiResponse<ResumeBuilderSummary>> getSummary(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        ResumeBuilderSummary summary = builderService.getSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // ================== Admin Endpoints ==================

    @PostMapping("/admin/templates")
    @Operation(summary = "Create system template (admin only)")
    public ResponseEntity<ApiResponse<ResumeTemplateDto>> createSystemTemplate(
            @Valid @RequestBody TemplateRequest request) {

        ResumeTemplateDto template = builderService.createSystemTemplate(request);
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    @GetMapping("/admin/statistics")
    @Operation(summary = "Get resume builder statistics (admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        Map<String, Object> stats = builderService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ================== Helper Methods ==================

    private UUID extractUserId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}
