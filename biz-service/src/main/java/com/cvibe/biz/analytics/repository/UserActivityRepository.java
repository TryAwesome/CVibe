package com.cvibe.biz.analytics.repository;

import com.cvibe.biz.analytics.entity.UserActivity;
import com.cvibe.biz.analytics.entity.UserActivity.ActivityCategory;
import com.cvibe.biz.analytics.entity.UserActivity.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, UUID> {

    // ================== Basic Queries ==================

    Page<UserActivity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<UserActivity> findByActivityTypeOrderByCreatedAtDesc(ActivityType type, Pageable pageable);

    Page<UserActivity> findByCategoryOrderByCreatedAtDesc(ActivityCategory category, Pageable pageable);

    List<UserActivity> findByUserIdAndCreatedAtBetween(UUID userId, Instant start, Instant end);

    // ================== Activity Count ==================

    long countByUserId(UUID userId);

    long countByUserIdAndActivityType(UUID userId, ActivityType type);

    long countByActivityTypeAndCreatedAtBetween(ActivityType type, Instant start, Instant end);

    long countByCategoryAndCreatedAtBetween(ActivityCategory category, Instant start, Instant end);

    // ================== User Activity Analysis ==================

    @Query("""
            SELECT ua.activityType, COUNT(ua) FROM UserActivity ua
            WHERE ua.user.id = :userId
            AND ua.createdAt BETWEEN :start AND :end
            GROUP BY ua.activityType
            ORDER BY COUNT(ua) DESC
            """)
    List<Object[]> countByUserAndTypeInRange(
            @Param("userId") UUID userId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT ua.category, COUNT(ua) FROM UserActivity ua
            WHERE ua.user.id = :userId
            AND ua.createdAt BETWEEN :start AND :end
            GROUP BY ua.category
            ORDER BY COUNT(ua) DESC
            """)
    List<Object[]> countByUserAndCategoryInRange(
            @Param("userId") UUID userId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT FUNCTION('DATE', ua.createdAt), COUNT(ua) FROM UserActivity ua
            WHERE ua.user.id = :userId
            AND ua.createdAt BETWEEN :start AND :end
            GROUP BY FUNCTION('DATE', ua.createdAt)
            ORDER BY FUNCTION('DATE', ua.createdAt)
            """)
    List<Object[]> countByUserAndDayInRange(
            @Param("userId") UUID userId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    // ================== Platform Analytics ==================

    @Query("""
            SELECT ua.activityType, COUNT(ua) FROM UserActivity ua
            WHERE ua.createdAt BETWEEN :start AND :end
            GROUP BY ua.activityType
            ORDER BY COUNT(ua) DESC
            """)
    List<Object[]> countByTypeInRange(
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT FUNCTION('DATE', ua.createdAt), COUNT(ua) FROM UserActivity ua
            WHERE ua.createdAt BETWEEN :start AND :end
            GROUP BY FUNCTION('DATE', ua.createdAt)
            ORDER BY FUNCTION('DATE', ua.createdAt)
            """)
    List<Object[]> countByDayInRange(
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT FUNCTION('HOUR', ua.createdAt), COUNT(ua) FROM UserActivity ua
            WHERE ua.createdAt BETWEEN :start AND :end
            GROUP BY FUNCTION('HOUR', ua.createdAt)
            ORDER BY FUNCTION('HOUR', ua.createdAt)
            """)
    List<Object[]> countByHourInRange(
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT ua.deviceType, COUNT(ua) FROM UserActivity ua
            WHERE ua.createdAt BETWEEN :start AND :end
            AND ua.deviceType IS NOT NULL
            GROUP BY ua.deviceType
            """)
    List<Object[]> countByDeviceInRange(
            @Param("start") Instant start,
            @Param("end") Instant end);

    // ================== Active Users ==================

    @Query("""
            SELECT COUNT(DISTINCT ua.user.id) FROM UserActivity ua
            WHERE ua.createdAt BETWEEN :start AND :end
            """)
    long countDistinctUsersInRange(
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT COUNT(DISTINCT ua.user.id) FROM UserActivity ua
            WHERE ua.activityType = :type
            AND ua.createdAt BETWEEN :start AND :end
            """)
    long countDistinctUsersByTypeInRange(
            @Param("type") ActivityType type,
            @Param("start") Instant start,
            @Param("end") Instant end);

    // ================== Top Users ==================

    @Query("""
            SELECT ua.user.id, ua.user.fullName, COUNT(ua) FROM UserActivity ua
            WHERE ua.createdAt BETWEEN :start AND :end
            GROUP BY ua.user.id, ua.user.fullName
            ORDER BY COUNT(ua) DESC
            """)
    List<Object[]> findMostActiveUsers(
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);

    // ================== Session Analysis ==================

    @Query("""
            SELECT COUNT(DISTINCT ua.sessionId) FROM UserActivity ua
            WHERE ua.user.id = :userId
            AND ua.createdAt BETWEEN :start AND :end
            AND ua.sessionId IS NOT NULL
            """)
    long countSessionsByUserInRange(
            @Param("userId") UUID userId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT AVG(ua.durationSeconds) FROM UserActivity ua
            WHERE ua.user.id = :userId
            AND ua.durationSeconds IS NOT NULL
            AND ua.createdAt BETWEEN :start AND :end
            """)
    Double avgSessionDurationByUserInRange(
            @Param("userId") UUID userId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    // ================== Feature Usage ==================

    @Query("""
            SELECT ua.entityType, COUNT(ua) FROM UserActivity ua
            WHERE ua.activityType = 'FEATURE_USE'
            AND ua.createdAt BETWEEN :start AND :end
            GROUP BY ua.entityType
            ORDER BY COUNT(ua) DESC
            """)
    List<Object[]> countFeatureUsageInRange(
            @Param("start") Instant start,
            @Param("end") Instant end);

    // ================== Page Views ==================

    @Query("""
            SELECT ua.pagePath, COUNT(ua) FROM UserActivity ua
            WHERE ua.activityType = 'PAGE_VIEW'
            AND ua.createdAt BETWEEN :start AND :end
            GROUP BY ua.pagePath
            ORDER BY COUNT(ua) DESC
            """)
    List<Object[]> countPageViewsInRange(
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);
}
