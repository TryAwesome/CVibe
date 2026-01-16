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
     * Language Model name (e.g., gpt-4, gpt-3.5-turbo)
     */
    @Column(name = "model_name", length = 100)
    private String modelName;

    /**
     * Provider type for UI display
     */
    @Column(name = "provider", length = 50)
    private String provider;

    // ============ Vision Model Configuration ============
    
    /**
     * Vision Model base URL (can be different from language model)
     */
    @Column(name = "vision_base_url")
    private String visionBaseUrl;
    
    /**
     * Vision Model API Key (encrypted at rest)
     */
    @Column(name = "vision_api_key_encrypted")
    private String visionApiKeyEncrypted;
    
    /**
     * Vision Model name (e.g., gpt-4o, gemini-pro-vision)
     */
    @Column(name = "vision_model_name", length = 100)
    private String visionModelName;
    
    /**
     * Vision Model provider type
     */
    @Column(name = "vision_provider", length = 50)
    private String visionProvider;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Check if Language Model config is properly set up
     */
    public boolean isConfigured() {
        return apiKeyEncrypted != null && !apiKeyEncrypted.isEmpty()
                && modelName != null && !modelName.isEmpty();
    }
    
    /**
     * Check if Vision Model config is properly set up
     */
    public boolean isVisionConfigured() {
        return visionApiKeyEncrypted != null && !visionApiKeyEncrypted.isEmpty()
                && visionModelName != null && !visionModelName.isEmpty();
    }
}
