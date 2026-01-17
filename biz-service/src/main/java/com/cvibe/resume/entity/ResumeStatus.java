package com.cvibe.resume.entity;

/**
 * 简历处理状态枚举
 */
public enum ResumeStatus {
    /**
     * 待处理
     */
    PENDING,
    
    /**
     * 处理中（AI 解析中）
     */
    PROCESSING,
    
    /**
     * 处理完成
     */
    COMPLETED,
    
    /**
     * 处理失败
     */
    FAILED
}
