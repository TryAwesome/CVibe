package com.cvibe.biz.notification.repository;

import com.cvibe.biz.notification.entity.Notification;
import com.cvibe.biz.notification.entity.Notification.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // ================== Basic Queries ==================

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(UUID userId, Boolean isRead, Pageable pageable);

    Page<Notification> findByUserIdAndCategoryOrderByCreatedAtDesc(UUID userId, NotificationCategory category, Pageable pageable);

    Page<Notification> findByUserIdAndNotificationTypeOrderByCreatedAtDesc(UUID userId, NotificationType type, Pageable pageable);

    List<Notification> findByUserIdAndIsReadFalseAndIsDismissedFalseOrderByCreatedAtDesc(UUID userId);

    // ================== Count Queries ==================

    long countByUserIdAndIsReadFalse(UUID userId);

    long countByUserIdAndIsReadFalseAndCategory(UUID userId, NotificationCategory category);

    long countByUserId(UUID userId);

    @Query("SELECT n.category, COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false GROUP BY n.category")
    List<Object[]> countUnreadByCategory(@Param("userId") UUID userId);

    // ================== Mark as Read ==================

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.id = :id AND n.isRead = false")
    int markAsRead(@Param("id") UUID id, @Param("readAt") Instant readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") UUID userId, @Param("readAt") Instant readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.user.id = :userId AND n.category = :category AND n.isRead = false")
    int markCategoryAsRead(@Param("userId") UUID userId, @Param("category") NotificationCategory category, @Param("readAt") Instant readAt);

    // ================== Dismiss ==================

    @Modifying
    @Query("UPDATE Notification n SET n.isDismissed = true WHERE n.id = :id")
    int dismiss(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Notification n SET n.isDismissed = true WHERE n.user.id = :userId AND n.isRead = true")
    int dismissAllRead(@Param("userId") UUID userId);

    // ================== Delete ==================

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId AND n.isRead = true AND n.createdAt < :before")
    int deleteOldRead(@Param("userId") UUID userId, @Param("before") Instant before);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);

    // ================== Recent Notifications ==================

    @Query("""
            SELECT n FROM Notification n
            WHERE n.user.id = :userId
            AND n.isDismissed = false
            AND (n.expiresAt IS NULL OR n.expiresAt > :now)
            ORDER BY n.isRead ASC, n.priority DESC, n.createdAt DESC
            """)
    List<Notification> findRecentForUser(@Param("userId") UUID userId, @Param("now") Instant now, Pageable pageable);

    // ================== Priority Notifications ==================

    @Query("""
            SELECT n FROM Notification n
            WHERE n.user.id = :userId
            AND n.isRead = false
            AND n.priority IN ('HIGH', 'URGENT')
            AND n.isDismissed = false
            ORDER BY n.priority DESC, n.createdAt DESC
            """)
    List<Notification> findHighPriorityUnread(@Param("userId") UUID userId);

    // ================== Statistics ==================

    @Query("SELECT DATE(n.createdAt), COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.createdAt >= :since GROUP BY DATE(n.createdAt)")
    List<Object[]> countByDaySince(@Param("userId") UUID userId, @Param("since") Instant since);

    @Query("SELECT n.notificationType, COUNT(n) FROM Notification n WHERE n.user.id = :userId GROUP BY n.notificationType")
    List<Object[]> countByType(@Param("userId") UUID userId);

    // ================== Email Queue ==================

    @Query("""
            SELECT n FROM Notification n
            WHERE n.emailSent = false
            AND n.channel IN ('EMAIL', 'ALL')
            AND n.createdAt >= :since
            ORDER BY n.priority DESC, n.createdAt ASC
            """)
    List<Notification> findPendingEmails(@Param("since") Instant since, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.emailSent = true, n.emailSentAt = :sentAt WHERE n.id = :id")
    int markEmailSent(@Param("id") UUID id, @Param("sentAt") Instant sentAt);

    // ================== Push Queue ==================

    @Query("""
            SELECT n FROM Notification n
            WHERE n.pushSent = false
            AND n.channel IN ('PUSH', 'ALL')
            AND n.createdAt >= :since
            ORDER BY n.priority DESC, n.createdAt ASC
            """)
    List<Notification> findPendingPush(@Param("since") Instant since, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.pushSent = true, n.pushSentAt = :sentAt WHERE n.id = :id")
    int markPushSent(@Param("id") UUID id, @Param("sentAt") Instant sentAt);

    // ================== Find by Entity ==================

    List<Notification> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.entityType = :entityType AND n.entityId = :entityId")
    List<Notification> findByUserAndEntity(@Param("userId") UUID userId, @Param("entityType") String entityType, @Param("entityId") UUID entityId);
}
