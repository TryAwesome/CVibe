package com.cvibe.biz.settings.dto;

import com.cvibe.biz.user.entity.UserAiConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * AI Configuration Request/Response DTO
 * Supports separate Language Model and Vision Model configurations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfigDto {

    // ============ Language Model Configuration ============
    private String baseUrl;
    private String apiKey;  // Only for request, masked in response
    private String modelName;
    private String provider;
    private boolean configured;
    
    // ============ Vision Model Configuration ============
    private String visionBaseUrl;
    private String visionApiKey;  // Only for request, masked in response
    private String visionModelName;
    private String visionProvider;
    private boolean visionConfigured;
    
    // ============ Metadata ============
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Convert entity to response DTO (with masked API keys)
     */
    public static AiConfigDto fromEntity(UserAiConfig config) {
        if (config == null) {
            return AiConfigDto.builder()
                    .configured(false)
                    .visionConfigured(false)
                    .build();
        }

        return AiConfigDto.builder()
                // Language Model
                .baseUrl(config.getBaseUrl())
                .apiKey(maskApiKey(config.getApiKeyEncrypted()))
                .modelName(config.getModelName())
                .provider(config.getProvider())
                .configured(config.isConfigured())
                // Vision Model
                .visionBaseUrl(config.getVisionBaseUrl())
                .visionApiKey(maskApiKey(config.getVisionApiKeyEncrypted()))
                .visionModelName(config.getVisionModelName())
                .visionProvider(config.getVisionProvider())
                .visionConfigured(config.isVisionConfigured())
                // Metadata
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    /**
     * Mask API key for security (show only first and last 4 chars)
     */
    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
