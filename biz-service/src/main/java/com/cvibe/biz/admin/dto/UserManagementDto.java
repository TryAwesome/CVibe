package com.cvibe.biz.admin.dto;

import com.cvibe.biz.user.entity.User.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * User Management DTOs for admin operations
 */
public class UserManagementDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserListItem {
        private UUID id;
        private String email;
        private String fullName;
        private String avatarUrl;
        private String role;
        private Boolean enabled;
        private Instant createdAt;
        private Instant lastLoginAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDetailResponse {
        private UUID id;
        private String email;
        private String fullName;
        private String avatarUrl;
        private String role;
        private Boolean enabled;
        private Boolean emailVerified;
        private Boolean googleLinked;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant lastLoginAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDetail {
        private UUID id;
        private String email;
        private String fullName;
        private String phone;
        private String avatarUrl;
        private UserRole role;
        private Boolean isEnabled;
        private Boolean emailVerified;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant lastLoginAt;
        private String lastLoginIp;
        private int loginCount;
        
        // Profile info
        private String headline;
        private String location;
        private String linkedinUrl;
        private String githubUrl;
        
        // Stats
        private int resumeCount;
        private int interviewCount;
        private int mockInterviewCount;
        private int postCount;
        private int commentCount;
        private int followerCount;
        private int followingCount;
        
        // Recent activity
        private List<String> recentActivities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUserRequest {
        private String fullName;
        private String role;
        private Boolean enabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkUserActionRequest {
        private List<UUID> userIds;
        private BulkAction action;
        private String reason;
    }

    public enum BulkAction {
        ENABLE,
        DISABLE,
        DELETE,
        RESET_PASSWORD,
        CHANGE_ROLE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkActionResult {
        private int totalRequested;
        private int successCount;
        private int failureCount;
        private List<String> errors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSearchRequest {
        private String keyword;         // Search in email, name
        private UserRole role;
        private Boolean isEnabled;
        private Boolean emailVerified;
        private Instant createdAfter;
        private Instant createdBefore;
        private Instant lastLoginAfter;
        private Instant lastLoginBefore;
        private String sortBy;          // createdAt, lastLoginAt, loginCount
        private String sortDirection;   // ASC, DESC
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUserRequest {
        private String email;
        private String password;
        private String fullName;
        private String phone;
        private UserRole role;
        private Boolean sendWelcomeEmail;
    }
}
