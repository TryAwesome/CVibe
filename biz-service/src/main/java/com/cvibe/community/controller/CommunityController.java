package com.cvibe.community.controller;

import com.cvibe.common.dto.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import com.cvibe.community.dto.*;
import com.cvibe.community.entity.PostCategory;
import com.cvibe.community.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for community features.
 * 
 * API Base Path: /api/v1/community
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    // ==================== Post Endpoints ====================

    /**
     * Create a new post.
     * POST /api/v1/community/posts
     */
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<PostDto>> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePostRequest request) {
        log.info("Creating post for user: {}", principal.getId());
        PostDto post = communityService.createPost(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    /**
     * Get a post by ID.
     * GET /api/v1/community/posts/{postId}
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostDto>> getPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId) {
        UUID userId = principal != null ? principal.getId() : null;
        PostDto post = communityService.getPost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    /**
     * Update a post.
     * PUT /api/v1/community/posts/{postId}
     */
    @PutMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostDto>> updatePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request) {
        log.info("Updating post {} by user {}", postId, principal.getId());
        PostDto post = communityService.updatePost(principal.getId(), postId, request);
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    /**
     * Delete a post.
     * DELETE /api/v1/community/posts/{postId}
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId) {
        log.info("Deleting post {} by user {}", postId, principal.getId());
        communityService.deletePost(principal.getId(), postId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Get feed (paginated posts).
     * GET /api/v1/community/feed
     */
    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<PagedResponse<PostDto>>> getFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) PostCategory category) {
        UUID userId = principal != null ? principal.getId() : null;
        PagedResponse<PostDto> feed = communityService.getFeed(userId, page, size, category);
        return ResponseEntity.ok(ApiResponse.success(feed));
    }

    /**
     * Get trending posts.
     * GET /api/v1/community/posts/trending
     */
    @GetMapping("/posts/trending")
    public ResponseEntity<ApiResponse<PagedResponse<PostDto>>> getTrendingPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = principal != null ? principal.getId() : null;
        PagedResponse<PostDto> posts = communityService.getTrendingPosts(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    /**
     * Search posts by keyword.
     * GET /api/v1/community/posts/search
     */
    @GetMapping("/posts/search")
    public ResponseEntity<ApiResponse<PagedResponse<PostDto>>> searchPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = principal != null ? principal.getId() : null;
        PagedResponse<PostDto> posts = communityService.searchPosts(userId, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    // ==================== Like Endpoints ====================

    /**
     * Like a post.
     * POST /api/v1/community/posts/{postId}/like
     */
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<Void>> likePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId) {
        log.info("User {} liking post {}", principal.getId(), postId);
        communityService.likePost(principal.getId(), postId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Unlike a post.
     * DELETE /api/v1/community/posts/{postId}/like
     */
    @DeleteMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<Void>> unlikePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId) {
        log.info("User {} unliking post {}", principal.getId(), postId);
        communityService.unlikePost(principal.getId(), postId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== Comment Endpoints ====================

    /**
     * Add a comment to a post.
     * POST /api/v1/community/posts/{postId}/comments
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentDto>> addComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {
        log.info("User {} adding comment to post {}", principal.getId(), postId);
        CommentDto comment = communityService.addComment(principal.getId(), postId, request);
        return ResponseEntity.ok(ApiResponse.success(comment));
    }

    /**
     * Get comments for a post.
     * GET /api/v1/community/posts/{postId}/comments
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PagedResponse<CommentDto>>> getComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PagedResponse<CommentDto> comments = communityService.getComments(postId, page, size);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    /**
     * Delete a comment.
     * DELETE /api/v1/community/comments/{commentId}
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID commentId) {
        log.info("User {} deleting comment {}", principal.getId(), commentId);
        communityService.deleteComment(principal.getId(), commentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
