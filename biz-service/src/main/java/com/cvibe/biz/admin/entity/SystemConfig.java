package com.cvibe.biz.admin.entity;

import com.cvibe.biz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * SystemConfig Entity
 * 
 * Stores dynamic system configuration as key-value pairs.
 * Allows runtime configuration changes without redeployment.
 */
@Entity
@Table(name = "system_configs", indexes = {
        @Index(name = "idx_system_configs_category", columnList = "category"),
        @Index(name = "idx_system_configs_key", columnList = "config_key", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Configuration category
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ConfigCategory category;

    /**
     * Configuration key (unique)
     */
    @Column(name = "config_key", nullable = false, unique = true)
    private String configKey;

    /**
     * Configuration value
     */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    /**
     * Value type for validation
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false)
    @Builder.Default
    private ConfigValueType valueType = ConfigValueType.STRING;

    /**
     * Default value
     */
    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    /**
     * Human-readable description
     */
    @Column(name = "description")
    private String description;

    /**
     * Whether the config is active
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Whether the config is sensitive (password, API key, etc.)
     */
    @Column(name = "is_sensitive")
    @Builder.Default
    private Boolean isSensitive = false;

    /**
     * Whether the config is editable at runtime
     */
    @Column(name = "is_editable")
    @Builder.Default
    private Boolean isEditable = true;

    /**
     * Last modified by
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    @ToString.Exclude
    private User modifiedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ================== Enums ==================

    public enum ConfigCategory {
        GENERAL,        // General settings
        SECURITY,       // Security settings
        AI,             // AI/ML settings
        EMAIL,          // Email settings
        NOTIFICATION,   // Notification settings
        RATE_LIMIT,     // Rate limiting
        FEATURE_FLAG,   // Feature flags
        INTEGRATION,    // Third-party integrations
        MAINTENANCE     // Maintenance mode settings
    }

    public enum ConfigValueType {
        STRING,
        INTEGER,
        BOOLEAN,
        DECIMAL,
        JSON,
        CSV,
        URL,
        EMAIL
    }

    // ================== Static Factory ==================

    public static SystemConfig of(ConfigCategory category, String key, String value, String description) {
        return SystemConfig.builder()
                .category(category)
                .configKey(key)
                .configValue(value)
                .description(description)
                .build();
    }

    public static SystemConfig featureFlag(String key, boolean enabled, String description) {
        return SystemConfig.builder()
                .category(ConfigCategory.FEATURE_FLAG)
                .configKey(key)
                .configValue(String.valueOf(enabled))
                .valueType(ConfigValueType.BOOLEAN)
                .description(description)
                .build();
    }

    // ================== Value Getters ==================

    public String getValueOrDefault() {
        return configValue != null ? configValue : defaultValue;
    }

    public boolean getAsBoolean() {
        String val = getValueOrDefault();
        return val != null && Boolean.parseBoolean(val);
    }

    public int getAsInt(int defaultVal) {
        try {
            String val = getValueOrDefault();
            return val != null ? Integer.parseInt(val) : defaultVal;
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    public String getMaskedValue() {
        if (isSensitive && configValue != null && configValue.length() > 4) {
            return configValue.substring(0, 2) + "****" + configValue.substring(configValue.length() - 2);
        }
        return configValue;
    }
}
