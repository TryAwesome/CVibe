package com.cvibe.biz.analytics.service;

import com.cvibe.biz.analytics.dto.*;
import com.cvibe.biz.analytics.dto.PlatformAnalyticsDto.*;
import com.cvibe.biz.analytics.dto.ReportDto.*;
import com.cvibe.biz.analytics.dto.UserAnalyticsDto.*;
import com.cvibe.biz.analytics.entity.*;
import com.cvibe.biz.analytics.entity.DailyStats.StatType;
import com.cvibe.biz.analytics.entity.UserActivity.ActivityCategory;
import com.cvibe.biz.analytics.entity.UserActivity.ActivityType;
import com.cvibe.biz.analytics.entity.UserActivity.DeviceType;
import com.cvibe.biz.analytics.repository.*;
import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final UserActivityRepository activityRepository;
    private final AnalyticsEventRepository eventRepository;
    private final DailyStatsRepository statsRepository;
    private final UserRepository userRepository;

    // ================== Activity Tracking ==================

    @Transactional
    public void trackActivity(UUID userId, ActivityType type, ActivityCategory category,
                              String entityType, UUID entityId, String metadata) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserActivity activity = UserActivity.builder()
                .user(user)
                .activityType(type)
                .category(category)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(metadata)
                .build();

        activityRepository.save(activity);
    }

    @Transactional
    public void trackActivityWithContext(UUID userId, ActivityType type, ActivityCategory category,
                                         String sessionId, String ipAddress, String userAgent,
                                         String pagePath, String referrer) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserActivity activity = UserActivity.builder()
                .user(user)
                .activityType(type)
                .category(category)
                .sessionId(sessionId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .pagePath(pagePath)
                .referrer(referrer)
                .deviceType(detectDeviceType(userAgent))
                .build();

        activityRepository.save(activity);
    }

    @Transactional
    public void trackEvent(String eventName, AnalyticsEvent.EventType eventType, 
                          Double eventValue, UUID userId, String properties) {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventName(eventName)
                .eventType(eventType)
                .eventValue(eventValue)
                .userId(userId)
                .properties(properties)
                .build();

        eventRepository.save(event);
    }

    // ================== User Analytics ==================

    public UserActivitySummary getUserActivitySummary(UUID userId, Instant start, Instant end) {
        // Activity by type
        List<Object[]> typeData = activityRepository.countByUserAndTypeInRange(userId, start, end);
        Map<String, Long> activityByType = new LinkedHashMap<>();
        for (Object[] row : typeData) {
            activityByType.put(row[0].toString(), (Long) row[1]);
        }

        // Activity by category
        List<Object[]> categoryData = activityRepository.countByUserAndCategoryInRange(userId, start, end);
        Map<String, Long> activityByCategory = new LinkedHashMap<>();
        for (Object[] row : categoryData) {
            activityByCategory.put(row[0].toString(), (Long) row[1]);
        }

        // Daily activities
        List<Object[]> dailyData = activityRepository.countByUserAndDayInRange(userId, start, end);
        List<DailyActivity> dailyActivities = dailyData.stream()
                .map(row -> DailyActivity.builder()
                        .date(LocalDate.parse(row[0].toString()))
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());

        long totalActivities = activityRepository.countByUserId(userId);
        long sessionsCount = activityRepository.countSessionsByUserInRange(userId, start, end);
        Double avgDuration = activityRepository.avgSessionDurationByUserInRange(userId, start, end);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserActivitySummary.builder()
                .userId(userId)
                .userName(user.getFullName())
                .totalActivities(totalActivities)
                .sessionsCount(sessionsCount)
                .avgSessionDuration(avgDuration)
                .activityByType(activityByType)
                .activityByCategory(activityByCategory)
                .dailyActivities(dailyActivities)
                .build();
    }

    public UserEngagementMetrics getUserEngagementMetrics(UUID userId, int days) {
        Instant start = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant end = Instant.now();

        List<Object[]> dailyData = activityRepository.countByUserAndDayInRange(userId, start, end);
        int daysActive = dailyData.size();

        // Calculate streak
        int streakDays = calculateStreak(dailyData);

        // Calculate engagement score (0-100)
        double engagementScore = calculateEngagementScore(userId, daysActive, days);

        String engagementLevel;
        if (engagementScore >= 80) engagementLevel = "POWER_USER";
        else if (engagementScore >= 60) engagementLevel = "HIGH";
        else if (engagementScore >= 30) engagementLevel = "MEDIUM";
        else engagementLevel = "LOW";

        // Feature usage
        List<Object[]> featureData = activityRepository.countByUserAndTypeInRange(userId, start, end);
        Map<String, Long> featureUsage = new LinkedHashMap<>();
        for (Object[] row : featureData) {
            featureUsage.put(row[0].toString(), (Long) row[1]);
        }

        List<String> topFeatures = featureUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return UserEngagementMetrics.builder()
                .userId(userId)
                .daysActive(daysActive)
                .streakDays(streakDays)
                .engagementScore(engagementScore)
                .engagementLevel(engagementLevel)
                .featureUsage(featureUsage)
                .topFeatures(topFeatures)
                .build();
    }

    // ================== Platform Analytics ==================

    public PlatformOverview getPlatformOverview(LocalDate start, LocalDate end) {
        Instant startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        return PlatformOverview.builder()
                .userMetrics(buildUserMetrics(startInstant, endInstant))
                .engagementMetrics(buildEngagementMetrics(startInstant, endInstant))
                .contentMetrics(buildContentMetrics(startInstant, endInstant))
                .conversionMetrics(buildConversionMetrics(startInstant, endInstant))
                .deviceDistribution(getDeviceDistribution(startInstant, endInstant))
                .topPages(getTopPages(startInstant, endInstant))
                .build();
    }

    private UserMetrics buildUserMetrics(Instant start, Instant end) {
        Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant weekAgo = today.minus(7, ChronoUnit.DAYS);
        Instant monthAgo = today.minus(30, ChronoUnit.DAYS);

        long totalUsers = userRepository.count();
        long activeToday = activityRepository.countDistinctUsersInRange(today, Instant.now());
        long activeWeek = activityRepository.countDistinctUsersInRange(weekAgo, Instant.now());
        long activeMonth = activityRepository.countDistinctUsersInRange(monthAgo, Instant.now());

        long newToday = userRepository.countByCreatedAtAfter(today);
        long newWeek = userRepository.countByCreatedAtAfter(weekAgo);
        long newMonth = userRepository.countByCreatedAtAfter(monthAgo);

        return UserMetrics.builder()
                .totalUsers(totalUsers)
                .activeUsersToday(activeToday)
                .activeUsersWeek(activeWeek)
                .activeUsersMonth(activeMonth)
                .newUsersToday(newToday)
                .newUsersWeek(newWeek)
                .newUsersMonth(newMonth)
                .dailyActiveRate(totalUsers > 0 ? (double) activeToday / totalUsers * 100 : 0)
                .weeklyActiveRate(totalUsers > 0 ? (double) activeWeek / totalUsers * 100 : 0)
                .monthlyActiveRate(totalUsers > 0 ? (double) activeMonth / totalUsers * 100 : 0)
                .build();
    }

    private EngagementMetrics buildEngagementMetrics(Instant start, Instant end) {
        List<Object[]> activityData = activityRepository.countByTypeInRange(start, end);
        Map<String, Long> activityDistribution = new LinkedHashMap<>();
        for (Object[] row : activityData) {
            activityDistribution.put(row[0].toString(), (Long) row[1]);
        }

        List<Object[]> hourlyData = activityRepository.countByHourInRange(start, end);
        Map<Integer, Long> hourlyDistribution = new LinkedHashMap<>();
        for (Object[] row : hourlyData) {
            hourlyDistribution.put((Integer) row[0], (Long) row[1]);
        }

        List<Object[]> topUsersData = activityRepository.findMostActiveUsers(start, end, PageRequest.of(0, 10));
        List<TopUser> topUsers = topUsersData.stream()
                .map(row -> TopUser.builder()
                        .id(row[0].toString())
                        .name(row[1] != null ? row[1].toString() : "Unknown")
                        .activityCount((Long) row[2])
                        .build())
                .collect(Collectors.toList());

        return EngagementMetrics.builder()
                .activityDistribution(activityDistribution)
                .hourlyDistribution(hourlyDistribution)
                .topActiveUsers(topUsers)
                .build();
    }

    private ContentMetrics buildContentMetrics(Instant start, Instant end) {
        long resumesCreated = activityRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.RESUME_CREATE, start, end);
        long interviews = activityRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.INTERVIEW_COMPLETE, start, end);
        long posts = activityRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.POST_CREATE, start, end);
        long comments = activityRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.COMMENT_CREATE, start, end);

        Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long resumesToday = activityRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.RESUME_CREATE, today, Instant.now());
        long interviewsToday = activityRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.INTERVIEW_COMPLETE, today, Instant.now());

        return ContentMetrics.builder()
                .resumesCreatedToday(resumesToday)
                .interviewsToday(interviewsToday)
                .postsToday(posts)
                .commentsToday(comments)
                .build();
    }

    private ConversionMetrics buildConversionMetrics(Instant start, Instant end) {
        // Simplified conversion funnel
        List<FunnelStep> funnel = new ArrayList<>();
        
        long registrations = activityRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.REGISTER, start, end);
        long profileCompletes = activityRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.PAGE_VIEW, start, end);
        long resumeCreates = activityRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.RESUME_CREATE, start, end);
        long interviewStarts = activityRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.INTERVIEW_START, start, end);

        funnel.add(buildFunnelStep("Registration", registrations, registrations));
        funnel.add(buildFunnelStep("Profile Complete", profileCompletes, registrations));
        funnel.add(buildFunnelStep("Resume Created", resumeCreates, registrations));
        funnel.add(buildFunnelStep("Interview Started", interviewStarts, registrations));

        return ConversionMetrics.builder()
                .conversionFunnel(funnel)
                .registrationRate(100.0)
                .profileCompletionRate(registrations > 0 ? (double) profileCompletes / registrations * 100 : 0)
                .resumeCreationRate(registrations > 0 ? (double) resumeCreates / registrations * 100 : 0)
                .interviewCompletionRate(registrations > 0 ? (double) interviewStarts / registrations * 100 : 0)
                .build();
    }

    private FunnelStep buildFunnelStep(String name, long count, long baseCount) {
        double rate = baseCount > 0 ? (double) count / baseCount * 100 : 0;
        return FunnelStep.builder()
                .name(name)
                .count(count)
                .rate(rate)
                .dropOffRate(100 - rate)
                .build();
    }

    private Map<String, Long> getDeviceDistribution(Instant start, Instant end) {
        List<Object[]> data = activityRepository.countByDeviceInRange(start, end);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : data) {
            if (row[0] != null) {
                result.put(row[0].toString(), (Long) row[1]);
            }
        }
        return result;
    }

    private Map<String, Long> getTopPages(Instant start, Instant end) {
        List<Object[]> data = activityRepository.countPageViewsInRange(start, end, PageRequest.of(0, 10));
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : data) {
            if (row[0] != null) {
                result.put(row[0].toString(), (Long) row[1]);
            }
        }
        return result;
    }

    // ================== Reports ==================

    public DailySummaryReport generateDailySummary(LocalDate date) {
        Instant dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant yesterday = date.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        long newUsers = userRepository.countByCreatedAtBetween(dayStart, dayEnd);
        long activeUsers = activityRepository.countDistinctUsersInRange(dayStart, dayEnd);
        long logins = activityRepository.countByActivityTypeAndCreatedAtBetween(ActivityType.LOGIN, dayStart, dayEnd);
        long resumes = activityRepository.countByActivityTypeAndCreatedAtBetween(ActivityType.RESUME_CREATE, dayStart, dayEnd);
        long interviews = activityRepository.countByActivityTypeAndCreatedAtBetween(ActivityType.INTERVIEW_COMPLETE, dayStart, dayEnd);
        long posts = activityRepository.countByActivityTypeAndCreatedAtBetween(ActivityType.POST_CREATE, dayStart, dayEnd);
        long comments = activityRepository.countByActivityTypeAndCreatedAtBetween(ActivityType.COMMENT_CREATE, dayStart, dayEnd);

        // Yesterday comparison
        long yesterdayNewUsers = userRepository.countByCreatedAtBetween(yesterday, dayStart);
        long yesterdayActiveUsers = activityRepository.countDistinctUsersInRange(yesterday, dayStart);

        Map<String, Double> vsYesterday = new LinkedHashMap<>();
        vsYesterday.put("newUsers", calculateChange(newUsers, yesterdayNewUsers));
        vsYesterday.put("activeUsers", calculateChange(activeUsers, yesterdayActiveUsers));

        List<String> highlights = new ArrayList<>();
        if (newUsers > yesterdayNewUsers * 1.2) {
            highlights.add("New user registrations up " + String.format("%.1f", vsYesterday.get("newUsers")) + "% vs yesterday");
        }

        return DailySummaryReport.builder()
                .date(date)
                .newUsers(newUsers)
                .activeUsers(activeUsers)
                .totalLogins(logins)
                .resumesCreated(resumes)
                .interviewsCompleted(interviews)
                .postsCreated(posts)
                .commentsCreated(comments)
                .vsYesterday(vsYesterday)
                .highlights(highlights)
                .build();
    }

    // ================== Daily Stats Aggregation ==================

    @Scheduled(cron = "0 0 1 * * *") // Run at 1 AM daily
    @Transactional
    public void aggregateDailyStats() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Instant start = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();

        log.info("Aggregating daily stats for {}", yesterday);

        // User stats
        long newUsers = userRepository.countByCreatedAtBetween(start, end);
        long activeUsers = activityRepository.countDistinctUsersInRange(start, end);
        saveDailyStat(yesterday, StatType.USER, "new_users", newUsers);
        saveDailyStat(yesterday, StatType.USER, "active_users", activeUsers);

        // Engagement stats
        for (ActivityType type : ActivityType.values()) {
            long count = activityRepository.countByActivityTypeAndCreatedAtBetween(type, start, end);
            if (count > 0) {
                saveDailyStat(yesterday, StatType.ENGAGEMENT, type.name().toLowerCase(), count);
            }
        }

        log.info("Daily stats aggregation completed for {}", yesterday);
    }

    @Transactional
    public void saveDailyStat(LocalDate date, StatType type, String key, long count) {
        DailyStats existing = statsRepository.findByStatDateAndStatTypeAndStatKey(date, type, key)
                .orElse(null);

        if (existing != null) {
            existing.setCountValue(count);
            statsRepository.save(existing);
        } else {
            DailyStats stats = DailyStats.count(date, type, key, count);
            
            // Calculate change percent from previous day
            statsRepository.findForComparison(type, key, date.minusDays(1))
                    .ifPresent(stats::calculateChangePercent);
            
            statsRepository.save(stats);
        }
    }

    // ================== Helper Methods ==================

    private DeviceType detectDeviceType(String userAgent) {
        if (userAgent == null) return DeviceType.UNKNOWN;
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return DeviceType.MOBILE;
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return DeviceType.TABLET;
        }
        return DeviceType.DESKTOP;
    }

    private int calculateStreak(List<Object[]> dailyData) {
        if (dailyData.isEmpty()) return 0;
        
        // Sort by date descending and count consecutive days
        List<LocalDate> dates = dailyData.stream()
                .map(row -> LocalDate.parse(row[0].toString()))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        int streak = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).plusDays(1).equals(dates.get(i - 1))) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private double calculateEngagementScore(UUID userId, int daysActive, int totalDays) {
        // Simple engagement score based on activity frequency
        double frequencyScore = Math.min(100, (double) daysActive / totalDays * 100);
        
        // Could add more factors like feature variety, session duration, etc.
        return frequencyScore;
    }

    private double calculateChange(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((double) current - previous) / previous * 100;
    }

    // ================== Trend Data ==================

    public List<TrendData> getTrendData(StatType type, String key, LocalDate start, LocalDate end) {
        List<DailyStats> stats = statsRepository.findTrendData(type, key, start, end);
        return stats.stream()
                .map(s -> TrendData.builder()
                        .date(s.getStatDate())
                        .value(s.getCountValue())
                        .changePercent(s.getChangePercent())
                        .metric(key)
                        .build())
                .collect(Collectors.toList());
    }

    // ================== Feature Usage ==================

    public FeatureUsageReport getFeatureUsageReport(LocalDate start, LocalDate end) {
        Instant startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Object[]> data = activityRepository.countFeatureUsageInRange(startInstant, endInstant);
        Map<String, Long> featureUsage = new LinkedHashMap<>();
        for (Object[] row : data) {
            if (row[0] != null) {
                featureUsage.put(row[0].toString(), (Long) row[1]);
            }
        }

        List<FeatureDetail> topFeatures = featureUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> FeatureDetail.builder()
                        .name(e.getKey())
                        .usageCount(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return FeatureUsageReport.builder()
                .featureUsage(featureUsage)
                .topFeatures(topFeatures)
                .build();
    }
}
