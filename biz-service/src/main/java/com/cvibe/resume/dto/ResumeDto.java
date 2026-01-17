package com.cvibe.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历 DTO
 * 用于 API 响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDto {

    private String id;
    private String userId;
    
    /**
     * 存储的文件名
     */
    private String fileName;
    
    /**
     * 原始文件名
     */
    private String originalName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String contentType;
    
    /**
     * 下载 URL（预签名 URL）
     */
    private String downloadUrl;
    
    /**
     * 处理状态: PENDING, PROCESSING, COMPLETED, FAILED
     */
    private String status;
    
    /**
     * 是否为主简历
     */
    private Boolean isPrimary;
    
    /**
     * 从简历中提取的技能列表
     */
    private List<String> skills;
    
    /**
     * AI 解析后的简历内容
     */
    private ParsedContent parsedContent;
    
    /**
     * 用户备注
     */
    private String notes;
    
    /**
     * 错误信息（解析失败时）
     */
    private String errorMessage;
    
    private String createdAt;
    private String updatedAt;
}
