package com.cvibe.biz.resume.service;

import com.cvibe.biz.resume.dto.ResumeHistoryDto;
import com.cvibe.biz.resume.entity.ResumeHistory;
import com.cvibe.biz.resume.repository.ResumeHistoryRepository;
import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.response.ErrorCode;
import com.cvibe.common.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private static final String RESUME_FOLDER = "resumes";
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final ResumeHistoryRepository resumeRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    /**
     * Upload a resume file
     */
    @Transactional
    public ResumeHistoryDto uploadResume(UUID userId, MultipartFile file, String notes) {
        validateFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String objectPath = storageService.uploadFile(file, RESUME_FOLDER + "/" + userId);

        // Determine version number
        long count = resumeRepository.countByUserId(userId);
        int version = (int) count + 1;

        ResumeHistory resume = ResumeHistory.builder()
                .user(user)
                .fileName(objectPath.substring(objectPath.lastIndexOf("/") + 1))
                .originalName(file.getOriginalFilename())
                .filePath(objectPath)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .source(ResumeHistory.ResumeSource.UPLOADED)
                .version(version)
                .isPrimary(count == 0)  // First upload is primary
                .notes(notes)
                .build();

        resume = resumeRepository.save(resume);
        log.info("Resume uploaded: userId={}, resumeId={}", userId, resume.getId());

        return mapToDto(resume);
    }

    /**
     * Save a generated resume (from profile + template)
     */
    @Transactional
    public ResumeHistoryDto saveGeneratedResume(UUID userId, InputStream pdfStream, 
                                                 long size, UUID templateId,
                                                 String targetJobTitle, String targetCompany) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String fileName = UUID.randomUUID() + ".pdf";
        String objectPath = storageService.uploadFile(
                pdfStream,
                RESUME_FOLDER + "/" + userId,
                fileName,
                "application/pdf",
                size
        );

        long count = resumeRepository.countByUserId(userId);

        ResumeHistory resume = ResumeHistory.builder()
                .user(user)
                .fileName(fileName)
                .originalName("resume_" + System.currentTimeMillis() + ".pdf")
                .filePath(objectPath)
                .fileSize(size)
                .contentType("application/pdf")
                .source(ResumeHistory.ResumeSource.GENERATED)
                .templateId(templateId)
                .targetJobTitle(targetJobTitle)
                .targetCompany(targetCompany)
                .version((int) count + 1)
                .isPrimary(false)
                .build();

        resume = resumeRepository.save(resume);
        log.info("Generated resume saved: userId={}, resumeId={}", userId, resume.getId());

        return mapToDto(resume);
    }

    /**
     * Get resume history list
     */
    @Transactional(readOnly = true)
    public Page<ResumeHistoryDto> getResumeHistory(UUID userId, Pageable pageable) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDto);
    }

    /**
     * Get all resumes for user
     */
    @Transactional(readOnly = true)
    public List<ResumeHistoryDto> getAllResumes(UUID userId) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Get single resume
     */
    @Transactional(readOnly = true)
    public ResumeHistoryDto getResume(UUID userId, UUID resumeId) {
        ResumeHistory resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
        return mapToDto(resume);
    }

    /**
     * Get primary resume
     */
    @Transactional(readOnly = true)
    public ResumeHistoryDto getPrimaryResume(UUID userId) {
        ResumeHistory resume = resumeRepository.findByUserIdAndIsPrimaryTrue(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
        return mapToDto(resume);
    }

    /**
     * Download resume file
     */
    public InputStream downloadResume(UUID userId, UUID resumeId) {
        ResumeHistory resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
        return storageService.downloadFile(resume.getFilePath());
    }

    /**
     * Set resume as primary
     */
    @Transactional
    public ResumeHistoryDto setPrimary(UUID userId, UUID resumeId) {
        ResumeHistory resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        // Clear existing primary
        resumeRepository.clearPrimaryForUser(userId);

        resume.setIsPrimary(true);
        resume = resumeRepository.save(resume);

        log.info("Set primary resume: userId={}, resumeId={}", userId, resumeId);
        return mapToDto(resume);
    }

    /**
     * Delete resume
     */
    @Transactional
    public void deleteResume(UUID userId, UUID resumeId) {
        ResumeHistory resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        // Delete from storage
        storageService.deleteFile(resume.getFilePath());

        // Delete from database
        resumeRepository.delete(resume);

        log.info("Resume deleted: userId={}, resumeId={}", userId, resumeId);
    }

    // ==================== Helper Methods ====================

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE,
                    "Allowed types: PDF, DOC, DOCX");
        }
    }

    private ResumeHistoryDto mapToDto(ResumeHistory resume) {
        String downloadUrl = null;
        try {
            downloadUrl = storageService.getPresignedUrl(
                    resume.getFilePath(), 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to generate presigned URL for resume: {}", resume.getId());
        }

        return ResumeHistoryDto.builder()
                .id(resume.getId())
                .fileName(resume.getFileName())
                .originalName(resume.getOriginalName())
                .fileSize(resume.getFileSize())
                .contentType(resume.getContentType())
                .source(resume.getSource())
                .templateId(resume.getTemplateId())
                .targetJobTitle(resume.getTargetJobTitle())
                .targetCompany(resume.getTargetCompany())
                .version(resume.getVersion())
                .isPrimary(resume.getIsPrimary())
                .notes(resume.getNotes())
                .createdAt(resume.getCreatedAt())
                .downloadUrl(downloadUrl)
                .build();
    }
}
