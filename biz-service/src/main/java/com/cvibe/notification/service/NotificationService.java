package com.cvibe.notification.service;

import com.cvibe.auth.entity.User;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import com.cvibe.notification.dto.*;
import com.cvibe.notification.entity.Notification;
import com.cvibe.notification.entity.NotificationPriority;
import com.cvibe.notification.entity.NotificationType;
import com.cvibe.notification.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing notifications
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get paginated notifications for a user
     */
    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(UUID userId, int page, int size, 
                                                      String category, Boolean unreadOnly) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage;

        if (category != null && !category.isEmpty()) {
            notificationPage = notificationRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(
                    userId, category, pageable);
        } else if (Boolean.TRUE.equals(unreadOnly)) {
            notificationPage = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(
                    userId, false, pageable);
        } else {
            notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        List<NotificationDto> notifications = notificationPage.getContent()
                .stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());

        UnreadCountDto unreadCount = getUnreadCount(userId);

        return NotificationListResponse.builder()
                .notifications(notifications)
                .unreadCount(unreadCount)
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .last(notificationPage.isLast())
                .build();
    }

    /**
     * Get recent notifications for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getRecentNotifications(UUID userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findRecentByUserId(userId, pageable)
                .stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notification count with category breakdown
     */
    @Transactional(readOnly = true)
    public UnreadCountDto getUnreadCount(UUID userId) {
        int total = notificationRepository.countByUserIdAndIsRead(userId, false);
        int highPriority = notificationRepository.countByUserIdAndPriorityAndIsRead(
                userId, NotificationPriority.HIGH, false);

        // Get category breakdown
        Map<String, Integer> byCategory = new HashMap<>();
        List<String> categories = notificationRepository.findDistinctCategoriesByUserIdAndUnread(userId);
        for (String category : categories) {
            int count = notificationRepository.countByUserIdAndCategoryAndIsRead(userId, category, false);
            byCategory.put(category, count);
        }

        return UnreadCountDto.builder()
                .total(total)
                .byCategory(byCategory)
                .highPriority(highPriority)
                .build();
    }

    /**
     * Mark a notification as read
     */
    @Transactional
    public NotificationDto markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.setIsRead(true);
        notification = notificationRepository.save(notification);

        log.info("Marked notification {} as read for user {}", notificationId, userId);
        return NotificationDto.fromEntity(notification);
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public int markAllAsRead(UUID userId) {
        int count = notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked {} notifications as read for user {}", count, userId);
        return count;
    }

    /**
     * Delete a notification
     */
    @Transactional
    public void deleteNotification(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notificationRepository.delete(notification);
        log.info("Deleted notification {} for user {}", notificationId, userId);
    }

    // ==================== Helper Methods to Create Notifications ====================

    /**
     * Send a system notification
     */
    @Transactional
    public Notification sendSystemNotification(UUID userId, String title, String content) {
        return createNotification(userId, NotificationType.SYSTEM, NotificationPriority.NORMAL,
                "system", title, content, null, null, null);
    }

    /**
     * Send a job match notification
     */
    @Transactional
    public Notification sendJobMatchNotification(UUID userId, String jobTitle, String company, 
                                                  String actionUrl, Map<String, Object> jobData) {
        String title = "New Job Match: " + jobTitle;
        String content = String.format("We found a job that matches your profile: %s at %s", jobTitle, company);
        String dataJson = toJson(jobData);

        return createNotification(userId, NotificationType.JOB_MATCH, NotificationPriority.NORMAL,
                "jobs", title, content, actionUrl, "View Job", dataJson);
    }

    /**
     * Send an interview reminder notification
     */
    @Transactional
    public Notification sendInterviewReminder(UUID userId, String interviewTitle, String scheduledTime,
                                               String actionUrl) {
        String title = "Interview Reminder: " + interviewTitle;
        String content = String.format("Your interview '%s' is scheduled for %s. Don't forget to prepare!",
                interviewTitle, scheduledTime);

        return createNotification(userId, NotificationType.INTERVIEW_REMINDER, NotificationPriority.HIGH,
                "interview", title, content, actionUrl, "Prepare Now", null);
    }

    /**
     * Send a growth tip notification
     */
    @Transactional
    public Notification sendGrowthTip(UUID userId, String tipTitle, String tipContent, String actionUrl) {
        return createNotification(userId, NotificationType.GROWTH_TIP, NotificationPriority.LOW,
                "growth", tipTitle, tipContent, actionUrl, "Learn More", null);
    }

    /**
     * Send a community notification
     */
    @Transactional
    public Notification sendCommunityNotification(UUID userId, String title, String content,
                                                   String actionUrl, Map<String, Object> data) {
        String dataJson = toJson(data);
        return createNotification(userId, NotificationType.COMMUNITY, NotificationPriority.NORMAL,
                "community", title, content, actionUrl, "View", dataJson);
    }

    /**
     * Create a notification
     */
    @Transactional
    public Notification createNotification(UUID userId, NotificationType type, NotificationPriority priority,
                                            String category, String title, String content,
                                            String actionUrl, String actionText, String dataJson) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .priority(priority)
                .category(category)
                .title(title)
                .content(content)
                .actionUrl(actionUrl)
                .actionText(actionText)
                .dataJson(dataJson)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Created {} notification {} for user {}", type, notification.getId(), userId);

        return notification;
    }

    /**
     * Convert object to JSON string
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON", e);
            return null;
        }
    }
}
