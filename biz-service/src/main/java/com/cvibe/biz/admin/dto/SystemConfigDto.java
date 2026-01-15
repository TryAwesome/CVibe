package com.cvibe.biz.admin.dto;

import com.cvibe.biz.admin.entity.SystemConfig.ConfigCategory;
import com.cvibe.biz.admin.entity.SystemConfig.ConfigValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * System Config DTOs
 */
public class SystemConfigDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigResponse {
        private UUID id;
        private ConfigCategory category;
        private String configKey;
        private String configValue;
        private ConfigValueType valueType;
        private String defaultValue;
        private String description;
        private Boolean isEncrypted;
        private Boolean isReadonly;
        private Boolean isActive;
        private Boolean isSensitive;
        private Boolean isEditable;
        private String modifiedByName;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigListResponse {
        private ConfigCategory category;
        private List<ConfigResponse> configs;
        private int totalCount;
        private int activeCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateConfigRequest {
        private ConfigCategory category;
        private String configKey;
        private String configValue;
        private ConfigValueType valueType;
        private String defaultValue;
        private String description;
        private Boolean encrypted;
        private Boolean readonly;
        private Boolean isSensitive;
        private Boolean isEditable;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateConfigRequest {
        private String configValue;
        private String description;
        private Boolean isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchUpdateRequest {
        private List<ConfigUpdate> updates;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigUpdate {
        private String configKey;
        private String configValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureFlagRequest {
        private String key;
        private String description;
        private Boolean enabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigSummary {
        private long totalConfigs;
        private long activeConfigs;
        private long sensitiveConfigs;
        private java.util.Map<ConfigCategory, Long> countByCategory;
        private List<String> recentlyModified;
    }
}
