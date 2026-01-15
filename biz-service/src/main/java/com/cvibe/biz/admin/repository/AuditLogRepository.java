package com.cvibe.biz.admin.repository;

import com.cvibe.biz.admin.entity.AuditLog;
import com.cvibe.biz.admin.entity.AuditLog.AuditAction;
import com.cvibe.biz.admin.entity.AuditLog.AuditStatus;
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
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // ================== Basic Queries ==================

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);

    Page<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

    Page<AuditLog> findByStatusOrderByCreatedAtDesc(AuditStatus status, Pageable pageable);

    // ================== Time Range Queries ==================

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.createdAt BETWEEN :start AND :end
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findByTimeRange(
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.user.id = :userId
            AND a.createdAt BETWEEN :start AND :end
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findByUserIdAndTimeRange(
            @Param("userId") UUID userId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);

    // ================== Search Queries ==================

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:userId IS NULL OR a.user.id = :userId)
            AND (:action IS NULL OR a.action = :action)
            AND (:entityType IS NULL OR a.entityType = :entityType)
            AND (:status IS NULL OR a.status = :status)
            AND (:start IS NULL OR a.createdAt >= :start)
            AND (:end IS NULL OR a.createdAt <= :end)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> search(
            @Param("userId") UUID userId,
            @Param("action") AuditAction action,
            @Param("entityType") String entityType,
            @Param("status") AuditStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);

    // ================== Statistics ==================

    @Query("""
            SELECT a.action, COUNT(a) FROM AuditLog a
            WHERE a.createdAt >= :since
            GROUP BY a.action
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> countByActionSince(@Param("since") Instant since);

    @Query("""
            SELECT a.status, COUNT(a) FROM AuditLog a
            WHERE a.createdAt >= :since
            GROUP BY a.status
            """)
    List<Object[]> countByStatusSince(@Param("since") Instant since);

    @Query("""
            SELECT FUNCTION('DATE', a.createdAt), COUNT(a) FROM AuditLog a
            WHERE a.createdAt >= :since
            GROUP BY FUNCTION('DATE', a.createdAt)
            ORDER BY FUNCTION('DATE', a.createdAt)
            """)
    List<Object[]> countByDaySince(@Param("since") Instant since);

    long countByCreatedAtAfter(Instant since);

    long countByStatusAndCreatedAtAfter(AuditStatus status, Instant since);

    // ================== Security Related ==================

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.action IN :actions
            AND a.createdAt >= :since
            ORDER BY a.createdAt DESC
            """)
    List<AuditLog> findSecurityEvents(
            @Param("actions") List<AuditAction> actions,
            @Param("since") Instant since);

    @Query("""
            SELECT a.ipAddress, COUNT(a) FROM AuditLog a
            WHERE a.action = :action
            AND a.status = 'FAILURE'
            AND a.createdAt >= :since
            GROUP BY a.ipAddress
            HAVING COUNT(a) >= :threshold
            """)
    List<Object[]> findSuspiciousIPs(
            @Param("action") AuditAction action,
            @Param("since") Instant since,
            @Param("threshold") long threshold);

    // ================== Entity History ==================

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.entityType = :entityType
            AND a.entityId = :entityId
            ORDER BY a.createdAt DESC
            """)
    List<AuditLog> findEntityHistory(
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId);

    // ================== Cleanup ==================

    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :before")
    void deleteOlderThan(@Param("before") Instant before);
}
