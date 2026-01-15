package com.cvibe.biz.analytics.repository;

import com.cvibe.biz.analytics.entity.AnalyticsEvent;
import com.cvibe.biz.analytics.entity.AnalyticsEvent.EventSource;
import com.cvibe.biz.analytics.entity.AnalyticsEvent.EventType;
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
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    // ================== Basic Queries ==================

    Page<AnalyticsEvent> findByEventTypeOrderByCreatedAtDesc(EventType eventType, Pageable pageable);

    Page<AnalyticsEvent> findByEventNameOrderByCreatedAtDesc(String eventName, Pageable pageable);

    List<AnalyticsEvent> findByEventTypeAndCreatedAtBetween(EventType eventType, Instant start, Instant end);

    List<AnalyticsEvent> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // ================== Event Counts ==================

    long countByEventType(EventType eventType);

    long countByEventName(String eventName);

    long countByEventTypeAndCreatedAtBetween(EventType eventType, Instant start, Instant end);

    long countByEventNameAndCreatedAtBetween(String eventName, Instant start, Instant end);

    // ================== Aggregation Queries ==================

    @Query("""
            SELECT ae.eventName, COUNT(ae), SUM(ae.eventValue) FROM AnalyticsEvent ae
            WHERE ae.eventType = :eventType
            AND ae.createdAt BETWEEN :start AND :end
            GROUP BY ae.eventName
            ORDER BY COUNT(ae) DESC
            """)
    List<Object[]> aggregateByNameInRange(
            @Param("eventType") EventType eventType,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT FUNCTION('DATE', ae.createdAt), COUNT(ae), SUM(ae.eventValue) FROM AnalyticsEvent ae
            WHERE ae.eventType = :eventType
            AND ae.createdAt BETWEEN :start AND :end
            GROUP BY FUNCTION('DATE', ae.createdAt)
            ORDER BY FUNCTION('DATE', ae.createdAt)
            """)
    List<Object[]> aggregateByDayInRange(
            @Param("eventType") EventType eventType,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT ae.source, COUNT(ae) FROM AnalyticsEvent ae
            WHERE ae.createdAt BETWEEN :start AND :end
            GROUP BY ae.source
            """)
    List<Object[]> countBySourceInRange(
            @Param("start") Instant start,
            @Param("end") Instant end);

    // ================== Conversion Funnel ==================

    @Query("""
            SELECT ae.eventName, COUNT(DISTINCT ae.userId) FROM AnalyticsEvent ae
            WHERE ae.eventType = 'CONVERSION'
            AND ae.eventName IN :eventNames
            AND ae.createdAt BETWEEN :start AND :end
            GROUP BY ae.eventName
            """)
    List<Object[]> countConversionFunnel(
            @Param("eventNames") List<String> eventNames,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT ae.eventName, COUNT(DISTINCT ae.userId), SUM(ae.eventValue) FROM AnalyticsEvent ae
            WHERE ae.eventType = 'CONVERSION'
            AND ae.createdAt BETWEEN :start AND :end
            GROUP BY ae.eventName
            ORDER BY COUNT(DISTINCT ae.userId) DESC
            """)
    List<Object[]> aggregateConversionsInRange(
            @Param("start") Instant start,
            @Param("end") Instant end);

    // ================== Business Metrics ==================

    @Query("""
            SELECT ae.eventName, AVG(ae.eventValue), MIN(ae.eventValue), MAX(ae.eventValue) FROM AnalyticsEvent ae
            WHERE ae.eventType = 'BUSINESS'
            AND ae.createdAt BETWEEN :start AND :end
            GROUP BY ae.eventName
            """)
    List<Object[]> aggregateBusinessMetricsInRange(
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT SUM(ae.eventValue) FROM AnalyticsEvent ae
            WHERE ae.eventName = :eventName
            AND ae.createdAt BETWEEN :start AND :end
            """)
    Double sumEventValueInRange(
            @Param("eventName") String eventName,
            @Param("start") Instant start,
            @Param("end") Instant end);

    // ================== Error Tracking ==================

    @Query("""
            SELECT ae.eventName, COUNT(ae) FROM AnalyticsEvent ae
            WHERE ae.eventType = 'ERROR'
            AND ae.createdAt BETWEEN :start AND :end
            GROUP BY ae.eventName
            ORDER BY COUNT(ae) DESC
            """)
    List<Object[]> countErrorsByNameInRange(
            @Param("start") Instant start,
            @Param("end") Instant end);

    // ================== Performance Metrics ==================

    @Query("""
            SELECT ae.eventName, AVG(ae.eventValue), COUNT(ae) FROM AnalyticsEvent ae
            WHERE ae.eventType = 'PERFORMANCE'
            AND ae.createdAt BETWEEN :start AND :end
            GROUP BY ae.eventName
            """)
    List<Object[]> aggregatePerformanceInRange(
            @Param("start") Instant start,
            @Param("end") Instant end);

    // ================== User Journey ==================

    @Query("""
            SELECT ae FROM AnalyticsEvent ae
            WHERE ae.userId = :userId
            AND ae.eventType = 'CONVERSION'
            ORDER BY ae.createdAt ASC
            """)
    List<AnalyticsEvent> findUserConversionJourney(@Param("userId") UUID userId);

    // ================== Cleanup ==================

    @Query("DELETE FROM AnalyticsEvent ae WHERE ae.createdAt < :before")
    void deleteOlderThan(@Param("before") Instant before);
}
