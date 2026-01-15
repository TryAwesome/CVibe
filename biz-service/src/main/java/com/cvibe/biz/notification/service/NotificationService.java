package com.cvibe.biz.notification.service;

import com.cvibe.biz.notification.dto.NotificationDto.*;
import com.cvibe.biz.notification.dto.NotificationPreferenceDto.*;
import com.cvibe.biz.notification.entity.*;
import com.cvibe.biz.notification.entity.Notification.*;
import com.cvibe.biz.notification.entity.NotificationPreference.*;
import com.cvibe.biz.notification.repository.*;
import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    // ================== Send Notifications ==================

    @Transactional
    public NotificationResponse sendNotification(CreateNotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User sender = null;
        if (request.getSenderId() != null) {
            sender = userRepository.findById(request.getSenderId()).orElse(null);
        }

        // Check user preferences
        NotificationPreference prefs = getOrCreatePreference(user);
        NotificationCategory category = request.getCategory() != null ? request.getCategory() : NotificationCategory.GENERAL;

        if (!prefs.shouldSendInApp(category)) {
            log.debug("User {} has disabled in-app notifications for category {}", user.getId(), category);
            return null;
        }

        Notification notification = Notification.builder()
                .user(user)
                .notificationType(request.getType())
                .category(category)
                .title(request.getTitle())
                .content(request.getContent())
                .priority(request.getPriority() != null ? request.getPriority() : Priority.NORMAL)
                .actionUrl(request.getActionUrl())
                .actionText(request.getActionText())
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .sender(sender)
                .imageUrl(request.getImageUrl())
                .metadata(request.getMetadata())
                .channel(request.getChannel() != null ? request.getChannel() : DeliveryChannel.IN_APP)
                .expiresAt(request.getExpiresAt())
                .build();

        Notification saved = notificationRepository.save(notification);
        return toNotificationResponse(saved);
    }

    @Transactional
    public void sendWelcomeNotification(User user) {
        Notification notification = Notification.welcome(user);
        notificationRepository.save(notification);
    }

    @Transactional
    public void sendSystemNotification(User user, String title, String content) {
        Notification notification = Notification.system(user, title, content);
        notificationRepository.save(notification);
    }

    @Transactional
    public void sendSocialNotification(User user, User sender, NotificationType type, 
                                       String title, String content, String entityType, UUID entityId) {
        NotificationPreference prefs = getOrCreatePreference(user);
        if (!prefs.shouldSendInApp(NotificationCategory.COMMUNITY)) {
            return;
        }

        Notification notification = Notification.social(user, sender, type, title, content, entityType, entityId);
        notificationRepository.save(notification);
    }

    @Transactional
    public void sendAchievementNotification(User user, String title, String content, String actionUrl) {
        NotificationPreference prefs = getOrCreatePreference(user);
        if (!prefs.shouldSendInApp(NotificationCategory.GROWTH)) {
            return;
        }

        Notification notification = Notification.achievement(user, title, content, actionUrl);
        notificationRepository.save(notification);
    }

    @Transactional
    public int sendBulkNotification(BulkNotificationRequest request) {
        int sent = 0;
        for (UUID userId : request.getUserIds()) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) continue;

                Notification notification = Notification.builder()
                        .user(user)
                        .notificationType(request.getType())
                        .category(request.getCategory())
                        .title(request.getTitle())
                        .content(request.getContent())
                        .priority(request.getPriority() != null ? request.getPriority() : Priority.NORMAL)
                        .actionUrl(request.getActionUrl())
                        .actionText(request.getActionText())
                        .channel(request.getChannel() != null ? request.getChannel() : DeliveryChannel.IN_APP)
                        .build();

                notificationRepository.save(notification);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send notification to user {}: {}", userId, e.getMessage());
            }
        }
        return sent;
    }

    // ================== Get Notifications ==================

    public NotificationListResponse getNotifications(UUID userId, Pageable pageable, 
                                                     NotificationCategory category, Boolean unreadOnly) {
        Page<Notification> page;
        if (category != null) {
            page = notificationRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(userId, category, pageable);
        } else if (Boolean.TRUE.equals(unreadOnly)) {
            page = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, pageable);
        } else {
            page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        List<NotificationResponse> notifications = page.getContent().stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        Map<String, Long> unreadByCategory = getUnreadCountByCategory(userId);

        return NotificationListResponse.builder()
                .notifications(notifications)
                .unreadCount(unreadCount)
                .unreadByCategory(unreadByCategory)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public UnreadCountResponse getUnreadCount(UUID userId) {
        long total = notificationRepository.countByUserIdAndIsReadFalse(userId);
        Map<String, Long> byCategory = getUnreadCountByCategory(userId);
        long highPriority = notificationRepository.findHighPriorityUnread(userId).size();

        return UnreadCountResponse.builder()
                .total(total)
                .byCategory(byCategory)
                .highPriority(highPriority)
                .build();
    }

    private Map<String, Long> getUnreadCountByCategory(UUID userId) {
        List<Object[]> data = notificationRepository.countUnreadByCategory(userId);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : data) {
            result.put(row[0].toString(), (Long) row[1]);
        }
        return result;
    }

    public List<NotificationResponse> getRecentNotifications(UUID userId, int limit) {
        List<Notification> notifications = notificationRepository
                .findRecentForUser(userId, Instant.now(), PageRequest.of(0, limit));
        return notifications.stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getHighPriorityUnread(UUID userId) {
        List<Notification> notifications = notificationRepository.findHighPriorityUnread(userId);
        return notifications.stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());
    }

    // ================== Mark as Read ==================

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.markAsRead(notificationId, Instant.now());
    }

    @Transactional
    public void markAsClicked(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null) {
            notification.markAsClicked();
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsRead(userId, Instant.now());
    }

    @Transactional
    public int markCategoryAsRead(UUID userId, NotificationCategory category) {
        return notificationRepository.markCategoryAsRead(userId, category, Instant.now());
    }

    @Transactional
    public void markMultipleAsRead(UUID userId, List<UUID> notificationIds) {
        Instant now = Instant.now();
        for (UUID id : notificationIds) {
            notificationRepository.markAsRead(id, now);
        }
    }

    // ================== Dismiss ==================

    @Transactional
    public void dismiss(UUID notificationId) {
        notificationRepository.dismiss(notificationId);
    }

    @Transactional
    public int dismissAllRead(UUID userId) {
        return notificationRepository.dismissAllRead(userId);
    }

    // ================== Delete ==================

    @Transactional
    public void deleteNotification(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Transactional
    public int deleteAllNotifications(UUID userId) {
        return notificationRepository.deleteAllByUserId(userId);
    }

    @Transactional
    public int deleteOldNotifications(UUID userId, int daysOld) {
        Instant before = Instant.now().minus(daysOld, ChronoUnit.DAYS);
        return notificationRepository.deleteOldRead(userId, before);
    }

    // ================== Preferences ==================

    public PreferenceResponse getPreferences(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        NotificationPreference prefs = getOrCreatePreference(user);
        return toPreferenceResponse(prefs);
    }

    @Transactional
    public PreferenceResponse updatePreferences(UUID userId, UpdatePreferenceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        NotificationPreference prefs = getOrCreatePreference(user);

        // Update global toggles
        if (request.getInAppEnabled() != null) prefs.setInAppEnabled(request.getInAppEnabled());
        if (request.getEmailEnabled() != null) prefs.setEmailEnabled(request.getEmailEnabled());
        if (request.getPushEnabled() != null) prefs.setPushEnabled(request.getPushEnabled());

        // Update in-app categories
        if (request.getInAppSystem() != null) prefs.setInAppSystem(request.getInAppSystem());
        if (request.getInAppAccount() != null) prefs.setInAppAccount(request.getInAppAccount());
        if (request.getInAppResume() != null) prefs.setInAppResume(request.getInAppResume());
        if (request.getInAppInterview() != null) prefs.setInAppInterview(request.getInAppInterview());
        if (request.getInAppJob() != null) prefs.setInAppJob(request.getInAppJob());
        if (request.getInAppCommunity() != null) prefs.setInAppCommunity(request.getInAppCommunity());
        if (request.getInAppGrowth() != null) prefs.setInAppGrowth(request.getInAppGrowth());

        // Update email categories
        if (request.getEmailSystem() != null) prefs.setEmailSystem(request.getEmailSystem());
        if (request.getEmailAccount() != null) prefs.setEmailAccount(request.getEmailAccount());
        if (request.getEmailResume() != null) prefs.setEmailResume(request.getEmailResume());
        if (request.getEmailInterview() != null) prefs.setEmailInterview(request.getEmailInterview());
        if (request.getEmailJob() != null) prefs.setEmailJob(request.getEmailJob());
        if (request.getEmailCommunity() != null) prefs.setEmailCommunity(request.getEmailCommunity());
        if (request.getEmailGrowth() != null) prefs.setEmailGrowth(request.getEmailGrowth());
        if (request.getEmailMarketing() != null) prefs.setEmailMarketing(request.getEmailMarketing());
        if (request.getEmailWeeklyDigest() != null) prefs.setEmailWeeklyDigest(request.getEmailWeeklyDigest());

        // Update push categories
        if (request.getPushSystem() != null) prefs.setPushSystem(request.getPushSystem());
        if (request.getPushAccount() != null) prefs.setPushAccount(request.getPushAccount());
        if (request.getPushResume() != null) prefs.setPushResume(request.getPushResume());
        if (request.getPushInterview() != null) prefs.setPushInterview(request.getPushInterview());
        if (request.getPushJob() != null) prefs.setPushJob(request.getPushJob());
        if (request.getPushCommunity() != null) prefs.setPushCommunity(request.getPushCommunity());
        if (request.getPushGrowth() != null) prefs.setPushGrowth(request.getPushGrowth());

        // Update quiet hours
        if (request.getQuietHoursEnabled() != null) prefs.setQuietHoursEnabled(request.getQuietHoursEnabled());
        if (request.getQuietHoursStart() != null) prefs.setQuietHoursStart(request.getQuietHoursStart());
        if (request.getQuietHoursEnd() != null) prefs.setQuietHoursEnd(request.getQuietHoursEnd());
        if (request.getTimezone() != null) prefs.setTimezone(request.getTimezone());

        // Update frequency
        if (request.getEmailFrequency() != null) prefs.setEmailFrequency(request.getEmailFrequency());
        if (request.getDigestFrequency() != null) prefs.setDigestFrequency(request.getDigestFrequency());

        NotificationPreference saved = preferenceRepository.save(prefs);
        return toPreferenceResponse(saved);
    }

    @Transactional
    public NotificationPreference getOrCreatePreference(User user) {
        return preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    NotificationPreference prefs = NotificationPreference.defaultPreferences(user);
                    return preferenceRepository.save(prefs);
                });
    }

    // ================== Scheduled Tasks ==================

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void cleanupExpiredNotifications() {
        int deleted = notificationRepository.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.info("Deleted {} expired notifications", deleted);
        }
    }

    // ================== Helpers ==================

    private NotificationResponse toNotificationResponse(Notification n) {
        SenderInfo sender = null;
        if (n.getSender() != null) {
            sender = SenderInfo.builder()
                    .id(n.getSender().getId())
                    .name(n.getSender().getFullName())
                    .avatarUrl(n.getSender().getAvatarUrl())
                    .build();
        }

        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getNotificationType())
                .category(n.getCategory())
                .title(n.getTitle())
                .content(n.getContent())
                .priority(n.getPriority())
                .actionUrl(n.getActionUrl())
                .actionText(n.getActionText())
                .entityType(n.getEntityType())
                .entityId(n.getEntityId())
                .sender(sender)
                .imageUrl(n.getImageUrl())
                .isRead(n.getIsRead())
                .readAt(n.getReadAt())
                .isClicked(n.getIsClicked())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private PreferenceResponse toPreferenceResponse(NotificationPreference p) {
        return PreferenceResponse.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .inAppEnabled(p.getInAppEnabled())
                .inApp(InAppPreferences.builder()
                        .system(p.getInAppSystem())
                        .account(p.getInAppAccount())
                        .resume(p.getInAppResume())
                        .interview(p.getInAppInterview())
                        .job(p.getInAppJob())
                        .community(p.getInAppCommunity())
                        .growth(p.getInAppGrowth())
                        .build())
                .emailEnabled(p.getEmailEnabled())
                .email(EmailPreferences.builder()
                        .system(p.getEmailSystem())
                        .account(p.getEmailAccount())
                        .resume(p.getEmailResume())
                        .interview(p.getEmailInterview())
                        .job(p.getEmailJob())
                        .community(p.getEmailCommunity())
                        .growth(p.getEmailGrowth())
                        .marketing(p.getEmailMarketing())
                        .weeklyDigest(p.getEmailWeeklyDigest())
                        .build())
                .pushEnabled(p.getPushEnabled())
                .push(PushPreferences.builder()
                        .system(p.getPushSystem())
                        .account(p.getPushAccount())
                        .resume(p.getPushResume())
                        .interview(p.getPushInterview())
                        .job(p.getPushJob())
                        .community(p.getPushCommunity())
                        .growth(p.getPushGrowth())
                        .build())
                .quietHours(QuietHoursSettings.builder()
                        .enabled(p.getQuietHoursEnabled())
                        .startTime(p.getQuietHoursStart())
                        .endTime(p.getQuietHoursEnd())
                        .timezone(p.getTimezone())
                        .build())
                .emailFrequency(p.getEmailFrequency())
                .digestFrequency(p.getDigestFrequency())
                .build();
    }
}
