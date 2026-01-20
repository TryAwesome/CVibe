package com.cvibe.resume.service;

import com.cvibe.auth.entity.User;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import com.cvibe.common.grpc.AIEngineClient;
import com.cvibe.profile.dto.CertificationDto;
import com.cvibe.profile.dto.EducationDto;
import com.cvibe.profile.dto.ExperienceDto;
import com.cvibe.profile.dto.LanguageDto;
import com.cvibe.profile.dto.ProjectDto;
import com.cvibe.profile.dto.SkillDto;
import com.cvibe.profile.service.ProfileService;
import com.cvibe.resume.dto.ParsedContent;
import com.cvibe.resume.dto.ResumeDto;
import com.cvibe.resume.dto.ResumeUploadResponse;
import com.cvibe.resume.entity.ResumeHistory;
import com.cvibe.resume.entity.ResumeStatus;
import com.cvibe.resume.repository.ResumeHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 简历服务
 * 处理简历上传、解析、管理等业务逻辑
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeHistoryRepository resumeRepository;
    private final ResumeStorageService storageService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AIEngineClient aiEngineClient;
    private final ProfileService profileService;

    // 允许的文件类型
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp"
    );

    // 允许的文件扩展名
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "png", "jpg", "jpeg", "webp"
    );

    // 最大文件大小（10MB）
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // 预签名 URL 有效期（分钟）
    private static final int PRESIGNED_URL_EXPIRE_MINUTES = 60;

    /**
     * 上传并解析简历
     */
    @Transactional
    public ResumeUploadResponse uploadResume(UUID userId, MultipartFile file, String notes) {
        // 1. 验证文件
        validateFile(file);

        // 2. 获取用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 3. 上传文件到 MinIO
        String filePath = storageService.uploadFile(file, userId);

        // 4. 判断是否为第一份简历（自动设为主简历）
        boolean isFirst = resumeRepository.countByUserId(userId) == 0;

        // 5. 创建简历记录
        ResumeHistory resume = ResumeHistory.builder()
                .user(user)
                .fileName(extractFileName(filePath))
                .originalName(file.getOriginalFilename())
                .filePath(filePath)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .status(ResumeStatus.PROCESSING)
                .isPrimary(isFirst)
                .notes(notes)
                .build();

        resume = resumeRepository.save(resume);

        // 6. 调用 AI Engine 解析简历
        try {
            byte[] fileBytes = file.getBytes();
            AIEngineClient.ResumeParseResult parseResult = aiEngineClient.parseResume(
                    fileBytes,
                    file.getOriginalFilename(),
                    file.getContentType()
            );

            if (!parseResult.isSuccess()) {
                throw new RuntimeException(parseResult.getErrorMessage());
            }

            // 转换为 ParsedContent
            ParsedContent parsedContent = convertToParseContent(parseResult);
            
            resume.setParsedData(toJson(parsedContent));
            resume.setSkills(toJson(parsedContent.getSkillNames()));
            resume.setStatus(ResumeStatus.COMPLETED);
            resume = resumeRepository.save(resume);

            log.info("简历解析成功: userId={}, resumeId={}, name={}", 
                    userId, resume.getId(), parseResult.getName());

            return ResumeUploadResponse.success(toResumeDto(resume));

        } catch (Exception e) {
            log.error("简历解析失败", e);
            
            resume.setStatus(ResumeStatus.FAILED);
            resume.setErrorMessage("解析失败: " + e.getMessage());
            resumeRepository.save(resume);

            // 文件上传成功，但解析失败
            return ResumeUploadResponse.failed(toResumeDto(resume), 
                    "文件上传成功，但解析失败: " + e.getMessage());
        }
    }

    /**
     * 重新解析简历
     */
    @Transactional
    public ResumeDto reparseResume(UUID userId, UUID resumeId) {
        ResumeHistory resume = getResumeWithOwnershipCheck(userId, resumeId);

        try {
            // 从 MinIO 获取文件内容
            byte[] fileBytes = storageService.getFileContent(resume.getFilePath());
            
            resume.setStatus(ResumeStatus.PROCESSING);
            resumeRepository.save(resume);

            // 调用 AI Engine 解析
            AIEngineClient.ResumeParseResult parseResult = aiEngineClient.parseResume(
                    fileBytes,
                    resume.getOriginalName(),
                    resume.getContentType()
            );

            if (!parseResult.isSuccess()) {
                throw new RuntimeException(parseResult.getErrorMessage());
            }

            ParsedContent parsedContent = convertToParseContent(parseResult);
            
            resume.setParsedData(toJson(parsedContent));
            resume.setSkills(toJson(parsedContent.getSkillNames()));
            resume.setStatus(ResumeStatus.COMPLETED);
            resume.setErrorMessage(null);
            resume = resumeRepository.save(resume);

            log.info("简历重新解析成功: userId={}, resumeId={}", userId, resumeId);
            return toResumeDto(resume);

        } catch (Exception e) {
            log.error("简历重新解析失败", e);
            resume.setStatus(ResumeStatus.FAILED);
            resume.setErrorMessage("重新解析失败: " + e.getMessage());
            resumeRepository.save(resume);
            throw new BusinessException(ErrorCode.RESUME_PARSE_FAILED, e.getMessage());
        }
    }

    /**
     * 将简历解析结果同步到用户 Profile
     * 
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param options 同步选项 (syncExperiences, syncEducations, syncSkills, syncProjects)
     * @return 同步结果摘要
     */
    @Transactional
    public SyncToProfileResult syncToProfile(UUID userId, UUID resumeId, SyncOptions options) {
        ResumeHistory resume = getResumeWithOwnershipCheck(userId, resumeId);

        if (resume.getStatus() != ResumeStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.RESUME_NOT_PARSED, "简历尚未解析完成");
        }

        ParsedContent parsedContent = parseParsedContent(resume.getParsedData());
        if (parsedContent == null) {
            throw new BusinessException(ErrorCode.RESUME_NOT_PARSED, "简历解析数据为空");
        }

        SyncToProfileResult result = new SyncToProfileResult();

        try {
            // 同步基本信息
            if (options.isSyncBasicInfo() && parsedContent.getPersonalInfo() != null) {
                // 更新 Profile 基本信息
                var profile = profileService.getOrCreateProfile(userId);
                // 基本信息会通过其他接口更新
                result.setBasicInfoSynced(true);
            }

            // 同步工作经历
            if (options.isSyncExperiences() && parsedContent.getExperiences() != null) {
                int count = 0;
                for (ParsedContent.WorkExperience exp : parsedContent.getExperiences()) {
                    try {
                        ExperienceDto dto = ExperienceDto.builder()
                                .company(exp.getCompany())
                                .title(exp.getTitle())
                                .location(exp.getLocation())
                                .employmentType(exp.getEmploymentType())
                                .startDate(normalizeDate(exp.getStartDate()))
                                .endDate(normalizeDate(exp.getEndDate()))
                                .isCurrent(exp.getIsCurrent())
                                .description(exp.getDescription())
                                .achievements(exp.getAchievements())
                                .technologies(exp.getTechnologies())
                                .build();
                        profileService.addExperience(userId, dto);
                        count++;
                    } catch (Exception e) {
                        log.warn("同步工作经历失败: {}", e.getMessage());
                    }
                }
                result.setExperiencesSynced(count);
            }

            // 同步教育经历
            if (options.isSyncEducations() && parsedContent.getEducation() != null) {
                int count = 0;
                for (ParsedContent.Education edu : parsedContent.getEducation()) {
                    try {
                        EducationDto dto = EducationDto.builder()
                                .school(edu.getSchool())
                                .degree(edu.getDegree())
                                .fieldOfStudy(edu.getField())
                                .location(edu.getLocation())
                                .startDate(normalizeDate(edu.getStartDate()))
                                .endDate(normalizeDate(edu.getEndDate() != null ? edu.getEndDate() : edu.getGraduationDate()))
                                .gpa(edu.getGpa())
                                .description(edu.getDescription())
                                .activities(edu.getActivities())
                                .build();
                        profileService.addEducation(userId, dto);
                        count++;
                    } catch (Exception e) {
                        log.warn("同步教育经历失败: {}", e.getMessage());
                    }
                }
                result.setEducationsSynced(count);
            }

            // 同步技能
            if (options.isSyncSkills() && parsedContent.getSkills() != null) {
                int count = 0;
                for (ParsedContent.Skill skill : parsedContent.getSkills()) {
                    try {
                        SkillDto dto = SkillDto.builder()
                                .name(skill.getName())
                                .level(skill.getLevel())
                                .build();
                        profileService.addSkill(userId, dto);
                        count++;
                    } catch (BusinessException e) {
                        // 技能已存在，忽略
                        if (e.getErrorCode() != ErrorCode.SKILL_ALREADY_EXISTS) {
                            log.warn("同步技能失败: {}", e.getMessage());
                        }
                    } catch (Exception e) {
                        log.warn("同步技能失败: {}", e.getMessage());
                    }
                }
                result.setSkillsSynced(count);
            }

            // 同步项目经历
            if (options.isSyncProjects() && parsedContent.getProjects() != null) {
                int count = 0;
                for (ParsedContent.Project proj : parsedContent.getProjects()) {
                    try {
                        ProjectDto dto = ProjectDto.builder()
                                .name(proj.getName())
                                .description(proj.getDescription())
                                .url(proj.getUrl())
                                .repoUrl(proj.getRepoUrl())
                                .technologies(proj.getTechnologies())
                                .startDate(normalizeDate(proj.getStartDate()))
                                .endDate(normalizeDate(proj.getEndDate()))
                                .highlights(proj.getHighlights())
                                .build();
                        profileService.addProject(userId, dto);
                        count++;
                    } catch (Exception e) {
                        log.warn("同步项目失败: {}", e.getMessage());
                    }
                }
                result.setProjectsSynced(count);
            }

            // 同步语言能力
            if (options.isSyncLanguages() && parsedContent.getLanguages() != null) {
                int count = 0;
                for (ParsedContent.Language lang : parsedContent.getLanguages()) {
                    try {
                        LanguageDto dto = LanguageDto.builder()
                                .language(lang.getLanguage())
                                .proficiency(lang.getProficiency())
                                .build();
                        profileService.addLanguage(userId, dto);
                        count++;
                    } catch (BusinessException e) {
                        // 语言已存在，忽略
                        if (e.getErrorCode() != ErrorCode.LANGUAGE_ALREADY_EXISTS) {
                            log.warn("同步语言失败: {}", e.getMessage());
                        }
                    } catch (Exception e) {
                        log.warn("同步语言失败: {}", e.getMessage());
                    }
                }
                result.setLanguagesSynced(count);
            }

            // 同步证书
            if (options.isSyncCertifications() && parsedContent.getCertifications() != null) {
                int count = 0;
                for (ParsedContent.Certification cert : parsedContent.getCertifications()) {
                    try {
                        CertificationDto dto = CertificationDto.builder()
                                .name(cert.getName())
                                .issuer(cert.getIssuer())
                                .issueDate(parseLocalDate(normalizeDate(cert.getDate())))
                                .credentialUrl(cert.getUrl())
                                .build();
                        profileService.addCertification(userId, dto);
                        count++;
                    } catch (Exception e) {
                        log.warn("同步证书失败: {}", e.getMessage());
                    }
                }
                result.setCertificationsSynced(count);
            }

            result.setSuccess(true);
            log.info("简历同步到 Profile 成功: userId={}, result={}", userId, result);

        } catch (Exception e) {
            log.error("简历同步到 Profile 失败", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * 获取用户所有简历
     */
    @Transactional(readOnly = true)
    public List<ResumeDto> getResumeList(UUID userId) {
        return resumeRepository.findAllByUserIdOrdered(userId).stream()
                .map(this::toResumeDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取单个简历
     */
    @Transactional(readOnly = true)
    public ResumeDto getResume(UUID userId, UUID resumeId) {
        ResumeHistory resume = getResumeWithOwnershipCheck(userId, resumeId);
        return toResumeDto(resume);
    }

    /**
     * 获取主简历
     */
    @Transactional(readOnly = true)
    public ResumeDto getPrimaryResume(UUID userId) {
        return resumeRepository.findFirstByUserIdAndIsPrimaryTrue(userId)
                .map(this::toResumeDto)
                .orElse(null);
    }

    /**
     * 设置主简历
     */
    @Transactional
    public ResumeDto setPrimaryResume(UUID userId, UUID resumeId) {
        // 验证简历归属
        ResumeHistory resume = getResumeWithOwnershipCheck(userId, resumeId);

        // 清除所有主简历标记
        resumeRepository.clearPrimaryByUserId(userId);

        // 设置新的主简历
        resume.setIsPrimary(true);
        resume = resumeRepository.save(resume);

        log.info("用户 {} 设置主简历: {}", userId, resumeId);
        return toResumeDto(resume);
    }

    /**
     * 删除简历
     */
    @Transactional
    public void deleteResume(UUID userId, UUID resumeId) {
        ResumeHistory resume = getResumeWithOwnershipCheck(userId, resumeId);
        boolean wasPrimary = Boolean.TRUE.equals(resume.getIsPrimary());

        // 删除 MinIO 文件
        storageService.deleteFile(resume.getFilePath());

        // 删除数据库记录
        resumeRepository.delete(resume);

        // 如果删除的是主简历，重新指定主简历
        if (wasPrimary) {
            resumeRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                    .ifPresent(newPrimary -> {
                        newPrimary.setIsPrimary(true);
                        resumeRepository.save(newPrimary);
                        log.info("自动设置新的主简历: {}", newPrimary.getId());
                    });
        }

        log.info("用户 {} 删除简历: {}", userId, resumeId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将 AI Engine 解析结果转换为 ParsedContent
     */
    private ParsedContent convertToParseContent(AIEngineClient.ResumeParseResult parseResult) {
        ParsedContent.PersonalInfo personalInfo = ParsedContent.PersonalInfo.builder()
                .name(parseResult.getName())
                .email(parseResult.getEmail())
                .phone(parseResult.getPhone())
                .location(parseResult.getLocation())
                .linkedin(parseResult.getLinkedin())
                .github(parseResult.getGithub())
                .website(parseResult.getWebsite())
                .build();

        // 转换工作经历
        List<ParsedContent.WorkExperience> experiences = new ArrayList<>();
        if (parseResult.getExperiences() != null) {
            for (AIEngineClient.ExperienceData exp : parseResult.getExperiences()) {
                experiences.add(ParsedContent.WorkExperience.builder()
                        .company(exp.getCompany())
                        .title(exp.getTitle())
                        .location(exp.getLocation())
                        .employmentType(exp.getEmploymentType())
                        .startDate(exp.getStartDate())
                        .endDate(exp.getEndDate())
                        .isCurrent(exp.isCurrent())
                        .description(exp.getDescription())
                        .achievements(exp.getAchievements())
                        .technologies(exp.getTechnologies())
                        .build());
            }
        }

        // 转换教育经历
        List<ParsedContent.Education> education = new ArrayList<>();
        if (parseResult.getEducations() != null) {
            for (AIEngineClient.EducationData edu : parseResult.getEducations()) {
                education.add(ParsedContent.Education.builder()
                        .school(edu.getSchool())
                        .degree(edu.getDegree())
                        .field(edu.getField())
                        .location(edu.getLocation())
                        .startDate(edu.getStartDate())
                        .endDate(edu.getEndDate())
                        .gpa(edu.getGpa())
                        .description(edu.getDescription())
                        .activities(edu.getActivities())
                        .honors(edu.getHonors())
                        .build());
            }
        }

        // 转换项目
        List<ParsedContent.Project> projects = new ArrayList<>();
        if (parseResult.getProjects() != null) {
            for (AIEngineClient.ProjectData proj : parseResult.getProjects()) {
                projects.add(ParsedContent.Project.builder()
                        .name(proj.getName())
                        .description(proj.getDescription())
                        .url(proj.getUrl())
                        .repoUrl(proj.getRepoUrl())
                        .technologies(proj.getTechnologies())
                        .startDate(proj.getStartDate())
                        .endDate(proj.getEndDate())
                        .highlights(proj.getHighlights())
                        .build());
            }
        }

        // 转换技能
        List<ParsedContent.Skill> skills = new ArrayList<>();
        if (parseResult.getSkills() != null) {
            for (AIEngineClient.SkillData skill : parseResult.getSkills()) {
                skills.add(ParsedContent.Skill.builder()
                        .name(skill.getName())
                        .level(skill.getLevel())
                        .category(skill.getCategory())
                        .build());
            }
        }

        // 转换证书
        List<ParsedContent.Certification> certifications = new ArrayList<>();
        if (parseResult.getCertifications() != null) {
            for (AIEngineClient.CertificationData cert : parseResult.getCertifications()) {
                certifications.add(ParsedContent.Certification.builder()
                        .name(cert.getName())
                        .issuer(cert.getIssuer())
                        .date(cert.getDate())
                        .url(cert.getUrl())
                        .build());
            }
        }

        // 转换语言
        List<ParsedContent.Language> languages = new ArrayList<>();
        if (parseResult.getLanguages() != null) {
            for (AIEngineClient.LanguageData lang : parseResult.getLanguages()) {
                languages.add(ParsedContent.Language.builder()
                        .language(lang.getLanguage())
                        .proficiency(lang.getProficiency())
                        .build());
            }
        }

        return ParsedContent.builder()
                .personalInfo(personalInfo)
                .headline(parseResult.getHeadline())
                .summary(parseResult.getSummary())
                .experiences(experiences)
                .education(education)
                .projects(projects)
                .skills(skills)
                .certifications(certifications)
                .languages(languages)
                .achievements(parseResult.getAchievements())
                .build();
    }

    /**
     * 标准化日期格式 (确保为 YYYY-MM-DD 格式)
     */
    private String normalizeDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        dateStr = dateStr.trim();
        
        // 如果是 "present"，返回 null
        if (dateStr.equalsIgnoreCase("present") || dateStr.equals("至今")) {
            return null;
        }
        
        // 如果是 YYYY-MM 格式，补充为 YYYY-MM-01
        if (dateStr.matches("\\d{4}-\\d{2}")) {
            return dateStr + "-01";
        }
        
        return dateStr;
    }

    /**
     * 解析日期字符串为 LocalDate
     */
    private java.time.LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("无法解析日期: {}", dateStr);
            return null;
        }
    }

    /**
     * 验证上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        String extension = getFileExtension(file.getOriginalFilename());

        // 检查 Content-Type 或文件扩展名
        boolean validType = ALLOWED_CONTENT_TYPES.contains(contentType);
        boolean validExtension = ALLOWED_EXTENSIONS.contains(extension.toLowerCase());

        if (!validType && !validExtension) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, 
                    "仅支持 PDF、DOC、DOCX、PNG、JPG 格式的文件");
        }
    }

    /**
     * 获取简历并验证所有权
     */
    private ResumeHistory getResumeWithOwnershipCheck(UUID userId, UUID resumeId) {
        ResumeHistory resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        if (!resume.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此简历");
        }

        return resume;
    }

    /**
     * 实体转 DTO
     */
    private ResumeDto toResumeDto(ResumeHistory resume) {
        String downloadUrl = storageService.getPresignedUrl(
                resume.getFilePath(), PRESIGNED_URL_EXPIRE_MINUTES);

        return ResumeDto.builder()
                .id(resume.getId().toString())
                .userId(resume.getUser().getId().toString())
                .fileName(resume.getFileName())
                .originalName(resume.getOriginalName())
                .fileSize(resume.getFileSize())
                .contentType(resume.getContentType())
                .downloadUrl(downloadUrl)
                .status(resume.getStatus().name())
                .isPrimary(resume.getIsPrimary())
                .skills(parseJsonArray(resume.getSkills()))
                .parsedContent(parseParsedContent(resume.getParsedData()))
                .notes(resume.getNotes())
                .errorMessage(resume.getErrorMessage())
                .createdAt(resume.getCreatedAt() != null ? resume.getCreatedAt().toString() : null)
                .updatedAt(resume.getUpdatedAt() != null ? resume.getUpdatedAt().toString() : null)
                .build();
    }

    /**
     * 从文件路径提取文件名
     */
    private String extractFileName(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        return lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 对象转 JSON
     */
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
     * JSON 转字符串列表
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析 JSON 数组失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * JSON 转 ParsedContent
     */
    private ParsedContent parseParsedContent(String json) {
        if (json == null || json.isBlank()) {
            return ParsedContent.empty();
        }
        try {
            return objectMapper.readValue(json, ParsedContent.class);
        } catch (JsonProcessingException e) {
            log.error("解析 ParsedContent 失败", e);
            return ParsedContent.empty();
        }
    }

    // ==================== DTOs ====================

    /**
     * 同步选项
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SyncOptions {
        private boolean syncBasicInfo = true;
        private boolean syncExperiences = true;
        private boolean syncEducations = true;
        private boolean syncSkills = true;
        private boolean syncProjects = true;
        private boolean syncLanguages = true;
        private boolean syncCertifications = true;
    }

    /**
     * 同步结果
     */
    @lombok.Data
    public static class SyncToProfileResult {
        private boolean success;
        private String errorMessage;
        private boolean basicInfoSynced;
        private int experiencesSynced;
        private int educationsSynced;
        private int skillsSynced;
        private int projectsSynced;
        private int languagesSynced;
        private int certificationsSynced;
    }
}
