package com.cvibe.biz.admin.dto;

import com.cvibe.biz.admin.entity.AuditLog.AuditAction;
import com.cvibe.biz.admin.entity.AuditLog.AuditStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit Log DTOs
 */
public class AuditLogDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogResponse {
        private UUID id;
        private UUID userId;
        private String userEmail;
        private String userName;
        private AuditAction action;
        private String targetType;
        private UUID targetId;
        private String entityType;
        private UUID entityId;
        private String description;
        private String oldValues;
        private String newValues;
        private String ipAddress;
        private String userAgent;
        private String requestPath;
        private String requestMethod;
        private AuditStatus status;
        private String errorMessage;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogSearchRequest {
        private UUID userId;
        private AuditAction action;
        private String entityType;
        private AuditStatus status;
        private Instant startTime;
        private Instant endTime;
        private String ipAddress;
        private String keyword;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogSummary {
        private long totalLogs;
        private long successCount;
        private long failureCount;
        private long todayCount;
        private java.util.Map<String, Long> actionCounts;
        private java.util.List<TopUser> topActiveUsers;
        private java.util.List<SuspiciousActivity> suspiciousActivities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopUser {
        private UUID userId;
        private String email;
        private String name;
        private long actionCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuspiciousActivity {
        private String ipAddress;
        private String activity;
        private long count;
        private Instant lastOccurrence;
    }
}
