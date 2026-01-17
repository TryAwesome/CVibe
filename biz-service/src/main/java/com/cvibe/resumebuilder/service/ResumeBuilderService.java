package com.cvibe.resumebuilder.service;

import com.cvibe.auth.entity.User;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import com.cvibe.profile.dto.ProfileDto;
import com.cvibe.profile.service.ProfileService;
import com.cvibe.resume.dto.ResumeDto;
import com.cvibe.resume.service.ResumeService;
import com.cvibe.resumebuilder.dto.GenerateResumeRequest;
import com.cvibe.resumebuilder.dto.GeneratedResumeDto;
import com.cvibe.resumebuilder.dto.ResumeTemplateDto;
import com.cvibe.resumebuilder.entity.GenerationStatus;
import com.cvibe.resumebuilder.entity.ResumeGeneration;
import com.cvibe.resumebuilder.entity.ResumeTemplate;
import com.cvibe.resumebuilder.entity.TemplateCategory;
import com.cvibe.resumebuilder.repository.ResumeGenerationRepository;
import com.cvibe.resumebuilder.repository.ResumeTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 简历生成器服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeBuilderService {

    private final ResumeTemplateRepository templateRepository;
    private final ResumeGenerationRepository generationRepository;
    private final ProfileService profileService;
    private final ResumeService resumeService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ==================== 模板相关方法 ====================

    /**
     * 获取所有模板（可选分类过滤）
     */
    public List<ResumeTemplateDto> getTemplates(String category) {
        if (category != null && !category.isBlank()) {
            try {
                TemplateCategory cat = TemplateCategory.valueOf(category.toUpperCase());
                return templateRepository.findByCategoryAndIsActiveTrueOrderByNameAsc(cat)
                        .stream()
                        .map(ResumeTemplateDto::fromEntity)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // 无效分类，返回所有
            }
        }
        return templateRepository.findAllByIsActiveTrueOrderByIsPremiumAscNameAsc()
                .stream()
                .map(ResumeTemplateDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取推荐模板
     */
    public List<ResumeTemplateDto> getFeaturedTemplates() {
        return templateRepository.findByIsFeaturedTrueAndIsActiveTrueOrderByNameAsc()
                .stream()
                .map(ResumeTemplateDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 按分类获取模板
     */
    public List<ResumeTemplateDto> getTemplatesByCategory(String category) {
        try {
            TemplateCategory cat = TemplateCategory.valueOf(category.toUpperCase());
            return templateRepository.findByCategoryAndIsActiveTrueOrderByNameAsc(cat)
                    .stream()
                    .map(ResumeTemplateDto::fromEntity)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    /**
     * 获取模板 LaTeX 内容
     */
    public String getTemplateContent(UUID templateId) {
        ResumeTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));
        return template.getLatexTemplate();
    }

    /**
     * 预览模板（返回示例 HTML）
     */
    public String previewTemplate(UUID templateId) {
        ResumeTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));
        return template.getSampleHtml();
    }

    // ==================== 生成相关方法 ====================

    /**
     * AI 生成简历
     */
    @Transactional
    public GeneratedResumeDto generateResume(UUID userId, GenerateResumeRequest request) {
        // 1. 验证模板存在
        UUID templateId = UUID.fromString(request.getTemplateId());
        ResumeTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));

        // 2. 获取用户数据
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ProfileDto profile = profileService.getOrCreateProfile(userId);
        ResumeDto primaryResume = resumeService.getPrimaryResume(userId);

        // 3. 验证用户有工作经历
        if (profile.getExperiences() == null || profile.getExperiences().isEmpty()) {
            throw new BusinessException(ErrorCode.PROFILE_EMPTY, "请先添加工作经历");
        }

        // 4. 创建生成记录
        ResumeGeneration generation = ResumeGeneration.builder()
                .user(user)
                .template(template)
                .targetPosition(request.getTargetPosition())
                .targetCompany(request.getTargetCompany())
                .customInstructions(request.getCustomInstructions())
                .customizations(toJson(request.getCustomizations()))
                .status(GenerationStatus.PROCESSING)
                .build();

        generation = generationRepository.save(generation);

        // 5. 调用 AI 生成（TODO: 集成 gRPC）
        try {
            // Mock 生成内容
            GeneratedResumeDto.ResumeContent content = createMockContent(profile, primaryResume);
            
            generation.setContentJson(toJson(content));
            generation.setStatus(GenerationStatus.COMPLETED);
            generation.setHtmlPreview(generateMockHtmlPreview(content, template));
            generation = generationRepository.save(generation);

            log.info("用户 {} 生成简历成功: {}", userId, generation.getId());
            return toGeneratedResumeDto(generation);

        } catch (Exception e) {
            log.error("简历生成失败", e);
            generation.setStatus(GenerationStatus.FAILED);
            generation.setErrorMessage("生成失败: " + e.getMessage());
            generationRepository.save(generation);
            throw new BusinessException(ErrorCode.RESUME_GENERATION_FAILED, e.getMessage());
        }
    }

    /**
     * 获取用户的生成历史
     */
    @Transactional(readOnly = true)
    public List<GeneratedResumeDto> getGenerations(UUID userId) {
        return generationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toGeneratedResumeDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取单个生成记录
     */
    @Transactional(readOnly = true)
    public GeneratedResumeDto getGeneration(UUID userId, UUID generationId) {
        ResumeGeneration generation = generationRepository.findByIdAndUserId(generationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
        return toGeneratedResumeDto(generation);
    }

    /**
     * 更新 LaTeX 内容
     */
    @Transactional
    public GeneratedResumeDto updateLatexContent(UUID userId, UUID generationId, String latexContent) {
        ResumeGeneration generation = generationRepository.findByIdAndUserId(generationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        generation.setLatexContent(latexContent);
        generation = generationRepository.save(generation);

        log.info("用户 {} 更新 LaTeX 内容: {}", userId, generationId);
        return toGeneratedResumeDto(generation);
    }

    // ==================== 私有辅助方法 ====================

    private GeneratedResumeDto toGeneratedResumeDto(ResumeGeneration generation) {
        return GeneratedResumeDto.builder()
                .id(generation.getId().toString())
                .templateId(generation.getTemplate().getId().toString())
                .templateName(generation.getTemplate().getName())
                .targetPosition(generation.getTargetPosition())
                .targetCompany(generation.getTargetCompany())
                .status(generation.getStatus().name())
                .content(parseContent(generation.getContentJson()))
                .latexContent(generation.getLatexContent())
                .htmlPreview(generation.getHtmlPreview())
                .pdfUrl(generation.getPdfPath())
                .errorMessage(generation.getErrorMessage())
                .createdAt(generation.getCreatedAt() != null ? generation.getCreatedAt().toString() : null)
                .updatedAt(generation.getUpdatedAt() != null ? generation.getUpdatedAt().toString() : null)
                .build();
    }

    private GeneratedResumeDto.ResumeContent parseContent(String json) {
        if (json == null || json.isBlank()) {
            return GeneratedResumeDto.ResumeContent.empty();
        }
        try {
            return objectMapper.readValue(json, GeneratedResumeDto.ResumeContent.class);
        } catch (JsonProcessingException e) {
            log.error("解析简历内容失败", e);
            return GeneratedResumeDto.ResumeContent.empty();
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("序列化 JSON 失败", e);
            return null;
        }
    }

    /**
     * 创建模拟内容（TODO: 替换为 AI 生成）
     */
    private GeneratedResumeDto.ResumeContent createMockContent(ProfileDto profile, ResumeDto resume) {
        return GeneratedResumeDto.ResumeContent.builder()
                .personalInfo(GeneratedResumeDto.PersonalInfo.builder()
                        .name("待生成")
                        .email("")
                        .phone("")
                        .location(profile.getLocation())
                        .build())
                .summary(profile.getSummary() != null ? profile.getSummary() : "专业人士，等待 AI 优化...")
                .experiences(List.of())
                .education(List.of())
                .skills(GeneratedResumeDto.SkillsGroup.builder()
                        .technical(List.of())
                        .soft(List.of())
                        .build())
                .build();
    }

    /**
     * 生成模拟 HTML 预览
     */
    private String generateMockHtmlPreview(GeneratedResumeDto.ResumeContent content, ResumeTemplate template) {
        return String.format("""
            <html>
            <head><title>%s - 简历预览</title></head>
            <body style="font-family: sans-serif; padding: 20px;">
                <h1>%s</h1>
                <p>%s</p>
                <hr>
                <p><em>使用模板: %s</em></p>
                <p><em>AI 服务就绪后将生成完整简历</em></p>
            </body>
            </html>
            """,
                content.getPersonalInfo().getName(),
                content.getPersonalInfo().getName(),
                content.getSummary(),
                template.getName()
        );
    }
}
