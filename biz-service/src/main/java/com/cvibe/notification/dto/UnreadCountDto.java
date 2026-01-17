package com.cvibe.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for unread notification counts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountDto {

    /**
     * Total unread notifications
     */
    private int total;

    /**
     * Unread count by category
     */
    private Map<String, Integer> byCategory;

    /**
     * Count of high priority unread notifications
     */
    private int highPriority;
}
