package com.cvibe.notification.dto;

import com.cvibe.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private String id;
    private String type;
    private String priority;
    private String category;
    private String title;
    private String content;
    private String actionUrl;
    private String actionText;
    private String dataJson;
    private Boolean isRead;
    private String createdAt;

    /**
     * Convert entity to DTO
     */
    public static NotificationDto fromEntity(Notification entity) {
        return NotificationDto.builder()
                .id(entity.getId().toString())
                .type(entity.getType().name())
                .priority(entity.getPriority().name())
                .category(entity.getCategory())
                .title(entity.getTitle())
                .content(entity.getContent())
                .actionUrl(entity.getActionUrl())
                .actionText(entity.getActionText())
                .dataJson(entity.getDataJson())
                .isRead(entity.getIsRead())
                .createdAt(formatInstant(entity.getCreatedAt()))
                .build();
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
