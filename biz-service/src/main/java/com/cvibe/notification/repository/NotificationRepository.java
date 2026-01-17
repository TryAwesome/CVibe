package com.cvibe.notification.repository;

import com.cvibe.notification.entity.Notification;
import com.cvibe.notification.entity.NotificationPriority;
import com.cvibe.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for notifications
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find all notifications by user ID with pagination
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find notifications by user ID and type
     */
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, NotificationType type, Pageable pageable);

    /**
     * Find notifications by user ID and category
     */
    Page<Notification> findByUserIdAndCategoryOrderByCreatedAtDesc(UUID userId, String category, Pageable pageable);

    /**
     * Find unread notifications by user ID
     */
    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(UUID userId, Boolean isRead, Pageable pageable);

    /**
     * Find notification by ID and user ID (for ownership verification)
     */
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Count notifications by user ID and read status
     */
    int countByUserIdAndIsRead(UUID userId, Boolean isRead);

    /**
     * Count notifications by user ID, category, and read status
     */
    int countByUserIdAndCategoryAndIsRead(UUID userId, String category, Boolean isRead);

    /**
     * Count notifications by user ID, priority, and read status
     */
    int countByUserIdAndPriorityAndIsRead(UUID userId, NotificationPriority priority, Boolean isRead);

    /**
     * Mark all notifications as read for a user
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") UUID userId);

    /**
     * Get recent notifications for a user (top N)
     */
    List<Notification> findTop5ByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Get recent notifications with custom limit
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Get distinct categories for a user's unread notifications
     */
    @Query("SELECT DISTINCT n.category FROM Notification n WHERE n.user.id = :userId AND n.isRead = false AND n.category IS NOT NULL")
    List<String> findDistinctCategoriesByUserIdAndUnread(@Param("userId") UUID userId);

    /**
     * Delete all notifications for a user
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
