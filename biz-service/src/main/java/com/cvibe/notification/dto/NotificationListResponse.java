package com.cvibe.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for paginated notification list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponse {

    /**
     * List of notifications
     */
    private List<NotificationDto> notifications;

    /**
     * Unread count summary
     */
    private UnreadCountDto unreadCount;

    /**
     * Current page number (0-based)
     */
    private int page;

    /**
     * Page size
     */
    private int size;

    /**
     * Total number of elements
     */
    private long totalElements;

    /**
     * Total number of pages
     */
    private int totalPages;

    /**
     * Whether this is the last page
     */
    private boolean last;
}
