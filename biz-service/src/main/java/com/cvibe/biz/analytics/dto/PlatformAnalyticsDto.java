package com.cvibe.biz.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Platform Analytics DTOs
 */
public class PlatformAnalyticsDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformOverview {
        private UserMetrics userMetrics;
        private EngagementMetrics engagementMetrics;
        private ContentMetrics contentMetrics;
        private ConversionMetrics conversionMetrics;
        private List<TrendData> trends;
        private Map<String, Long> deviceDistribution;
        private Map<String, Long> topPages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserMetrics {
        private long totalUsers;
        private long activeUsersToday;
        private long activeUsersWeek;
        private long activeUsersMonth;
        private long newUsersToday;
        private long newUsersWeek;
        private long newUsersMonth;
        private double dailyActiveRate;
        private double weeklyActiveRate;
        private double monthlyActiveRate;
        private double growthRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngagementMetrics {
        private long totalSessions;
        private double avgSessionDuration;
        private double avgPagesPerSession;
        private double bounceRate;
        private Map<String, Long> activityDistribution;
        private Map<Integer, Long> hourlyDistribution;
        private List<TopUser> topActiveUsers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopUser {
        private String id;
        private String name;
        private String email;
        private long activityCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentMetrics {
        private long totalResumes;
        private long resumesCreatedToday;
        private long totalInterviews;
        private long interviewsToday;
        private long totalPosts;
        private long postsToday;
        private long totalComments;
        private long commentsToday;
        private double avgInterviewScore;
        private double avgResumeScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversionMetrics {
        private double registrationRate;
        private double profileCompletionRate;
        private double resumeCreationRate;
        private double interviewCompletionRate;
        private double premiumConversionRate;
        private List<FunnelStep> conversionFunnel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunnelStep {
        private String name;
        private long count;
        private double rate;
        private double dropOffRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendData {
        private LocalDate date;
        private long value;
        private Double changePercent;
        private String metric;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureUsageReport {
        private Map<String, Long> featureUsage;
        private List<FeatureDetail> topFeatures;
        private Map<String, Double> featureAdoption;
        private List<String> underutilizedFeatures;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureDetail {
        private String name;
        private long usageCount;
        private long uniqueUsers;
        private double adoptionRate;
        private Double changePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetentionReport {
        private Map<String, Double> cohortRetention;
        private List<CohortData> cohorts;
        private double day1Retention;
        private double day7Retention;
        private double day30Retention;
        private double churnRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CohortData {
        private LocalDate cohortDate;
        private long cohortSize;
        private Map<Integer, Double> retentionByDay;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyticsRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private String granularity;  // HOUR, DAY, WEEK, MONTH
        private List<String> metrics;
        private List<String> dimensions;
        private Map<String, String> filters;
    }
}
