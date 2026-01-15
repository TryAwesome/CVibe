package com.cvibe.biz.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Report DTOs
 */
public class ReportDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportRequest {
        private ReportType reportType;
        private LocalDate startDate;
        private LocalDate endDate;
        private ReportFormat format;
        private List<String> metrics;
        private Map<String, String> filters;
        private boolean includeCharts;
        private String recipientEmail;
    }

    public enum ReportType {
        DAILY_SUMMARY,
        WEEKLY_SUMMARY,
        MONTHLY_SUMMARY,
        USER_ACTIVITY,
        ENGAGEMENT,
        CONVERSION,
        CONTENT,
        RETENTION,
        CUSTOM
    }

    public enum ReportFormat {
        JSON,
        CSV,
        PDF,
        EXCEL
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedReport {
        private UUID reportId;
        private ReportType reportType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Instant generatedAt;
        private ReportStatus status;
        private String downloadUrl;
        private long fileSize;
        private ReportSummary summary;
    }

    public enum ReportStatus {
        PENDING,
        GENERATING,
        COMPLETED,
        FAILED,
        EXPIRED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportSummary {
        private Map<String, Object> highlights;
        private List<KeyMetric> keyMetrics;
        private List<Trend> trends;
        private List<Alert> alerts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyMetric {
        private String name;
        private String displayName;
        private Object value;
        private Object previousValue;
        private Double changePercent;
        private String changeDirection;  // UP, DOWN, STABLE
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Trend {
        private String metric;
        private String direction;  // INCREASING, DECREASING, STABLE
        private double slope;
        private String insight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        private String type;
        private String severity;  // INFO, WARNING, CRITICAL
        private String title;
        private String description;
        private Map<String, Object> data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySummaryReport {
        private LocalDate date;
        
        // User metrics
        private long newUsers;
        private long activeUsers;
        private long totalLogins;
        
        // Content metrics
        private long resumesCreated;
        private long interviewsCompleted;
        private long postsCreated;
        private long commentsCreated;
        
        // Engagement
        private double avgSessionDuration;
        private long totalPageViews;
        private Map<String, Long> topPages;
        
        // Comparisons
        private Map<String, Double> vsYesterday;
        private Map<String, Double> vsLastWeek;
        
        // Highlights
        private List<String> highlights;
        private List<Alert> alerts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklySummaryReport {
        private LocalDate weekStart;
        private LocalDate weekEnd;
        
        // Aggregates
        private long totalNewUsers;
        private long totalActiveUsers;
        private long peakDailyUsers;
        private LocalDate peakDay;
        
        // Daily breakdown
        private List<DailyMetric> dailyBreakdown;
        
        // Trends
        private Map<String, Double> weekOverWeekChange;
        private List<Trend> trends;
        
        // Top performers
        private List<Map<String, Object>> topUsers;
        private List<Map<String, Object>> topContent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMetric {
        private LocalDate date;
        private String dayOfWeek;
        private Map<String, Long> metrics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportRequest {
        private String dataType;  // users, activities, events, stats
        private LocalDate startDate;
        private LocalDate endDate;
        private ReportFormat format;
        private List<String> fields;
        private Map<String, String> filters;
        private int limit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportResult {
        private UUID exportId;
        private String dataType;
        private long recordCount;
        private String downloadUrl;
        private Instant expiresAt;
        private long fileSizeBytes;
    }
}
