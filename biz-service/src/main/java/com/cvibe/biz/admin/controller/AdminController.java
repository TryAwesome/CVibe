package com.cvibe.biz.admin.controller;

import com.cvibe.biz.admin.dto.AdminDashboardDto;
import com.cvibe.biz.admin.dto.AuditLogDto.*;
import com.cvibe.biz.admin.dto.AnnouncementDto.*;
import com.cvibe.biz.admin.dto.SystemConfigDto.*;
import com.cvibe.biz.admin.dto.UserManagementDto.*;
import com.cvibe.biz.admin.entity.AuditLog.AuditAction;
import com.cvibe.biz.admin.entity.Announcement.*;
import com.cvibe.biz.admin.entity.SystemConfig.ConfigCategory;
import com.cvibe.biz.admin.service.AdminService;
import com.cvibe.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "管理后台 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ================== Dashboard ==================

    @GetMapping("/dashboard")
    @Operation(summary = "获取管理仪表板", description = "获取系统整体统计数据和概览")
    public ResponseEntity<ApiResponse<AdminDashboardDto>> getDashboard() {
        AdminDashboardDto dashboard = adminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    // ================== User Management ==================

    @GetMapping("/users")
    @Operation(summary = "获取用户列表", description = "分页获取所有用户")
    public ResponseEntity<ApiResponse<Page<UserListItem>>> listUsers(
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String search,
            @Parameter(description = "角色筛选") @RequestParam(required = false) String role,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled) {
        Page<UserListItem> users = adminService.listUsers(pageable, search, role, enabled);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "获取用户详情", description = "获取指定用户的详细信息")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserDetail(
            @Parameter(description = "用户ID") @PathVariable UUID userId) {
        UserDetailResponse user = adminService.getUserDetail(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/users/{userId}")
    @Operation(summary = "更新用户", description = "更新用户信息")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUser(
            @Parameter(description = "用户ID") @PathVariable UUID userId,
            @RequestBody UpdateUserRequest request) {
        UserDetailResponse user = adminService.updateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/users/{userId}/disable")
    @Operation(summary = "禁用用户", description = "禁用用户账号")
    public ResponseEntity<ApiResponse<Void>> disableUser(
            @Parameter(description = "用户ID") @PathVariable UUID userId) {
        adminService.disableUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/users/{userId}/enable")
    @Operation(summary = "启用用户", description = "启用用户账号")
    public ResponseEntity<ApiResponse<Void>> enableUser(
            @Parameter(description = "用户ID") @PathVariable UUID userId) {
        adminService.enableUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "删除用户", description = "永久删除用户账号")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "用户ID") @PathVariable UUID userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/users/{userId}/reset-password")
    @Operation(summary = "重置密码", description = "重置用户密码")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Parameter(description = "用户ID") @PathVariable UUID userId,
            @RequestParam String newPassword) {
        adminService.resetUserPassword(userId, newPassword);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ================== Audit Logs ==================

    @GetMapping("/audit-logs")
    @Operation(summary = "获取审计日志", description = "查询审计日志")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @PageableDefault(size = 50) Pageable pageable,
            @Parameter(description = "操作类型") @RequestParam(required = false) AuditAction action,
            @Parameter(description = "用户ID") @RequestParam(required = false) UUID userId,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        Page<AuditLogResponse> logs = adminService.getAuditLogs(pageable, action, userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    // ================== System Configs ==================

    @GetMapping("/configs")
    @Operation(summary = "获取所有配置", description = "获取所有系统配置项")
    public ResponseEntity<ApiResponse<List<ConfigResponse>>> getAllConfigs() {
        List<ConfigResponse> configs = adminService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    @GetMapping("/configs/category/{category}")
    @Operation(summary = "按类别获取配置", description = "获取指定类别的配置项")
    public ResponseEntity<ApiResponse<List<ConfigResponse>>> getConfigsByCategory(
            @PathVariable ConfigCategory category) {
        List<ConfigResponse> configs = adminService.getConfigsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    @GetMapping("/configs/{key}")
    @Operation(summary = "获取配置项", description = "获取指定key的配置项")
    public ResponseEntity<ApiResponse<ConfigResponse>> getConfig(
            @PathVariable String key) {
        ConfigResponse config = adminService.getConfig(key);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PostMapping("/configs")
    @Operation(summary = "创建配置", description = "创建新的系统配置项")
    public ResponseEntity<ApiResponse<ConfigResponse>> createConfig(
            @RequestBody CreateConfigRequest request) {
        ConfigResponse config = adminService.createConfig(request);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PutMapping("/configs/{key}")
    @Operation(summary = "更新配置", description = "更新系统配置项")
    public ResponseEntity<ApiResponse<ConfigResponse>> updateConfig(
            @PathVariable String key,
            @RequestBody UpdateConfigRequest request) {
        ConfigResponse config = adminService.updateConfig(key, request);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @DeleteMapping("/configs/{key}")
    @Operation(summary = "删除配置", description = "删除系统配置项")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable String key) {
        adminService.deleteConfig(key);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ================== Announcements ==================

    @GetMapping("/announcements")
    @Operation(summary = "获取公告列表", description = "分页获取公告")
    public ResponseEntity<ApiResponse<Page<AnnouncementResponse>>> getAnnouncements(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) AnnouncementType type,
            @RequestParam(required = false) AnnouncementStatus status,
            @RequestParam(required = false) TargetAudience audience) {
        Page<AnnouncementResponse> announcements = adminService.getAnnouncements(pageable, type, status, audience);
        return ResponseEntity.ok(ApiResponse.success(announcements));
    }

    @GetMapping("/announcements/{id}")
    @Operation(summary = "获取公告详情", description = "获取指定公告的详细信息")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncement(
            @PathVariable UUID id) {
        AnnouncementResponse announcement = adminService.getAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(announcement));
    }

    @GetMapping("/announcements/active")
    @Operation(summary = "获取活跃公告", description = "获取当前活跃的公告")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getActiveAnnouncements(
            @RequestParam(defaultValue = "ALL") TargetAudience audience) {
        List<AnnouncementResponse> announcements = adminService.getActiveAnnouncements(audience);
        return ResponseEntity.ok(ApiResponse.success(announcements));
    }

    @PostMapping("/announcements")
    @Operation(summary = "创建公告", description = "创建新公告")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @RequestBody CreateAnnouncementRequest request) {
        AnnouncementResponse announcement = adminService.createAnnouncement(request);
        return ResponseEntity.ok(ApiResponse.success(announcement));
    }

    @PutMapping("/announcements/{id}")
    @Operation(summary = "更新公告", description = "更新公告信息")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @PathVariable UUID id,
            @RequestBody UpdateAnnouncementRequest request) {
        AnnouncementResponse announcement = adminService.updateAnnouncement(id, request);
        return ResponseEntity.ok(ApiResponse.success(announcement));
    }

    @PostMapping("/announcements/{id}/publish")
    @Operation(summary = "发布公告", description = "发布草稿公告")
    public ResponseEntity<ApiResponse<Void>> publishAnnouncement(@PathVariable UUID id) {
        adminService.publishAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/announcements/{id}/archive")
    @Operation(summary = "归档公告", description = "归档公告")
    public ResponseEntity<ApiResponse<Void>> archiveAnnouncement(@PathVariable UUID id) {
        adminService.archiveAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/announcements/{id}")
    @Operation(summary = "删除公告", description = "删除公告")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable UUID id) {
        adminService.deleteAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
