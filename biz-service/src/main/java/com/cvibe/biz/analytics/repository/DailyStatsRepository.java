package com.cvibe.biz.analytics.repository;

import com.cvibe.biz.analytics.entity.DailyStats;
import com.cvibe.biz.analytics.entity.DailyStats.StatType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyStatsRepository extends JpaRepository<DailyStats, UUID> {

    // ================== Basic Queries ==================

    Optional<DailyStats> findByStatDateAndStatTypeAndStatKey(LocalDate date, StatType type, String key);

    List<DailyStats> findByStatDate(LocalDate date);

    List<DailyStats> findByStatType(StatType type);

    List<DailyStats> findByStatTypeAndStatKey(StatType type, String key);

    // ================== Date Range Queries ==================

    List<DailyStats> findByStatDateBetween(LocalDate start, LocalDate end);

    List<DailyStats> findByStatTypeAndStatDateBetween(StatType type, LocalDate start, LocalDate end);

    List<DailyStats> findByStatTypeAndStatKeyAndStatDateBetween(
            StatType type, String key, LocalDate start, LocalDate end);

    @Query("""
            SELECT ds FROM DailyStats ds
            WHERE ds.statType = :type
            AND ds.statKey = :key
            AND ds.statDate BETWEEN :start AND :end
            ORDER BY ds.statDate ASC
            """)
    List<DailyStats> findTrendData(
            @Param("type") StatType type,
            @Param("key") String key,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // ================== Aggregation Queries ==================

    @Query("""
            SELECT SUM(ds.countValue) FROM DailyStats ds
            WHERE ds.statType = :type
            AND ds.statKey = :key
            AND ds.statDate BETWEEN :start AND :end
            """)
    Long sumCountInRange(
            @Param("type") StatType type,
            @Param("key") String key,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
            SELECT AVG(ds.countValue) FROM DailyStats ds
            WHERE ds.statType = :type
            AND ds.statKey = :key
            AND ds.statDate BETWEEN :start AND :end
            """)
    Double avgCountInRange(
            @Param("type") StatType type,
            @Param("key") String key,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
            SELECT ds.statKey, SUM(ds.countValue), AVG(ds.avgValue) FROM DailyStats ds
            WHERE ds.statType = :type
            AND ds.statDate BETWEEN :start AND :end
            GROUP BY ds.statKey
            ORDER BY SUM(ds.countValue) DESC
            """)
    List<Object[]> aggregateByKeyInRange(
            @Param("type") StatType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // ================== Comparison Queries ==================

    @Query("""
            SELECT ds FROM DailyStats ds
            WHERE ds.statType = :type
            AND ds.statKey = :key
            AND ds.statDate = :date
            """)
    Optional<DailyStats> findForComparison(
            @Param("type") StatType type,
            @Param("key") String key,
            @Param("date") LocalDate date);

    @Query("""
            SELECT ds.statDate, ds.countValue FROM DailyStats ds
            WHERE ds.statType = :type
            AND ds.statKey = :key
            AND ds.statDate IN :dates
            ORDER BY ds.statDate
            """)
    List<Object[]> findForDates(
            @Param("type") StatType type,
            @Param("key") String key,
            @Param("dates") List<LocalDate> dates);

    // ================== Latest Stats ==================

    @Query("""
            SELECT ds FROM DailyStats ds
            WHERE ds.statType = :type
            AND ds.statKey = :key
            ORDER BY ds.statDate DESC
            LIMIT 1
            """)
    Optional<DailyStats> findLatest(
            @Param("type") StatType type,
            @Param("key") String key);

    @Query("""
            SELECT ds FROM DailyStats ds
            WHERE ds.statType = :type
            ORDER BY ds.statDate DESC
            """)
    List<DailyStats> findLatestByType(@Param("type") StatType type, Pageable pageable);

    // ================== Growth Analysis ==================

    @Query("""
            SELECT ds.statDate, ds.countValue, ds.changePercent FROM DailyStats ds
            WHERE ds.statType = :type
            AND ds.statKey = :key
            AND ds.statDate BETWEEN :start AND :end
            ORDER BY ds.statDate
            """)
    List<Object[]> findGrowthTrend(
            @Param("type") StatType type,
            @Param("key") String key,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // ================== Report Generation ==================

    @Query("""
            SELECT ds.statKey, ds.countValue, ds.avgValue, ds.changePercent FROM DailyStats ds
            WHERE ds.statType = :type
            AND ds.statDate = :date
            ORDER BY ds.countValue DESC
            """)
    List<Object[]> generateDailyReport(
            @Param("type") StatType type,
            @Param("date") LocalDate date);

    @Query("""
            SELECT ds.statType, ds.statKey, SUM(ds.countValue), AVG(ds.avgValue) FROM DailyStats ds
            WHERE ds.statDate BETWEEN :start AND :end
            GROUP BY ds.statType, ds.statKey
            ORDER BY ds.statType, SUM(ds.countValue) DESC
            """)
    List<Object[]> generatePeriodReport(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // ================== Cleanup ==================

    void deleteByStatDateBefore(LocalDate date);
}
