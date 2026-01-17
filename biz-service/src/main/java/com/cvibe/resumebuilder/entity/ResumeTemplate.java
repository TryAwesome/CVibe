package com.cvibe.resumebuilder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * 简历模板实体
 */
@Entity
@Table(name = "resume_templates")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 模板名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 模板描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 预览缩略图 URL
     */
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    /**
     * 模板分类
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TemplateCategory category;

    /**
     * 模板文件路径（Thymeleaf 模板或 LaTeX 模板）
     */
    @Column(name = "template_file")
    private String templateFile;

    /**
     * LaTeX 模板内容
     */
    @Column(name = "latex_template", columnDefinition = "TEXT")
    private String latexTemplate;

    /**
     * 示例 HTML（用于预览）
     */
    @Column(name = "sample_html", columnDefinition = "TEXT")
    private String sampleHtml;

    /**
     * 是否为高级模板（需要订阅）
     */
    @Column(name = "is_premium")
    @Builder.Default
    private Boolean isPremium = false;

    /**
     * 是否为推荐模板
     */
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * 是否激活
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * 获取缩略图，提供默认值
     */
    public String getThumbnailUrl() {
        return thumbnailUrl != null ? thumbnailUrl : "/images/default-template.png";
    }
}
