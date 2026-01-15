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
 * User Analytics DTOs
 */
public class UserAnalyticsDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivitySummary {
        private UUID userId;
        private String userName;
        private long totalActivities;
        private long sessionsCount;
        private Double avgSessionDuration;
        private Map<String, Long> activityByType;
        private Map<String, Long> activityByCategory;
        private List<DailyActivity> dailyActivities;
        private Instant firstActivity;
        private Instant lastActivity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyActivity {
        private LocalDate date;
        private long count;
        private List<String> topActivities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserEngagementMetrics {
        private UUID userId;
        private int daysActive;
        private int streakDays;
        private double engagementScore;
        private String engagementLevel;  // LOW, MEDIUM, HIGH, POWER_USER
        private Map<String, Long> featureUsage;
        private List<String> topFeatures;
        private double retentionScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserJourney {
        private UUID userId;
        private List<JourneyStep> steps;
        private String currentStage;  // ONBOARDING, EXPLORING, ENGAGED, POWER_USER
        private double completionRate;
        private List<String> completedMilestones;
        private List<String> pendingMilestones;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JourneyStep {
        private String stepName;
        private String status;  // COMPLETED, IN_PROGRESS, NOT_STARTED
        private Instant completedAt;
        private Integer durationDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInsights {
        private UUID userId;
        private List<Insight> insights;
        private List<Recommendation> recommendations;
        private Map<String, Double> scores;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Insight {
        private String type;
        private String title;
        private String description;
        private String severity;  // INFO, WARNING, SUCCESS
        private Map<String, Object> data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        private String type;
        private String title;
        private String description;
        private String action;
        private String priority;  // LOW, MEDIUM, HIGH
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityTimelineRequest {
        private UUID userId;
        private Instant startDate;
        private Instant endDate;
        private List<String> activityTypes;
        private List<String> categories;
        private int limit;
    }
}
