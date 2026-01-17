package com.cvibe.resumebuilder.entity;

/**
 * 简历生成状态枚举
 */
public enum GenerationStatus {
    /**
     * 处理中
     */
    PROCESSING,
    
    /**
     * 已完成
     */
    COMPLETED,
    
    /**
     * 失败
     */
    FAILED
}
