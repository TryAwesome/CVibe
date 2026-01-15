package com.cvibe.biz.settings.dto;

import com.cvibe.biz.user.entity.UserAiConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI Configuration Request/Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfigDto {

    private String baseUrl;
    private String apiKey;  // Only for request, masked in response
    private String modelName;
    private String provider;
    private boolean configured;

    /**
     * Convert entity to response DTO (with masked API key)
     */
    public static AiConfigDto fromEntity(UserAiConfig config) {
        if (config == null) {
            return AiConfigDto.builder()
                    .configured(false)
                    .build();
        }

        return AiConfigDto.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(maskApiKey(config.getApiKeyEncrypted()))
                .modelName(config.getModelName())
                .provider(config.getProvider())
                .configured(config.isConfigured())
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
