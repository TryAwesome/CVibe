package com.cvibe.settings.entity;

import com.cvibe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * 用户 AI 配置实体
 * 存储用户的 AI 交互偏好设置
 */
@Entity
@Table(name = "user_ai_configs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * 语言偏好: zh, en
     */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String language = "zh";

    /**
     * AI 响应风格: concise, detailed, balanced
     */
    @Column(name = "response_style", nullable = false, length = 20)
    @Builder.Default
    private String responseStyle = "balanced";

    /**
     * 面试难度: easy, medium, hard
     */
    @Column(name = "interview_difficulty", length = 10)
    @Builder.Default
    private String interviewDifficulty = "medium";

    /**
     * 关注领域，存储为 JSON 数组
     * 例如: ["algorithms", "system-design", "behavioral"]
     */
    @Column(name = "focus_areas", columnDefinition = "TEXT")
    private String focusAreas;

    /**
     * 自定义指令
     */
    @Column(name = "custom_instructions", length = 1000)
    private String customInstructions;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
