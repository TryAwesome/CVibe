package com.cvibe.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历上传响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUploadResponse {

    /**
     * 上传的简历信息
     */
    private ResumeDto resume;

    /**
     * 解析状态: SUCCESS, PARTIAL, FAILED
     */
    private String parseStatus;

    /**
     * 解析消息（成功或失败的原因）
     */
    private String message;

    /**
     * 创建成功响应
     */
    public static ResumeUploadResponse success(ResumeDto resume) {
        return ResumeUploadResponse.builder()
                .resume(resume)
                .parseStatus("SUCCESS")
                .message("简历上传并解析成功")
                .build();
    }

    /**
     * 创建部分成功响应（文件上传成功但解析部分失败）
     */
    public static ResumeUploadResponse partial(ResumeDto resume, String message) {
        return ResumeUploadResponse.builder()
                .resume(resume)
                .parseStatus("PARTIAL")
                .message(message)
                .build();
    }

    /**
     * 创建失败响应（文件上传成功但解析完全失败）
     */
    public static ResumeUploadResponse failed(ResumeDto resume, String message) {
        return ResumeUploadResponse.builder()
                .resume(resume)
                .parseStatus("FAILED")
                .message(message)
                .build();
    }
}
