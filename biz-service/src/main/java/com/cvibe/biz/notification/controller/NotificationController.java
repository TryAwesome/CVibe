package com.cvibe.biz.notification.controller;

import com.cvibe.biz.notification.dto.NotificationDto.*;
import com.cvibe.biz.notification.dto.NotificationPreferenceDto.*;
import com.cvibe.biz.notification.entity.Notification.NotificationCategory;
import com.cvibe.biz.notification.service.NotificationService;
import com.cvibe.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@Tag(name = "Notifications", description = "通知管理接口")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ================== Get Notifications ==================

    @GetMapping
    @Operation(summary = "获取通知列表", description = "获取当前用户的通知列表，支持分页和筛选")
    public ApiResponse<NotificationListResponse> getNotifications(
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "分类筛选") @RequestParam(required = false) NotificationCategory category,
            @Parameter(description = "仅未读") @RequestParam(required = false) Boolean unreadOnly) {
        
        Pageable pageable = PageRequest.of(page, size);
        NotificationListResponse response = notificationService.getNotifications(userId, pageable, category, unreadOnly);
        return ApiResponse.success(response);
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近通知", description = "获取最近的通知，用于快速预览")
    public ApiResponse<List<NotificationResponse>> getRecentNotifications(
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "5") int limit) {
        
        List<NotificationResponse> notifications = notificationService.getRecentNotifications(userId, limit);
        return ApiResponse.success(notifications);
    }

    @GetMapping("/high-priority")
    @Operation(summary = "获取高优先级未读通知", description = "获取所有未读的高优先级和紧急通知")
    public ApiResponse<List<NotificationResponse>> getHighPriorityUnread(
            @RequestHeader("X-User-Id") UUID userId) {
        
        List<NotificationResponse> notifications = notificationService.getHighPriorityUnread(userId);
        return ApiResponse.success(notifications);
    }

    @GetMapping("/unread/count")
    @Operation(summary = "获取未读数量", description = "获取未读通知数量统计")
    public ApiResponse<UnreadCountResponse> getUnreadCount(
            @RequestHeader("X-User-Id") UUID userId) {
        
        UnreadCountResponse response = notificationService.getUnreadCount(userId);
        return ApiResponse.success(response);
    }

    // ================== Mark as Read ==================

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "标记已读", description = "将单个通知标记为已读")
    public ApiResponse<Void> markAsRead(
            @PathVariable UUID notificationId) {
        
        notificationService.markAsRead(notificationId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{notificationId}/click")
    @Operation(summary = "记录点击", description = "记录通知被点击，同时标记为已读")
    public ApiResponse<Void> markAsClicked(
            @PathVariable UUID notificationId) {
        
        notificationService.markAsClicked(notificationId);
        return ApiResponse.success(null);
    }

    @PostMapping("/read/all")
    @Operation(summary = "全部标记已读", description = "将所有通知标记为已读")
    public ApiResponse<Integer> markAllAsRead(
            @RequestHeader("X-User-Id") UUID userId) {
        
        int count = notificationService.markAllAsRead(userId);
        return ApiResponse.success(count);
    }

    @PostMapping("/read/category/{category}")
    @Operation(summary = "分类标记已读", description = "将指定分类的通知全部标记为已读")
    public ApiResponse<Integer> markCategoryAsRead(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable NotificationCategory category) {
        
        int count = notificationService.markCategoryAsRead(userId, category);
        return ApiResponse.success(count);
    }

    @PostMapping("/read/batch")
    @Operation(summary = "批量标记已读", description = "批量标记指定通知为已读")
    public ApiResponse<Void> markMultipleAsRead(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody MarkReadRequest request) {
        
        notificationService.markMultipleAsRead(userId, request.getNotificationIds());
        return ApiResponse.success(null);
    }

    // ================== Dismiss & Delete ==================

    @PostMapping("/{notificationId}/dismiss")
    @Operation(summary = "忽略通知", description = "忽略单个通知")
    public ApiResponse<Void> dismiss(
            @PathVariable UUID notificationId) {
        
        notificationService.dismiss(notificationId);
        return ApiResponse.success(null);
    }

    @PostMapping("/dismiss/read")
    @Operation(summary = "忽略所有已读", description = "忽略所有已读通知")
    public ApiResponse<Integer> dismissAllRead(
            @RequestHeader("X-User-Id") UUID userId) {
        
        int count = notificationService.dismissAllRead(userId);
        return ApiResponse.success(count);
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "删除通知", description = "删除单个通知")
    public ApiResponse<Void> deleteNotification(
            @PathVariable UUID notificationId) {
        
        notificationService.deleteNotification(notificationId);
        return ApiResponse.success(null);
    }

    @DeleteMapping
    @Operation(summary = "删除所有通知", description = "删除当前用户的所有通知")
    public ApiResponse<Integer> deleteAllNotifications(
            @RequestHeader("X-User-Id") UUID userId) {
        
        int count = notificationService.deleteAllNotifications(userId);
        return ApiResponse.success(count);
    }

    @DeleteMapping("/old")
    @Operation(summary = "删除旧通知", description = "删除指定天数前的已读通知")
    public ApiResponse<Integer> deleteOldNotifications(
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "天数") @RequestParam(defaultValue = "30") int days) {
        
        int count = notificationService.deleteOldNotifications(userId, days);
        return ApiResponse.success(count);
    }

    // ================== Preferences ==================

    @GetMapping("/preferences")
    @Operation(summary = "获取通知偏好设置", description = "获取当前用户的通知偏好设置")
    public ApiResponse<PreferenceResponse> getPreferences(
            @RequestHeader("X-User-Id") UUID userId) {
        
        PreferenceResponse response = notificationService.getPreferences(userId);
        return ApiResponse.success(response);
    }

    @PutMapping("/preferences")
    @Operation(summary = "更新通知偏好设置", description = "更新当前用户的通知偏好设置")
    public ApiResponse<PreferenceResponse> updatePreferences(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody UpdatePreferenceRequest request) {
        
        PreferenceResponse response = notificationService.updatePreferences(userId, request);
        return ApiResponse.success(response);
    }

    @PutMapping("/preferences/quick-toggle")
    @Operation(summary = "快速开关", description = "快速开启/关闭某个通知渠道")
    public ApiResponse<PreferenceResponse> quickToggle(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody QuickToggleRequest request) {
        
        UpdatePreferenceRequest updateRequest = new UpdatePreferenceRequest();
        switch (request.getChannel()) {
            case "in_app" -> updateRequest.setInAppEnabled(request.getEnabled());
            case "email" -> updateRequest.setEmailEnabled(request.getEnabled());
            case "push" -> updateRequest.setPushEnabled(request.getEnabled());
        }
        
        PreferenceResponse response = notificationService.updatePreferences(userId, updateRequest);
        return ApiResponse.success(response);
    }

    // ================== Admin APIs ==================

    @PostMapping("/send")
    @Operation(summary = "发送通知（管理员）", description = "发送单个通知给指定用户")
    public ApiResponse<NotificationResponse> sendNotification(
            @RequestBody CreateNotificationRequest request) {
        
        NotificationResponse response = notificationService.sendNotification(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/send/bulk")
    @Operation(summary = "批量发送通知（管理员）", description = "批量发送通知给多个用户")
    public ApiResponse<Integer> sendBulkNotification(
            @RequestBody BulkNotificationRequest request) {
        
        int sent = notificationService.sendBulkNotification(request);
        return ApiResponse.success(sent);
    }
}
