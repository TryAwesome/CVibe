package com.cvibe.biz.analytics.controller;

import com.cvibe.biz.analytics.dto.PlatformAnalyticsDto.*;
import com.cvibe.biz.analytics.dto.ReportDto.*;
import com.cvibe.biz.analytics.dto.UserAnalyticsDto.*;
import com.cvibe.biz.analytics.entity.DailyStats.StatType;
import com.cvibe.biz.analytics.entity.UserActivity.ActivityCategory;
import com.cvibe.biz.analytics.entity.UserActivity.ActivityType;
import com.cvibe.biz.analytics.service.AnalyticsService;
import com.cvibe.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "数据分析与洞察 API")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ================== Activity Tracking ==================

    @PostMapping("/track/activity")
    @Operation(summary = "记录用户活动", description = "追踪用户行为活动")
    public ResponseEntity<ApiResponse<Void>> trackActivity(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam ActivityType type,
            @RequestParam ActivityCategory category,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String metadata) {
        
        UUID userId = UUID.fromString(userDetails.getUsername());
        analyticsService.trackActivity(userId, type, category, entityType, entityId, metadata);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/track/event")
    @Operation(summary = "记录分析事件", description = "记录平台级分析事件")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> trackEvent(
            @RequestParam String eventName,
            @RequestParam com.cvibe.biz.analytics.entity.AnalyticsEvent.EventType eventType,
            @RequestParam(required = false) Double eventValue,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String properties) {
        
        analyticsService.trackEvent(eventName, eventType, eventValue, userId, properties);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ================== User Analytics ==================

    @GetMapping("/user/summary")
    @Operation(summary = "获取用户活动摘要", description = "获取当前用户的活动数据摘要")
    public ResponseEntity<ApiResponse<UserActivitySummary>> getUserActivitySummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        
        UUID userId = UUID.fromString(userDetails.getUsername());
        UserActivitySummary summary = analyticsService.getUserActivitySummary(userId, start, end);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/user/{userId}/summary")
    @Operation(summary = "获取指定用户活动摘要", description = "管理员获取指定用户的活动数据摘要")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserActivitySummary>> getUserActivitySummaryByAdmin(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        
        UserActivitySummary summary = analyticsService.getUserActivitySummary(userId, start, end);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/user/engagement")
    @Operation(summary = "获取用户参与度指标", description = "获取当前用户的参与度分析数据")
    public ResponseEntity<ApiResponse<UserEngagementMetrics>> getUserEngagementMetrics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "30") int days) {
        
        UUID userId = UUID.fromString(userDetails.getUsername());
        UserEngagementMetrics metrics = analyticsService.getUserEngagementMetrics(userId, days);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/user/{userId}/engagement")
    @Operation(summary = "获取指定用户参与度指标", description = "管理员获取指定用户的参与度分析数据")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserEngagementMetrics>> getUserEngagementMetricsByAdmin(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "30") int days) {
        
        UserEngagementMetrics metrics = analyticsService.getUserEngagementMetrics(userId, days);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    // ================== Platform Analytics ==================

    @GetMapping("/platform/overview")
    @Operation(summary = "获取平台数据概览", description = "管理员获取平台整体数据分析")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PlatformOverview>> getPlatformOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        PlatformOverview overview = analyticsService.getPlatformOverview(start, end);
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    // ================== Trends ==================

    @GetMapping("/trends/{type}/{key}")
    @Operation(summary = "获取趋势数据", description = "获取指定指标的趋势数据")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TrendData>>> getTrendData(
            @PathVariable StatType type,
            @PathVariable String key,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        List<TrendData> trends = analyticsService.getTrendData(type, key, start, end);
        return ResponseEntity.ok(ApiResponse.success(trends));
    }

    // ================== Feature Usage ==================

    @GetMapping("/features/usage")
    @Operation(summary = "获取功能使用报告", description = "获取平台功能使用情况分析")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeatureUsageReport>> getFeatureUsageReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        FeatureUsageReport report = analyticsService.getFeatureUsageReport(start, end);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // ================== Reports ==================

    @GetMapping("/reports/daily")
    @Operation(summary = "获取日报", description = "获取指定日期的每日摘要报告")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DailySummaryReport>> getDailySummaryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        DailySummaryReport report = analyticsService.generateDailySummary(date);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/reports/daily/today")
    @Operation(summary = "获取今日日报", description = "获取今日的每日摘要报告")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DailySummaryReport>> getTodayDailySummaryReport() {
        DailySummaryReport report = analyticsService.generateDailySummary(LocalDate.now());
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // ================== Admin: Manual Aggregation ==================

    @PostMapping("/admin/aggregate-daily")
    @Operation(summary = "手动触发日统计聚合", description = "管理员手动触发每日统计数据聚合")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> triggerDailyAggregation() {
        analyticsService.aggregateDailyStats();
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
