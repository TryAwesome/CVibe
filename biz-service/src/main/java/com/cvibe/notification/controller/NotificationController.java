package com.cvibe.notification.controller;

import com.cvibe.common.dto.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import com.cvibe.notification.dto.*;
import com.cvibe.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for notifications
 * 
 * API Base Path: /api/v1/notifications
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get notifications for current user (paginated)
     * GET /api/v1/notifications
     */
    @GetMapping
    public ApiResponse<NotificationListResponse> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean unreadOnly) {
        log.info("Getting notifications for user: {}, page: {}, size: {}", principal.getId(), page, size);
        NotificationListResponse response = notificationService.getNotifications(
                principal.getId(), page, size, category, unreadOnly);
        return ApiResponse.success(response);
    }

    /**
     * Get recent notifications
     * GET /api/v1/notifications/recent
     */
    @GetMapping("/recent")
    public ApiResponse<List<NotificationDto>> getRecentNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "5") int limit) {
        log.info("Getting recent {} notifications for user: {}", limit, principal.getId());
        List<NotificationDto> notifications = notificationService.getRecentNotifications(principal.getId(), limit);
        return ApiResponse.success(notifications);
    }

    /**
     * Get unread notification count with breakdown
     * GET /api/v1/notifications/unread/count
     */
    @GetMapping("/unread/count")
    public ApiResponse<UnreadCountDto> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Getting unread count for user: {}", principal.getId());
        UnreadCountDto unreadCount = notificationService.getUnreadCount(principal.getId());
        return ApiResponse.success(unreadCount);
    }

    /**
     * Mark a notification as read
     * POST /api/v1/notifications/{notificationId}/read
     */
    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationDto> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID notificationId) {
        log.info("Marking notification {} as read for user: {}", notificationId, principal.getId());
        NotificationDto notification = notificationService.markAsRead(principal.getId(), notificationId);
        return ApiResponse.success(notification);
    }

    /**
     * Mark all notifications as read
     * POST /api/v1/notifications/read-all
     */
    @PostMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Marking all notifications as read for user: {}", principal.getId());
        int count = notificationService.markAllAsRead(principal.getId());
        return ApiResponse.success(Map.of("markedAsRead", count));
    }

    /**
     * Delete a notification
     * DELETE /api/v1/notifications/{notificationId}
     */
    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteNotification(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID notificationId) {
        log.info("Deleting notification {} for user: {}", notificationId, principal.getId());
        notificationService.deleteNotification(principal.getId(), notificationId);
        return ApiResponse.success(null);
    }
}
