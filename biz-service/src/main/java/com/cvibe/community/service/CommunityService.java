package com.cvibe.community.service;

import com.cvibe.auth.entity.User;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import com.cvibe.community.dto.*;
import com.cvibe.community.entity.Comment;
import com.cvibe.community.entity.Post;
import com.cvibe.community.entity.PostCategory;
import com.cvibe.community.entity.PostLike;
import com.cvibe.community.repository.CommentRepository;
import com.cvibe.community.repository.PostLikeRepository;
import com.cvibe.community.repository.PostRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for community features including posts, comments, and likes.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ==================== Post Operations ====================

    /**
     * Create a new post.
     */
    @Transactional
    public PostDto createPost(UUID userId, CreatePostRequest request) {
        User user = findUserById(userId);

        Post post = Post.builder()
                .author(user)
                .content(request.getContent())
                .images(toJsonArray(request.getImages()))
                .tags(toJsonArray(request.getTags()))
                .category(request.getCategory())
                .build();

        post = postRepository.save(post);
        log.info("Created post {} by user {}", post.getId(), userId);
        return PostDto.fromEntity(post, false);
    }

    /**
     * Get a post by ID.
     */
    @Transactional(readOnly = true)
    public PostDto getPost(UUID postId, UUID userId) {
        Post post = postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        boolean liked = userId != null && postLikeRepository.existsByUserIdAndPostId(userId, postId);
        return PostDto.fromEntity(post, liked);
    }

    /**
     * Update a post.
     */
    @Transactional
    public PostDto updatePost(UUID userId, UUID postId, UpdatePostRequest request) {
        Post post = postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // Check ownership
        if (!post.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getImages() != null) {
            post.setImages(toJsonArray(request.getImages()));
        }
        if (request.getTags() != null) {
            post.setTags(toJsonArray(request.getTags()));
        }
        if (request.getCategory() != null) {
            post.setCategory(request.getCategory());
        }

        post = postRepository.save(post);
        log.info("Updated post {}", postId);

        boolean liked = postLikeRepository.existsByUserIdAndPostId(userId, postId);
        return PostDto.fromEntity(post, liked);
    }

    /**
     * Delete a post.
     */
    @Transactional
    public void deletePost(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // Check ownership
        if (!post.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // Delete related data first
        postLikeRepository.deleteByPostId(postId);
        commentRepository.deleteByPostId(postId);
        postRepository.delete(post);

        log.info("Deleted post {} by user {}", postId, userId);
    }

    /**
     * Get feed (all posts, paginated, newest first).
     */
    @Transactional(readOnly = true)
    public PagedResponse<PostDto> getFeed(UUID userId, int page, int size, PostCategory category) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts;

        if (category != null) {
            posts = postRepository.findByCategory(category, pageable);
        } else {
            posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        Set<UUID> likedPostIds = getUserLikedPostIds(userId, posts.getContent());
        List<PostDto> postDtos = posts.getContent().stream()
                .map(post -> PostDto.fromEntity(post, likedPostIds.contains(post.getId())))
                .collect(Collectors.toList());

        return PagedResponse.of(posts, postDtos);
    }

    /**
     * Get trending posts (ordered by likes).
     */
    @Transactional(readOnly = true)
    public PagedResponse<PostDto> getTrendingPosts(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findTrendingPosts(pageable);

        Set<UUID> likedPostIds = getUserLikedPostIds(userId, posts.getContent());
        List<PostDto> postDtos = posts.getContent().stream()
                .map(post -> PostDto.fromEntity(post, likedPostIds.contains(post.getId())))
                .collect(Collectors.toList());

        return PagedResponse.of(posts, postDtos);
    }

    /**
     * Search posts by keyword.
     */
    @Transactional(readOnly = true)
    public PagedResponse<PostDto> searchPosts(UUID userId, String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getFeed(userId, page, size, null);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.searchByKeyword(keyword.trim(), pageable);

        Set<UUID> likedPostIds = getUserLikedPostIds(userId, posts.getContent());
        List<PostDto> postDtos = posts.getContent().stream()
                .map(post -> PostDto.fromEntity(post, likedPostIds.contains(post.getId())))
                .collect(Collectors.toList());

        return PagedResponse.of(posts, postDtos);
    }

    // ==================== Like Operations ====================

    /**
     * Like a post.
     */
    @Transactional
    public void likePost(UUID userId, UUID postId) {
        if (postLikeRepository.existsByUserIdAndPostId(userId, postId)) {
            log.debug("User {} already liked post {}", userId, postId);
            return;
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        User user = findUserById(userId);

        PostLike like = PostLike.builder()
                .user(user)
                .post(post)
                .build();

        postLikeRepository.save(like);
        post.incrementLikes();
        postRepository.save(post);

        log.info("User {} liked post {}", userId, postId);
    }

    /**
     * Unlike a post.
     */
    @Transactional
    public void unlikePost(UUID userId, UUID postId) {
        if (!postLikeRepository.existsByUserIdAndPostId(userId, postId)) {
            log.debug("User {} has not liked post {}", userId, postId);
            return;
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        postLikeRepository.deleteByUserIdAndPostId(userId, postId);
        post.decrementLikes();
        postRepository.save(post);

        log.info("User {} unliked post {}", userId, postId);
    }

    // ==================== Comment Operations ====================

    /**
     * Add a comment to a post.
     */
    @Transactional
    public CommentDto addComment(UUID userId, UUID postId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        User user = findUserById(userId);

        Comment parent = null;
        if (request.getParentId() != null && !request.getParentId().isBlank()) {
            UUID parentId = UUID.fromString(request.getParentId());
            parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

            // Increment parent's reply count
            parent.incrementReplies();
            commentRepository.save(parent);
        }

        Comment comment = Comment.builder()
                .post(post)
                .author(user)
                .content(request.getContent())
                .parent(parent)
                .build();

        comment = commentRepository.save(comment);

        // Increment post's comment count
        post.incrementComments();
        postRepository.save(post);

        log.info("User {} added comment {} to post {}", userId, comment.getId(), postId);
        return CommentDto.fromEntity(comment);
    }

    /**
     * Get comments for a post (top-level comments with replies).
     */
    @Transactional(readOnly = true)
    public PagedResponse<CommentDto> getComments(UUID postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByPostIdAndParentIsNull(postId, pageable);

        List<CommentDto> commentDtos = comments.getContent().stream()
                .map(comment -> {
                    List<CommentDto> replies = commentRepository.findByParentId(comment.getId())
                            .stream()
                            .map(CommentDto::fromEntity)
                            .collect(Collectors.toList());
                    return CommentDto.fromEntity(comment, replies);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(comments, commentDtos);
    }

    /**
     * Delete a comment.
     */
    @Transactional
    public void deleteComment(UUID userId, UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        // Check ownership
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Post post = comment.getPost();
        Comment parent = comment.getParent();

        // If it's a reply, decrement parent's reply count
        if (parent != null) {
            parent.decrementReplies();
            commentRepository.save(parent);
        }

        commentRepository.delete(comment);

        // Decrement post's comment count
        post.decrementComments();
        postRepository.save(post);

        log.info("User {} deleted comment {}", userId, commentId);
    }

    // ==================== Helper Methods ====================

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Set<UUID> getUserLikedPostIds(UUID userId, List<Post> posts) {
        if (userId == null || posts.isEmpty()) {
            return Collections.emptySet();
        }

        List<UUID> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        return postLikeRepository.findLikedPostIdsByUserId(userId).stream()
                .filter(postIds::contains)
                .collect(Collectors.toSet());
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize list to JSON", e);
            return null;
        }
    }
}
