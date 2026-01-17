package com.cvibe.resume.entity;

import com.cvibe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * 简历历史实体
 * 存储用户上传的简历文件及解析结果
 */
@Entity
@Table(name = "resume_history", indexes = {
    @Index(name = "idx_resume_user_id", columnList = "user_id"),
    @Index(name = "idx_resume_is_primary", columnList = "user_id, is_primary")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 存储的文件名（UUID格式）
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * 原始文件名
     */
    @Column(name = "original_name", length = 255)
    private String originalName;

    /**
     * MinIO/S3 对象路径
     */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 文件 MIME 类型
     */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /**
     * 处理状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private ResumeStatus status = ResumeStatus.PENDING;

    /**
     * 是否为主简历
     */
    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    /**
     * 从简历中提取的技能列表（JSON 数组）
     * 例如: ["Java", "Python", "Spring Boot"]
     */
    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    /**
     * AI 解析后的简历数据（JSON 对象）
     */
    @Column(name = "parsed_data", columnDefinition = "TEXT")
    private String parsedData;

    /**
     * 用户备注
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * 解析失败时的错误信息
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
