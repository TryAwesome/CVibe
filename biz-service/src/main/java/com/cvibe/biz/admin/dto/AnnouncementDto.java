package com.cvibe.biz.admin.dto;

import com.cvibe.biz.admin.entity.Announcement.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Announcement DTOs
 */
public class AnnouncementDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnouncementResponse {
        private UUID id;
        private String title;
        private String content;
        private AnnouncementType announcementType;
        private Priority priority;
        private TargetAudience targetAudience;
        private AnnouncementStatus status;
        private Instant startTime;
        private Instant endTime;
        private Boolean isPinned;
        private Boolean isDismissible;
        private String linkUrl;
        private String linkText;
        private Integer viewCount;
        private Integer dismissCount;
        private String createdByName;
        private Instant createdAt;
        private Instant updatedAt;
        private boolean isCurrentlyActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAnnouncementRequest {
        private String title;
        private String content;
        private AnnouncementType announcementType;
        private Priority priority;
        private TargetAudience targetAudience;
        private Instant startTime;
        private Instant endTime;
        private Boolean isPinned;
        private Boolean isDismissible;
        private String linkUrl;
        private String linkText;
        private Boolean publishImmediately;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateAnnouncementRequest {
        private String title;
        private String content;
        private AnnouncementType announcementType;
        private Priority priority;
        private TargetAudience targetAudience;
        private Instant startTime;
        private Instant endTime;
        private Boolean isPinned;
        private Boolean isDismissible;
        private String linkUrl;
        private String linkText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnouncementSearchRequest {
        private AnnouncementStatus status;
        private AnnouncementType type;
        private Priority priority;
        private String keyword;
    }

    /**
     * DTO for user-facing announcement (simplified)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAnnouncementResponse {
        private UUID id;
        private String title;
        private String content;
        private AnnouncementType type;
        private Priority priority;
        private Boolean isPinned;
        private Boolean isDismissible;
        private String linkUrl;
        private String linkText;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnouncementSummary {
        private long totalAnnouncements;
        private long activeAnnouncements;
        private long scheduledAnnouncements;
        private long draftAnnouncements;
        private long totalViews;
        private long totalDismissals;
        private java.util.Map<AnnouncementType, Long> countByType;
    }
}
