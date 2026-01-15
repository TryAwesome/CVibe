package com.cvibe.biz.admin.service;

import com.cvibe.biz.admin.dto.*;
import com.cvibe.biz.admin.dto.AdminDashboardDto.*;
import com.cvibe.biz.admin.dto.AuditLogDto.*;
import com.cvibe.biz.admin.dto.AnnouncementDto.*;
import com.cvibe.biz.admin.dto.SystemConfigDto.*;
import com.cvibe.biz.admin.dto.UserManagementDto.*;
import com.cvibe.biz.admin.entity.*;
import com.cvibe.biz.admin.entity.AuditLog.AuditAction;
import com.cvibe.biz.admin.entity.AuditLog.AuditStatus;
import com.cvibe.biz.admin.entity.Announcement.*;
import com.cvibe.biz.admin.entity.SystemConfig.*;
import com.cvibe.biz.admin.repository.*;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.response.ErrorCode;
import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.entity.User.UserRole;
import com.cvibe.biz.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final AnnouncementRepository announcementRepository;
    private final PasswordEncoder passwordEncoder;

    // ================== Dashboard ==================

    public AdminDashboardDto getDashboard() {
        Instant now = Instant.now();
        Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        Instant oneWeekAgo = now.minus(7, ChronoUnit.DAYS);
        Instant oneMonthAgo = now.minus(30, ChronoUnit.DAYS);

        return AdminDashboardDto.builder()
                .userStats(buildUserStats(startOfDay, oneWeekAgo, oneMonthAgo))
                .contentStats(buildContentStats())
                .activityStats(buildActivityStats(startOfDay))
                .systemHealth(buildSystemHealth())
                .recentActivities(getRecentActivities(10))
                .build();
    }

    private UserStats buildUserStats(Instant startOfDay, Instant oneWeekAgo, Instant oneMonthAgo) {
        long totalUsers = userRepository.count();
        long newUsersToday = userRepository.countByCreatedAtAfter(startOfDay);
        long newUsersThisWeek = userRepository.countByCreatedAtAfter(oneWeekAgo);
        long newUsersThisMonth = userRepository.countByCreatedAtAfter(oneMonthAgo);
        long activeUsers = userRepository.countByLastLoginAtAfter(oneMonthAgo);

        return UserStats.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersThisMonth(newUsersThisMonth)
                .premiumUsers(0L)
                .disabledUsers(0L)
                .userGrowthRate(0.0)
                .build();
    }

    private ContentStats buildContentStats() {
        return ContentStats.builder()
                .totalResumes(0)
                .totalInterviews(0)
                .totalMockInterviews(0)
                .totalPosts(0)
                .totalComments(0)
                .jobsApplied(0)
                .resumesCreatedToday(0)
                .interviewsToday(0)
                .build();
    }

    private ActivityStats buildActivityStats(Instant startOfDay) {
        long auditLogsToday = auditLogRepository.countByCreatedAtAfter(startOfDay);
        return ActivityStats.builder()
                .loginsToday(0L)
                .apiCallsToday(0L)
                .auditLogsToday(auditLogsToday)
                .build();
    }

    private SystemHealth buildSystemHealth() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        
        return SystemHealth.builder()
                .status("HEALTHY")
                .cpuUsage(0.0)
                .memoryUsage((double) usedMemory / maxMemory * 100)
                .diskUsage(0.0)
                .build();
    }

    private List<RecentActivity> getRecentActivities(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogRepository.findAll(pageable).stream()
                .map(this::toRecentActivity)
                .collect(Collectors.toList());
    }

    private RecentActivity toRecentActivity(AuditLog log) {
        return RecentActivity.builder()
                .type(log.getAction().name())
                .description(log.getDescription())
                .userName(log.getUser() != null ? log.getUser().getFullName() : "System")
                .timestamp(log.getCreatedAt())
                .status(log.getStatus().name())
                .build();
    }

    // ================== User Management ==================

    public Page<UserListItem> listUsers(Pageable pageable, String search, String role, Boolean enabled) {
        Page<User> users;
        if (search != null && !search.isEmpty()) {
            users = userRepository.searchByKeyword(search, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(this::toUserListItem);
    }

    private UserListItem toUserListItem(User user) {
        return UserListItem.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    public UserDetailResponse getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return toUserDetailResponse(user);
    }

    private UserDetailResponse toUserDetailResponse(User user) {
        return UserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .emailVerified(true)
                .googleLinked(user.getGoogleSub() != null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    @Transactional
    public UserDetailResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getRole() != null) {
            user.setRole(UserRole.valueOf(request.getRole()));
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        User savedUser = userRepository.save(user);
        logAudit(user, AuditAction.USER_UPDATE, "Updated user: " + user.getEmail());
        return toUserDetailResponse(savedUser);
    }

    @Transactional
    public void disableUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setEnabled(false);
        userRepository.save(user);
        logAudit(user, AuditAction.USER_DISABLE, "Disabled user: " + user.getEmail());
    }

    @Transactional
    public void enableUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setEnabled(true);
        userRepository.save(user);
        logAudit(user, AuditAction.USER_ENABLE, "Enabled user: " + user.getEmail());
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        logAudit(user, AuditAction.USER_DELETE, "Deleted user: " + user.getEmail());
        userRepository.delete(user);
    }

    @Transactional
    public void resetUserPassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logAudit(user, AuditAction.PASSWORD_RESET, "Reset password for: " + user.getEmail());
    }

    // ================== Audit Logs ==================

    public Page<AuditLogResponse> getAuditLogs(Pageable pageable, AuditAction action, 
                                               UUID userId, Instant startDate, Instant endDate) {
        return auditLogRepository.findAll(pageable).map(this::toAuditLogResponse);
    }

    private AuditLogResponse toAuditLogResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .status(log.getStatus())
                .description(log.getDescription())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userName(log.getUser() != null ? log.getUser().getFullName() : null)
                .targetType(log.getEntityType())
                .targetId(log.getEntityId())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }

    @Transactional
    public void logAudit(User user, AuditAction action, String description) {
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .status(AuditStatus.SUCCESS)
                .description(description)
                .build();
        auditLogRepository.save(log);
    }

    // ================== System Configs ==================

    public List<ConfigResponse> getAllConfigs() {
        return systemConfigRepository.findAll().stream()
                .map(this::toConfigResponse)
                .collect(Collectors.toList());
    }

    public List<ConfigResponse> getConfigsByCategory(ConfigCategory category) {
        return systemConfigRepository.findByCategory(category).stream()
                .map(this::toConfigResponse)
                .collect(Collectors.toList());
    }

    public ConfigResponse getConfig(String key) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND));
        return toConfigResponse(config);
    }

    private ConfigResponse toConfigResponse(SystemConfig config) {
        return ConfigResponse.builder()
                .id(config.getId())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .category(config.getCategory())
                .valueType(config.getValueType())
                .description(config.getDescription())
                .isEncrypted(config.getIsSensitive())
                .isReadonly(!config.getIsEditable())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    @Transactional
    public ConfigResponse createConfig(CreateConfigRequest request) {
        if (systemConfigRepository.existsByConfigKey(request.getConfigKey())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ENTRY);
        }

        SystemConfig config = SystemConfig.builder()
                .configKey(request.getConfigKey())
                .configValue(request.getConfigValue())
                .category(request.getCategory())
                .valueType(request.getValueType())
                .description(request.getDescription())
                .isSensitive(request.getEncrypted() != null && request.getEncrypted())
                .isEditable(request.getReadonly() == null || !request.getReadonly())
                .build();

        SystemConfig saved = systemConfigRepository.save(config);
        logAudit(null, AuditAction.CONFIG_UPDATE, "Created config: " + request.getConfigKey());
        return toConfigResponse(saved);
    }

    @Transactional
    public ConfigResponse updateConfig(String key, UpdateConfigRequest request) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND));

        if (config.getIsEditable() != null && !config.getIsEditable()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION);
        }

        if (request.getConfigValue() != null) {
            config.setConfigValue(request.getConfigValue());
        }
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }

        SystemConfig saved = systemConfigRepository.save(config);
        logAudit(null, AuditAction.CONFIG_UPDATE, "Updated config: " + key);
        return toConfigResponse(saved);
    }

    @Transactional
    public void deleteConfig(String key) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND));

        if (config.getIsEditable() != null && !config.getIsEditable()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION);
        }

        systemConfigRepository.delete(config);
        logAudit(null, AuditAction.CONFIG_UPDATE, "Deleted config: " + key);
    }

    // ================== Announcements ==================

    public Page<AnnouncementResponse> getAnnouncements(Pageable pageable, AnnouncementType type,
                                                        AnnouncementStatus status, TargetAudience audience) {
        return announcementRepository.findAll(pageable).map(this::toAnnouncementResponse);
    }

    public AnnouncementResponse getAnnouncement(UUID id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        return toAnnouncementResponse(announcement);
    }

    public List<AnnouncementResponse> getActiveAnnouncements(TargetAudience audience) {
        List<Announcement> announcements = announcementRepository
                .findActiveForAudience(Instant.now(), audience);
        return announcements.stream()
                .map(this::toAnnouncementResponse)
                .collect(Collectors.toList());
    }

    private AnnouncementResponse toAnnouncementResponse(Announcement a) {
        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .announcementType(a.getAnnouncementType())
                .priority(a.getPriority())
                .targetAudience(a.getTargetAudience())
                .status(a.getStatus())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .isPinned(a.getIsPinned())
                .isDismissible(a.getIsDismissible())
                .linkUrl(a.getLinkUrl())
                .linkText(a.getLinkText())
                .viewCount(a.getViewCount())
                .dismissCount(a.getDismissCount())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .isCurrentlyActive(a.isCurrentlyActive())
                .build();
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(CreateAnnouncementRequest request) {
        // 需要获取当前管理员用户 - 这里简化处理
        User admin = userRepository.findAll(PageRequest.of(0, 1)).getContent().get(0);
        
        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .announcementType(request.getAnnouncementType())
                .priority(request.getPriority())
                .targetAudience(request.getTargetAudience())
                .status(AnnouncementStatus.DRAFT)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isPinned(request.getIsPinned() != null && request.getIsPinned())
                .createdBy(admin)
                .build();

        Announcement saved = announcementRepository.save(announcement);
        logAudit(admin, AuditAction.ANNOUNCEMENT_CREATE, "Created announcement: " + request.getTitle());
        return toAnnouncementResponse(saved);
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(UUID id, UpdateAnnouncementRequest request) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));

        if (request.getTitle() != null) a.setTitle(request.getTitle());
        if (request.getContent() != null) a.setContent(request.getContent());
        if (request.getAnnouncementType() != null) a.setAnnouncementType(request.getAnnouncementType());
        if (request.getPriority() != null) a.setPriority(request.getPriority());
        if (request.getTargetAudience() != null) a.setTargetAudience(request.getTargetAudience());
        if (request.getStartTime() != null) a.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) a.setEndTime(request.getEndTime());
        if (request.getIsPinned() != null) a.setIsPinned(request.getIsPinned());

        Announcement saved = announcementRepository.save(a);
        logAudit(null, AuditAction.ANNOUNCEMENT_CREATE, "Updated announcement: " + a.getTitle());
        return toAnnouncementResponse(saved);
    }

    @Transactional
    public void publishAnnouncement(UUID id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        a.publish();
        announcementRepository.save(a);
        logAudit(null, AuditAction.ANNOUNCEMENT_CREATE, "Published announcement: " + a.getTitle());
    }

    @Transactional
    public void archiveAnnouncement(UUID id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        a.archive();
        announcementRepository.save(a);
        logAudit(null, AuditAction.ANNOUNCEMENT_CREATE, "Archived announcement: " + a.getTitle());
    }

    @Transactional
    public void deleteAnnouncement(UUID id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        logAudit(null, AuditAction.ANNOUNCEMENT_CREATE, "Deleted announcement: " + a.getTitle());
        announcementRepository.delete(a);
    }
}
