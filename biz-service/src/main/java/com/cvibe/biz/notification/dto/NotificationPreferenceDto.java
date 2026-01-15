package com.cvibe.biz.notification.dto;

import com.cvibe.biz.notification.entity.NotificationPreference.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Notification Preference DTOs
 */
public class NotificationPreferenceDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreferenceResponse {
        private UUID id;
        private UUID userId;
        
        // In-App
        private Boolean inAppEnabled;
        private InAppPreferences inApp;
        
        // Email
        private Boolean emailEnabled;
        private EmailPreferences email;
        
        // Push
        private Boolean pushEnabled;
        private PushPreferences push;
        
        // Quiet Hours
        private QuietHoursSettings quietHours;
        
        // Frequency
        private EmailFrequency emailFrequency;
        private DigestFrequency digestFrequency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InAppPreferences {
        private Boolean system;
        private Boolean account;
        private Boolean resume;
        private Boolean interview;
        private Boolean job;
        private Boolean community;
        private Boolean growth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailPreferences {
        private Boolean system;
        private Boolean account;
        private Boolean resume;
        private Boolean interview;
        private Boolean job;
        private Boolean community;
        private Boolean growth;
        private Boolean marketing;
        private Boolean weeklyDigest;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PushPreferences {
        private Boolean system;
        private Boolean account;
        private Boolean resume;
        private Boolean interview;
        private Boolean job;
        private Boolean community;
        private Boolean growth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuietHoursSettings {
        private Boolean enabled;
        private String startTime;
        private String endTime;
        private String timezone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatePreferenceRequest {
        // Global toggles
        private Boolean inAppEnabled;
        private Boolean emailEnabled;
        private Boolean pushEnabled;
        
        // In-App categories
        private Boolean inAppSystem;
        private Boolean inAppAccount;
        private Boolean inAppResume;
        private Boolean inAppInterview;
        private Boolean inAppJob;
        private Boolean inAppCommunity;
        private Boolean inAppGrowth;
        
        // Email categories
        private Boolean emailSystem;
        private Boolean emailAccount;
        private Boolean emailResume;
        private Boolean emailInterview;
        private Boolean emailJob;
        private Boolean emailCommunity;
        private Boolean emailGrowth;
        private Boolean emailMarketing;
        private Boolean emailWeeklyDigest;
        
        // Push categories
        private Boolean pushSystem;
        private Boolean pushAccount;
        private Boolean pushResume;
        private Boolean pushInterview;
        private Boolean pushJob;
        private Boolean pushCommunity;
        private Boolean pushGrowth;
        
        // Quiet hours
        private Boolean quietHoursEnabled;
        private String quietHoursStart;
        private String quietHoursEnd;
        private String timezone;
        
        // Frequency
        private EmailFrequency emailFrequency;
        private DigestFrequency digestFrequency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickToggleRequest {
        private String channel; // in_app, email, push, all
        private String category; // system, account, resume, interview, job, community, growth
        private Boolean enabled;
    }
}
