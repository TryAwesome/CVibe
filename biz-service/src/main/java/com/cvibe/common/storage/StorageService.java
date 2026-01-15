package com.cvibe.common.storage;

import com.cvibe.common.config.MinioConfig;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.response.ErrorCode;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO Storage Service
 * Handles file upload, download, and management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * Upload a file to MinIO
     *
     * @param file     MultipartFile to upload
     * @param folder   Folder/prefix in bucket (e.g., "resumes", "avatars")
     * @return Object path in MinIO
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            ensureBucketExists();

            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String objectName = folder + "/" + UUID.randomUUID() + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("File uploaded successfully: {}", objectName);
            return objectName;

        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * Upload file with custom object name
     */
    public String uploadFile(MultipartFile file, String folder, String customName) {
        try {
            ensureBucketExists();

            String extension = getFileExtension(file.getOriginalFilename());
            String objectName = folder + "/" + customName + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("File uploaded successfully: {}", objectName);
            return objectName;

        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * Upload file from InputStream
     */
    public String uploadFile(InputStream inputStream, String folder, String fileName, 
                            String contentType, long size) {
        try {
            ensureBucketExists();

            String objectName = folder + "/" + fileName;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("File uploaded successfully: {}", objectName);
            return objectName;

        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * Download a file from MinIO
     *
     * @param objectPath Full object path in MinIO
     * @return InputStream of the file
     */
    public InputStream downloadFile(String objectPath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectPath)
                            .build()
            );
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to download file: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    /**
     * Get presigned URL for temporary access
     *
     * @param objectPath Object path in MinIO
     * @param expiry     Expiry time
     * @param timeUnit   Time unit for expiry
     * @return Presigned URL
     */
    public String getPresignedUrl(String objectPath, int expiry, TimeUnit timeUnit) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectPath)
                            .method(Method.GET)
                            .expiry(expiry, timeUnit)
                            .build()
            );
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Delete a file from MinIO
     *
     * @param objectPath Object path to delete
     */
    public void deleteFile(String objectPath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectPath)
                            .build()
            );
            log.info("File deleted successfully: {}", objectPath);
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to delete file: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String objectPath) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectPath)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get file metadata
     */
    public StatObjectResponse getFileInfo(String objectPath) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectPath)
                            .build()
            );
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to get file info: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    /**
     * Ensure bucket exists, create if not
     */
    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .build()
                );
                log.info("Created bucket: {}", minioConfig.getBucketName());
            }
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to ensure bucket exists: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
