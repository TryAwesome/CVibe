package com.cvibe.resume.service;

import com.cvibe.auth.entity.User;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
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

    // 允许的文件类型
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    // 允许的文件扩展名
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    // 最大文件大小（5MB）
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

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

        // 6. 尝试解析简历（异步或同步）
        try {
            // TODO: 调用 AI Engine gRPC 服务解析简历
            // 暂时使用 mock 数据
            ParsedContent parsedContent = createMockParsedContent(file.getOriginalFilename());
            
            resume.setParsedData(toJson(parsedContent));
            resume.setSkills(toJson(parsedContent.getSkills()));
            resume.setStatus(ResumeStatus.COMPLETED);
            resume = resumeRepository.save(resume);

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
                    "仅支持 PDF、DOC、DOCX 格式的文件");
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

    /**
     * 创建模拟的解析内容（TODO: 替换为真实 AI 解析）
     */
    private ParsedContent createMockParsedContent(String filename) {
        return ParsedContent.builder()
                .personalInfo(ParsedContent.PersonalInfo.builder()
                        .name("待解析")
                        .email("")
                        .phone("")
                        .location("")
                        .build())
                .summary("简历文件: " + filename + " 已上传，等待 AI 解析服务就绪后进行完整解析。")
                .experiences(Collections.emptyList())
                .education(Collections.emptyList())
                .skills(List.of("待解析"))
                .build();
    }
}
