package com.cvibe.biz.community.dto;

import com.cvibe.biz.community.entity.Post.PostCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for community dashboard summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunitySummary {

    // User stats
    private Long myPostCount;
    private Long myCommentCount;
    private Long totalLikesReceived;
    private Long followerCount;
    private Long followingCount;
    
    // Community stats
    private Long totalPosts;
    private Long totalUsers;
    private Long postsToday;
    
    // Distribution
    private Map<PostCategory, Long> categoryDistribution;
    private List<String> popularTags;
    
    // Content
    private List<PostDto> trendingPosts;
    private List<PostDto> recentPosts;
    private List<UserSummaryDto> suggestedUsers;
}
