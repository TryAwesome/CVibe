package com.cvibe.resumebuilder.entity;

import com.cvibe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * 简历生成记录实体
 */
@Entity
@Table(name = "resume_generations", indexes = {
    @Index(name = "idx_generation_user_id", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ResumeTemplate template;

    /**
     * 目标职位
     */
    @Column(name = "target_position", length = 100)
    private String targetPosition;

    /**
     * 目标公司
     */
    @Column(name = "target_company", length = 100)
    private String targetCompany;

    /**
     * 生成状态
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private GenerationStatus status = GenerationStatus.PROCESSING;

    /**
     * 生成的内容（JSON 格式）
     */
    @Column(name = "content_json", columnDefinition = "TEXT")
    private String contentJson;

    /**
     * LaTeX 内容
     */
    @Column(name = "latex_content", columnDefinition = "TEXT")
    private String latexContent;

    /**
     * HTML 预览
     */
    @Column(name = "html_preview", columnDefinition = "TEXT")
    private String htmlPreview;

    /**
     * PDF 文件路径（MinIO）
     */
    @Column(name = "pdf_path")
    private String pdfPath;

    /**
     * 错误信息
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /**
     * 自定义指令
     */
    @Column(name = "custom_instructions", columnDefinition = "TEXT")
    private String customInstructions;

    /**
     * 定制选项（JSON）
     */
    @Column(name = "customizations", columnDefinition = "TEXT")
    private String customizations;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
