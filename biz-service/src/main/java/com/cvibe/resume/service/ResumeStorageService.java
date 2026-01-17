package com.cvibe.resume.service;

import com.cvibe.common.config.MinioConfig;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 简历文件存储服务
 * 处理 MinIO 文件上传、下载、删除操作
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeStorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 初始化时确保 bucket 存在
     */
    @PostConstruct
    public void init() {
        try {
            String bucketName = minioConfig.getBucket().getResumes();
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建 MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.warn("初始化 MinIO bucket 失败，将在首次上传时重试: {}", e.getMessage());
        }
    }

    /**
     * 上传文件到 MinIO
     *
     * @param file 上传的文件
     * @param userId 用户 ID
     * @return 文件存储路径
     */
    public String uploadFile(MultipartFile file, UUID userId) {
        try {
            String bucketName = minioConfig.getBucket().getResumes();
            
            // 确保 bucket 存在
            ensureBucketExists(bucketName);

            // 生成文件路径: resumes/{userId}/{uuid}.{extension}
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String objectName = String.format("resumes/%s/%s.%s", 
                    userId.toString(), UUID.randomUUID().toString(), extension);

            // 上传文件
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
            }

            log.info("文件上传成功: bucket={}, object={}", bucketName, objectName);
            return objectName;

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param objectName 文件路径
     */
    public void deleteFile(String objectName) {
        try {
            String bucketName = minioConfig.getBucket().getResumes();
            
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());

            log.info("文件删除成功: bucket={}, object={}", bucketName, objectName);

        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                log.warn("文件不存在，跳过删除: {}", objectName);
            } else {
                log.error("文件删除失败", e);
            }
        } catch (Exception e) {
            log.error("文件删除失败", e);
        }
    }

    /**
     * 获取文件的预签名 URL
     *
     * @param objectName 文件路径
     * @param expireMinutes URL 有效期（分钟）
     * @return 预签名 URL
     */
    public String getPresignedUrl(String objectName, int expireMinutes) {
        try {
            String bucketName = minioConfig.getBucket().getResumes();
            
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(expireMinutes, TimeUnit.MINUTES)
                            .build());

        } catch (Exception e) {
            log.error("生成预签名 URL 失败", e);
            return null;
        }
    }

    /**
     * 获取文件内容（用于 AI 解析）
     *
     * @param objectName 文件路径
     * @return 文件字节数组
     */
    public byte[] getFileContent(String objectName) {
        try {
            String bucketName = minioConfig.getBucket().getResumes();
            
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build())) {
                return stream.readAllBytes();
            }

        } catch (Exception e) {
            log.error("获取文件内容失败", e);
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND, "文件不存在或无法读取");
        }
    }

    /**
     * 确保 bucket 存在
     */
    private void ensureBucketExists(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build());
        
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("创建 MinIO bucket: {}", bucketName);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "pdf"; // 默认扩展名
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
