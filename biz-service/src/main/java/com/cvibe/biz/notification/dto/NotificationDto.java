package com.cvibe.biz.notification.dto;

import com.cvibe.biz.notification.entity.Notification.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notification DTOs
 */
public class NotificationDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationResponse {
        private UUID id;
        private NotificationType type;
        private NotificationCategory category;
        private String title;
        private String content;
        private Priority priority;
        private String actionUrl;
        private String actionText;
        private String entityType;
        private UUID entityId;
        private SenderInfo sender;
        private String imageUrl;
        private Boolean isRead;
        private Instant readAt;
        private Boolean isClicked;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderInfo {
        private UUID id;
        private String name;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationListResponse {
        private List<NotificationResponse> notifications;
        private long unreadCount;
        private Map<String, Long> unreadByCategory;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnreadCountResponse {
        private long total;
        private Map<String, Long> byCategory;
        private long highPriority;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateNotificationRequest {
        private UUID userId;
        private NotificationType type;
        private NotificationCategory category;
        private String title;
        private String content;
        private Priority priority;
        private String actionUrl;
        private String actionText;
        private String entityType;
        private UUID entityId;
        private UUID senderId;
        private String imageUrl;
        private String metadata;
        private DeliveryChannel channel;
        private Instant expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkNotificationRequest {
        private List<UUID> userIds;
        private NotificationType type;
        private NotificationCategory category;
        private String title;
        private String content;
        private Priority priority;
        private String actionUrl;
        private String actionText;
        private DeliveryChannel channel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarkReadRequest {
        private List<UUID> notificationIds;
        private NotificationCategory category;
        private Boolean all;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationSummary {
        private long totalNotifications;
        private long unreadCount;
        private long todayCount;
        private Map<String, Long> countByCategory;
        private Map<String, Long> countByType;
        private List<NotificationResponse> recentHighPriority;
    }
}
