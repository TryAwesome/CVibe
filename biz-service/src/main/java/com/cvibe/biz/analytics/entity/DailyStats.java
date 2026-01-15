package com.cvibe.biz.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DailyStats Entity
 * 
 * Pre-aggregated daily statistics for fast reporting.
 */
@Entity
@Table(name = "daily_stats", indexes = {
        @Index(name = "idx_daily_stats_date", columnList = "stat_date"),
        @Index(name = "idx_daily_stats_type", columnList = "stat_type")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_stats", columnNames = {"stat_date", "stat_type", "stat_key"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Statistics date
     */
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    /**
     * Statistics type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false)
    private StatType statType;

    /**
     * Statistics key (e.g., "registrations", "logins", "resumes_created")
     */
    @Column(name = "stat_key", nullable = false)
    private String statKey;

    /**
     * Count value
     */
    @Column(name = "count_value")
    @Builder.Default
    private Long countValue = 0L;

    /**
     * Sum value
     */
    @Column(name = "sum_value")
    @Builder.Default
    private Double sumValue = 0.0;

    /**
     * Average value
     */
    @Column(name = "avg_value")
    @Builder.Default
    private Double avgValue = 0.0;

    /**
     * Minimum value
     */
    @Column(name = "min_value")
    private Double minValue;

    /**
     * Maximum value
     */
    @Column(name = "max_value")
    private Double maxValue;

    /**
     * Additional breakdown data (JSON)
     * e.g., {"by_device": {"mobile": 100, "desktop": 200}}
     */
    @Column(name = "breakdown", columnDefinition = "TEXT")
    private String breakdown;

    /**
     * Comparison with previous period (%)
     */
    @Column(name = "change_percent")
    private Double changePercent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ================== Enums ==================

    public enum StatType {
        USER,           // User statistics
        ENGAGEMENT,     // Engagement metrics
        CONTENT,        // Content statistics
        INTERVIEW,      // Interview statistics
        JOB,            // Job-related statistics
        RESUME,         // Resume statistics
        COMMUNITY,      // Community statistics
        CONVERSION,     // Conversion rates
        PERFORMANCE     // Performance metrics
    }

    // ================== Static Factory ==================

    public static DailyStats count(LocalDate date, StatType type, String key, long count) {
        return DailyStats.builder()
                .statDate(date)
                .statType(type)
                .statKey(key)
                .countValue(count)
                .build();
    }

    public static DailyStats withAvg(LocalDate date, StatType type, String key, long count, double sum) {
        return DailyStats.builder()
                .statDate(date)
                .statType(type)
                .statKey(key)
                .countValue(count)
                .sumValue(sum)
                .avgValue(count > 0 ? sum / count : 0.0)
                .build();
    }

    // ================== Business Methods ==================

    public void incrementCount(long delta) {
        this.countValue = (this.countValue == null ? 0L : this.countValue) + delta;
    }

    public void addToSum(double value) {
        this.sumValue = (this.sumValue == null ? 0.0 : this.sumValue) + value;
        recalculateAverage();
    }

    private void recalculateAverage() {
        if (countValue != null && countValue > 0 && sumValue != null) {
            this.avgValue = sumValue / countValue;
        }
    }

    public void updateMinMax(double value) {
        if (minValue == null || value < minValue) {
            this.minValue = value;
        }
        if (maxValue == null || value > maxValue) {
            this.maxValue = value;
        }
    }

    public void calculateChangePercent(DailyStats previous) {
        if (previous != null && previous.getCountValue() != null && previous.getCountValue() > 0) {
            double previousCount = previous.getCountValue();
            double currentCount = this.countValue != null ? this.countValue : 0;
            this.changePercent = ((currentCount - previousCount) / previousCount) * 100;
        }
    }
}
