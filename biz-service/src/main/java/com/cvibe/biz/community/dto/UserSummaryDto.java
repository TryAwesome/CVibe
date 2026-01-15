package com.cvibe.biz.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Simple DTO for user info in community context
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {

    private UUID id;
    private String name;
    private String avatarUrl;
    private String headline;
    
    // Social stats
    private Long postCount;
    private Long followerCount;
    private Long followingCount;
    
    // Relationship with current user
    private Boolean isFollowing;
    private Boolean isFollower;
    
    private Instant joinedAt;
}
