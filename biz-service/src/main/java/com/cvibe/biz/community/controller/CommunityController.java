package com.cvibe.biz.community.controller;

import com.cvibe.biz.community.dto.*;
import com.cvibe.biz.community.entity.Post.PostCategory;
import com.cvibe.biz.community.service.CommunityService;
import com.cvibe.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Community Controller
 * 
 * REST API for community features: posts, comments, likes, and follows.
 */
@RestController
@RequestMapping("/v1/community")
@RequiredArgsConstructor
@Tag(name = "Community", description = "Community features: posts, comments, likes, follows")
public class CommunityController {

    private final CommunityService communityService;

    // ================== Posts ==================

    @PostMapping("/posts")
    @Operation(summary = "Create post", description = "Create a new community post")
    public ApiResponse<PostDto> createPost(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.success(communityService.createPost(userId, request));
    }

    @GetMapping("/posts/{postId}")
    @Operation(summary = "Get post", description = "Get a post by ID")
    public ApiResponse<PostDto> getPost(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Post ID") @PathVariable UUID postId) {
        return ApiResponse.success(communityService.getPost(postId, userId));
    }

    @PutMapping("/posts/{postId}")
    @Operation(summary = "Update post", description = "Update an existing post")
    public ApiResponse<PostDto> updatePost(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.success(communityService.updatePost(userId, postId, request));
    }

    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "Delete post", description = "Delete a post")
    public ApiResponse<Void> deletePost(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Post ID") @PathVariable UUID postId) {
        communityService.deletePost(userId, postId);
        return ApiResponse.success(null);
    }

    @GetMapping("/feed")
    @Operation(summary = "Get feed", description = "Get posts from followed users")
    public ApiResponse<Page<PostDto>> getFeed(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(communityService.getFeed(userId, page, size));
    }

    @GetMapping("/posts/trending")
    @Operation(summary = "Get trending posts", description = "Get trending posts based on engagement")
    public ApiResponse<Page<PostDto>> getTrending(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(communityService.getTrending(userId, page, size));
    }

    @GetMapping("/posts/category/{category}")
    @Operation(summary = "Get posts by category", description = "Get posts in a specific category")
    public ApiResponse<Page<PostDto>> getByCategory(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Category") @PathVariable PostCategory category,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(communityService.getByCategory(category, userId, page, size));
    }

    @GetMapping("/posts/search")
    @Operation(summary = "Search posts", description = "Search posts by keyword")
    public ApiResponse<Page<PostDto>> searchPosts(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Search keyword") @RequestParam String q,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(communityService.searchPosts(q, userId, page, size));
    }

    @GetMapping("/users/{authorId}/posts")
    @Operation(summary = "Get user posts", description = "Get posts by a specific user")
    public ApiResponse<Page<PostDto>> getUserPosts(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Author ID") @PathVariable UUID authorId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(communityService.getUserPosts(authorId, userId, page, size));
    }

    // ================== Comments ==================

    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "Add comment", description = "Add a comment to a post")
    public ApiResponse<CommentDto> addComment(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.success(communityService.addComment(userId, postId, request));
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "Get comments", description = "Get comments for a post")
    public ApiResponse<Page<CommentDto>> getComments(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Post ID") @PathVariable UUID postId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(communityService.getComments(postId, userId, page, size));
    }

    @PutMapping("/comments/{commentId}")
    @Operation(summary = "Edit comment", description = "Edit a comment")
    public ApiResponse<CommentDto> editComment(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Comment ID") @PathVariable UUID commentId,
            @RequestBody String content) {
        return ApiResponse.success(communityService.editComment(userId, commentId, content));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete comment", description = "Delete a comment")
    public ApiResponse<Void> deleteComment(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Comment ID") @PathVariable UUID commentId) {
        communityService.deleteComment(userId, commentId);
        return ApiResponse.success(null);
    }

    // ================== Likes ==================

    @PostMapping("/posts/{postId}/like")
    @Operation(summary = "Like post", description = "Like a post")
    public ApiResponse<Void> likePost(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Post ID") @PathVariable UUID postId) {
        communityService.likePost(userId, postId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/posts/{postId}/like")
    @Operation(summary = "Unlike post", description = "Remove like from a post")
    public ApiResponse<Void> unlikePost(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Post ID") @PathVariable UUID postId) {
        communityService.unlikePost(userId, postId);
        return ApiResponse.success(null);
    }

    @PostMapping("/comments/{commentId}/like")
    @Operation(summary = "Like comment", description = "Like a comment")
    public ApiResponse<Void> likeComment(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Comment ID") @PathVariable UUID commentId) {
        communityService.likeComment(userId, commentId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/comments/{commentId}/like")
    @Operation(summary = "Unlike comment", description = "Remove like from a comment")
    public ApiResponse<Void> unlikeComment(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Comment ID") @PathVariable UUID commentId) {
        communityService.unlikeComment(userId, commentId);
        return ApiResponse.success(null);
    }

    // ================== Follows ==================

    @PostMapping("/users/{targetId}/follow")
    @Operation(summary = "Follow user", description = "Follow a user")
    public ApiResponse<Void> follow(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "User ID to follow") @PathVariable UUID targetId) {
        communityService.follow(userId, targetId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/users/{targetId}/follow")
    @Operation(summary = "Unfollow user", description = "Unfollow a user")
    public ApiResponse<Void> unfollow(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "User ID to unfollow") @PathVariable UUID targetId) {
        communityService.unfollow(userId, targetId);
        return ApiResponse.success(null);
    }

    @GetMapping("/users/{targetId}/followers")
    @Operation(summary = "Get followers", description = "Get followers of a user")
    public ApiResponse<Page<UserSummaryDto>> getFollowers(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "User ID") @PathVariable UUID targetId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(communityService.getFollowers(targetId, userId, page, size));
    }

    @GetMapping("/users/{targetId}/following")
    @Operation(summary = "Get following", description = "Get users followed by a user")
    public ApiResponse<Page<UserSummaryDto>> getFollowing(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "User ID") @PathVariable UUID targetId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(communityService.getFollowing(targetId, userId, page, size));
    }

    // ================== Dashboard ==================

    @GetMapping("/summary")
    @Operation(summary = "Get community summary", description = "Get community statistics and activity summary")
    public ApiResponse<CommunitySummary> getSummary(@AuthenticationPrincipal UUID userId) {
        return ApiResponse.success(communityService.getSummary(userId));
    }
}
