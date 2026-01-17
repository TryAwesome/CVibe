package com.cvibe.resumebuilder.controller;

import com.cvibe.common.dto.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import com.cvibe.resumebuilder.dto.GenerateResumeRequest;
import com.cvibe.resumebuilder.dto.GeneratedResumeDto;
import com.cvibe.resumebuilder.dto.ResumeTemplateDto;
import com.cvibe.resumebuilder.service.ResumeBuilderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 简历生成器控制器
 */
@RestController
@RequestMapping("/api/resume-builder")
@RequiredArgsConstructor
public class ResumeBuilderController {

    private final ResumeBuilderService resumeBuilderService;

    // ==================== 模板端点 ====================

    /**
     * 获取所有模板
     * 
     * GET /api/resume-builder/templates?category=PROFESSIONAL
     */
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<ResumeTemplateDto>>> getTemplates(
            @RequestParam(required = false) String category) {
        List<ResumeTemplateDto> templates = resumeBuilderService.getTemplates(category);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    /**
     * 获取推荐模板
     * 
     * GET /api/resume-builder/templates/featured
     */
    @GetMapping("/templates/featured")
    public ResponseEntity<ApiResponse<List<ResumeTemplateDto>>> getFeaturedTemplates() {
        List<ResumeTemplateDto> templates = resumeBuilderService.getFeaturedTemplates();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    /**
     * 按分类获取模板
     * 
     * GET /api/resume-builder/templates/category/{category}
     */
    @GetMapping("/templates/category/{category}")
    public ResponseEntity<ApiResponse<List<ResumeTemplateDto>>> getTemplatesByCategory(
            @PathVariable String category) {
        List<ResumeTemplateDto> templates = resumeBuilderService.getTemplatesByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    /**
     * 获取模板 LaTeX 内容
     * 
     * GET /api/resume-builder/templates/{templateId}/content
     */
    @GetMapping("/templates/{templateId}/content")
    public ResponseEntity<ApiResponse<TemplateContentResponse>> getTemplateContent(
            @PathVariable UUID templateId) {
        String content = resumeBuilderService.getTemplateContent(templateId);
        return ResponseEntity.ok(ApiResponse.success(new TemplateContentResponse(content)));
    }

    /**
     * 预览模板
     * 
     * GET /api/resume-builder/preview/{templateId}
     */
    @GetMapping("/preview/{templateId}")
    public ResponseEntity<ApiResponse<String>> previewTemplate(
            @PathVariable UUID templateId) {
        String html = resumeBuilderService.previewTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success(html));
    }

    // ==================== 生成端点 ====================

    /**
     * AI 生成简历
     * 
     * POST /api/resume-builder/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GeneratedResumeDto>> generateResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerateResumeRequest request) {
        GeneratedResumeDto result = resumeBuilderService.generateResume(
                principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取生成历史
     * 
     * GET /api/resume-builder/generations
     */
    @GetMapping("/generations")
    public ResponseEntity<ApiResponse<List<GeneratedResumeDto>>> getGenerations(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<GeneratedResumeDto> generations = resumeBuilderService.getGenerations(
                principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(generations));
    }

    /**
     * 获取单个生成记录
     * 
     * GET /api/resume-builder/generations/{generationId}
     */
    @GetMapping("/generations/{generationId}")
    public ResponseEntity<ApiResponse<GeneratedResumeDto>> getGeneration(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID generationId) {
        GeneratedResumeDto generation = resumeBuilderService.getGeneration(
                principal.getUserId(), generationId);
        return ResponseEntity.ok(ApiResponse.success(generation));
    }

    /**
     * 更新 LaTeX 内容
     * 
     * PUT /api/resume-builder/generations/{generationId}/latex
     */
    @PutMapping("/generations/{generationId}/latex")
    public ResponseEntity<ApiResponse<GeneratedResumeDto>> updateLatexContent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID generationId,
            @Valid @RequestBody UpdateLatexRequest request) {
        GeneratedResumeDto generation = resumeBuilderService.updateLatexContent(
                principal.getUserId(), generationId, request.latexContent());
        return ResponseEntity.ok(ApiResponse.success(generation));
    }

    // ==================== 内部 DTO ====================

    /**
     * 模板内容响应
     */
    public record TemplateContentResponse(String latexTemplate) {}

    /**
     * 更新 LaTeX 请求
     */
    public record UpdateLatexRequest(String latexContent) {}
}
