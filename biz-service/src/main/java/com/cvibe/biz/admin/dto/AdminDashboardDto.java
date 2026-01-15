package com.cvibe.biz.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Admin Dashboard DTO
 * 
 * Aggregated statistics and metrics for admin dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDto {

    // ================== User Stats ==================
    private UserStats userStats;
    
    // ================== Content Stats ==================
    private ContentStats contentStats;
    
    // ================== Activity Stats ==================
    private ActivityStats activityStats;
    
    // ================== System Health ==================
    private SystemHealth systemHealth;
    
    // ================== Recent Activity ==================
    private List<RecentActivity> recentActivities;
    
    // ================== Charts Data ==================
    private Map<String, Long> userRegistrationsByDay;
    private Map<String, Long> activeUsersByDay;
    private Map<String, Long> auditLogsByDay;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private long totalUsers;
        private long activeUsers;       // Active in last 30 days
        private long newUsersToday;
        private long newUsersThisWeek;
        private long newUsersThisMonth;
        private long premiumUsers;
        private long disabledUsers;
        private double userGrowthRate;  // Month over month %
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentStats {
        private long totalResumes;
        private long totalInterviews;
        private long totalMockInterviews;
        private long totalPosts;
        private long totalComments;
        private long jobsApplied;
        private long resumesCreatedToday;
        private long interviewsToday;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityStats {
        private long loginsToday;
        private long loginsTodaySuccess;
        private long loginsTodayFailed;
        private long apiCallsToday;
        private long errorsToday;
        private long auditLogsToday;
        private Map<String, Long> topActions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemHealth {
        private String status;          // HEALTHY, DEGRADED, DOWN
        private long uptime;            // In seconds
        private double cpuUsage;
        private double memoryUsage;
        private double diskUsage;
        private int activeConnections;
        private int pendingTasks;
        private Instant lastHealthCheck;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String type;
        private String description;
        private String userName;
        private String userEmail;
        private Instant timestamp;
        private String status;
    }
}
