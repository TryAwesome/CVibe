package com.cvibe.biz.admin.repository;

import com.cvibe.biz.admin.entity.Announcement;
import com.cvibe.biz.admin.entity.Announcement.*;
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
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    // ================== Basic Queries ==================

    Page<Announcement> findByStatusOrderByCreatedAtDesc(AnnouncementStatus status, Pageable pageable);

    Page<Announcement> findByAnnouncementTypeOrderByCreatedAtDesc(AnnouncementType type, Pageable pageable);

    Page<Announcement> findByCreatedByIdOrderByCreatedAtDesc(UUID createdById, Pageable pageable);

    // ================== Active Announcements ==================

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.status = 'ACTIVE'
            AND (a.startTime IS NULL OR a.startTime <= :now)
            AND (a.endTime IS NULL OR a.endTime > :now)
            AND (a.targetAudience = 'ALL' OR a.targetAudience = :audience)
            ORDER BY a.isPinned DESC, a.priority DESC, a.createdAt DESC
            """)
    List<Announcement> findActiveForAudience(
            @Param("now") Instant now,
            @Param("audience") TargetAudience audience);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.status = 'ACTIVE'
            AND (a.startTime IS NULL OR a.startTime <= :now)
            AND (a.endTime IS NULL OR a.endTime > :now)
            ORDER BY a.isPinned DESC, a.priority DESC, a.createdAt DESC
            """)
    List<Announcement> findAllActive(@Param("now") Instant now);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.status = 'ACTIVE'
            AND a.isPinned = true
            AND (a.startTime IS NULL OR a.startTime <= :now)
            AND (a.endTime IS NULL OR a.endTime > :now)
            ORDER BY a.priority DESC, a.createdAt DESC
            """)
    List<Announcement> findPinnedActive(@Param("now") Instant now);

    // ================== Status Management ==================

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.status = 'SCHEDULED'
            AND a.startTime <= :now
            """)
    List<Announcement> findScheduledToActivate(@Param("now") Instant now);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.status = 'ACTIVE'
            AND a.endTime IS NOT NULL
            AND a.endTime <= :now
            """)
    List<Announcement> findActiveToExpire(@Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE Announcement a
            SET a.status = 'ACTIVE'
            WHERE a.status = 'SCHEDULED'
            AND a.startTime <= :now
            """)
    int activateScheduled(@Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE Announcement a
            SET a.status = 'EXPIRED'
            WHERE a.status = 'ACTIVE'
            AND a.endTime IS NOT NULL
            AND a.endTime <= :now
            """)
    int expireOld(@Param("now") Instant now);

    // ================== Search ==================

    @Query("""
            SELECT a FROM Announcement a
            WHERE (:status IS NULL OR a.status = :status)
            AND (:type IS NULL OR a.announcementType = :type)
            AND (:priority IS NULL OR a.priority = :priority)
            AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY a.createdAt DESC
            """)
    Page<Announcement> search(
            @Param("status") AnnouncementStatus status,
            @Param("type") AnnouncementType type,
            @Param("priority") Priority priority,
            @Param("keyword") String keyword,
            Pageable pageable);

    // ================== Statistics ==================

    @Query("""
            SELECT a.status, COUNT(a) FROM Announcement a
            GROUP BY a.status
            """)
    List<Object[]> countByStatus();

    @Query("""
            SELECT a.announcementType, COUNT(a) FROM Announcement a
            WHERE a.createdAt >= :since
            GROUP BY a.announcementType
            """)
    List<Object[]> countByTypeSince(@Param("since") Instant since);

    @Query("""
            SELECT SUM(a.viewCount), SUM(a.dismissCount) FROM Announcement a
            WHERE a.status = 'ACTIVE'
            """)
    List<Object[]> getTotalEngagement();

    long countByStatus(AnnouncementStatus status);

    // ================== Type Specific ==================

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.announcementType = 'MAINTENANCE'
            AND a.status IN ('ACTIVE', 'SCHEDULED')
            AND (a.endTime IS NULL OR a.endTime > :now)
            ORDER BY a.startTime
            """)
    List<Announcement> findUpcomingMaintenance(@Param("now") Instant now);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.announcementType = 'CRITICAL'
            AND a.status = 'ACTIVE'
            AND (a.endTime IS NULL OR a.endTime > :now)
            """)
    List<Announcement> findCriticalAlerts(@Param("now") Instant now);
}
