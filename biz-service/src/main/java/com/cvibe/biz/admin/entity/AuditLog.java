package com.cvibe.biz.admin.entity;

import com.cvibe.biz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * AuditLog Entity
 * 
 * Tracks all important actions in the system for security and compliance.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_logs_action", columnList = "action"),
        @Index(name = "idx_audit_logs_entity_type", columnList = "entity_type"),
        @Index(name = "idx_audit_logs_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User who performed the action (null for system actions)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    /**
     * Action type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;

    /**
     * Entity type affected
     */
    @Column(name = "entity_type")
    private String entityType;

    /**
     * Entity ID affected
     */
    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * Description of the action
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Old values (JSON) for update actions
     */
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    /**
     * New values (JSON) for create/update actions
     */
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    /**
     * IP address of the request
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * User agent of the request
     */
    @Column(name = "user_agent")
    private String userAgent;

    /**
     * Request path
     */
    @Column(name = "request_path")
    private String requestPath;

    /**
     * Request method
     */
    @Column(name = "request_method")
    private String requestMethod;

    /**
     * Status (success/failure)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private AuditStatus status = AuditStatus.SUCCESS;

    /**
     * Error message if failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ================== Enums ==================

    public enum AuditAction {
        // Auth
        LOGIN,
        LOGOUT,
        REGISTER,
        PASSWORD_CHANGE,
        PASSWORD_RESET,
        
        // User Management
        USER_CREATE,
        USER_UPDATE,
        USER_DELETE,
        USER_DISABLE,
        USER_ENABLE,
        ROLE_CHANGE,
        
        // Profile
        PROFILE_UPDATE,
        RESUME_UPLOAD,
        RESUME_DELETE,
        
        // Content
        POST_CREATE,
        POST_UPDATE,
        POST_DELETE,
        COMMENT_DELETE,
        
        // Admin
        CONFIG_UPDATE,
        ANNOUNCEMENT_CREATE,
        DATA_EXPORT,
        BULK_OPERATION,
        
        // System
        SYSTEM_STARTUP,
        SYSTEM_SHUTDOWN,
        SCHEDULED_TASK
    }

    public enum AuditStatus {
        SUCCESS,
        FAILURE,
        PARTIAL
    }

    // ================== Static Factory ==================

    public static AuditLog create(User user, AuditAction action, String description) {
        return AuditLog.builder()
                .user(user)
                .action(action)
                .description(description)
                .status(AuditStatus.SUCCESS)
                .build();
    }

    public static AuditLog forEntity(User user, AuditAction action, String entityType, UUID entityId, String description) {
        return AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .status(AuditStatus.SUCCESS)
                .build();
    }

    public static AuditLog failure(User user, AuditAction action, String description, String errorMessage) {
        return AuditLog.builder()
                .user(user)
                .action(action)
                .description(description)
                .status(AuditStatus.FAILURE)
                .errorMessage(errorMessage)
                .build();
    }
}
