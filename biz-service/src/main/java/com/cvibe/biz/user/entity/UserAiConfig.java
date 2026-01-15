package com.cvibe.biz.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * User AI Configuration Entity
 * Stores user's custom AI API settings (encrypted)
 */
@Entity
@Table(name = "user_ai_configs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * AI Provider base URL (e.g., https://api.openai.com/v1)
     */
    @Column(name = "base_url")
    private String baseUrl;

    /**
     * API Key (encrypted at rest)
     */
    @Column(name = "api_key_encrypted")
    private String apiKeyEncrypted;

    /**
     * Model name (e.g., gpt-4, gpt-3.5-turbo)
     */
    @Column(name = "model_name", length = 100)
    private String modelName;

    /**
     * Provider type for UI display
     */
    @Column(name = "provider", length = 50)
    private String provider;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Check if AI config is properly set up
     */
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isEmpty()
                && apiKeyEncrypted != null && !apiKeyEncrypted.isEmpty()
                && modelName != null && !modelName.isEmpty();
    }
}
