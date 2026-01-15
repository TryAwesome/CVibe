package com.cvibe.biz.community.service;

import com.cvibe.biz.community.dto.*;
import com.cvibe.biz.community.entity.*;
import com.cvibe.biz.community.entity.Post.PostCategory;
import com.cvibe.biz.community.entity.Post.PostStatus;
import com.cvibe.biz.community.repository.*;
import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CommunityService
 * 
 * Handles community features: posts, comments, likes, and follows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    // ================== Posts ==================

    /**
     * Create a new post
     */
    @Transactional
    public PostDto createPost(UUID userId, CreatePostRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.builder()
                .author(author)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .coverImageUrl(request.getCoverImageUrl())
                .allowComments(request.getAllowComments())
                .status(Boolean.TRUE.equals(request.getIsDraft()) ? PostStatus.DRAFT : PostStatus.PUBLISHED)
                .build();

        if (request.getTags() != null) {
            post.setTagList(request.getTags());
        }

        post = postRepository.save(post);
        log.info("Created post {} by user {}", post.getId(), userId);

        return PostDto.from(post, userId, false);
    }

    /**
     * Get post by ID
     */
    @Transactional
    public PostDto getPost(UUID postId, UUID currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.isPublished() && !post.belongsToUser(currentUserId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // Increment view count
        post.incrementViews();
        postRepository.save(post);

        boolean isLiked = currentUserId != null && postLikeRepository.existsByPostIdAndUserId(postId, currentUserId);
        return PostDto.from(post, currentUserId, isLiked);
    }

    /**
     * Update a post
     */
    @Transactional
    public PostDto updatePost(UUID userId, UUID postId, CreatePostRequest request) {
        Post post = getPostForUser(userId, postId);

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(request.getCategory());
        post.setCoverImageUrl(request.getCoverImageUrl());
        post.setAllowComments(request.getAllowComments());
        if (request.getTags() != null) {
            post.setTagList(request.getTags());
        }

        if (Boolean.TRUE.equals(request.getIsDraft())) {
            post.setStatus(PostStatus.DRAFT);
        } else if (post.getStatus() == PostStatus.DRAFT) {
            post.publish();
        }

        post = postRepository.save(post);
        return PostDto.from(post, userId, false);
    }

    /**
     * Delete a post
     */
    @Transactional
    public void deletePost(UUID userId, UUID postId) {
        Post post = getPostForUser(userId, postId);
        post.delete();
        postRepository.save(post);
        log.info("Deleted post {} by user {}", postId, userId);
    }

    /**
     * Get feed (posts from followed users)
     */
    @Transactional(readOnly = true)
    public Page<PostDto> getFeed(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findFeedForUser(userId, pageable);
        
        List<UUID> postIds = posts.getContent().stream().map(Post::getId).collect(Collectors.toList());
        Set<UUID> likedPostIds = new HashSet<>(postLikeRepository.findLikedPostIds(userId, postIds));

        return posts.map(p -> PostDto.preview(p, userId, likedPostIds.contains(p.getId())));
    }

    /**
     * Get trending posts
     */
    @Transactional(readOnly = true)
    public Page<PostDto> getTrending(UUID currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        Page<Post> posts = postRepository.findTrending(since, pageable);

        List<UUID> postIds = posts.getContent().stream().map(Post::getId).collect(Collectors.toList());
        Set<UUID> likedPostIds = currentUserId != null 
                ? new HashSet<>(postLikeRepository.findLikedPostIds(currentUserId, postIds))
                : Set.of();

        return posts.map(p -> PostDto.preview(p, currentUserId, likedPostIds.contains(p.getId())));
    }

    /**
     * Get posts by category
     */
    @Transactional(readOnly = true)
    public Page<PostDto> getByCategory(PostCategory category, UUID currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByCategoryAndStatusOrderByCreatedAtDesc(
                category, PostStatus.PUBLISHED, pageable);

        List<UUID> postIds = posts.getContent().stream().map(Post::getId).collect(Collectors.toList());
        Set<UUID> likedPostIds = currentUserId != null 
                ? new HashSet<>(postLikeRepository.findLikedPostIds(currentUserId, postIds))
                : Set.of();

        return posts.map(p -> PostDto.preview(p, currentUserId, likedPostIds.contains(p.getId())));
    }

    /**
     * Search posts
     */
    @Transactional(readOnly = true)
    public Page<PostDto> searchPosts(String keyword, UUID currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.search(keyword, pageable);

        List<UUID> postIds = posts.getContent().stream().map(Post::getId).collect(Collectors.toList());
        Set<UUID> likedPostIds = currentUserId != null 
                ? new HashSet<>(postLikeRepository.findLikedPostIds(currentUserId, postIds))
                : Set.of();

        return posts.map(p -> PostDto.preview(p, currentUserId, likedPostIds.contains(p.getId())));
    }

    /**
     * Get user's posts
     */
    @Transactional(readOnly = true)
    public Page<PostDto> getUserPosts(UUID authorId, UUID currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findPublishedByAuthor(authorId, pageable);

        List<UUID> postIds = posts.getContent().stream().map(Post::getId).collect(Collectors.toList());
        Set<UUID> likedPostIds = currentUserId != null 
                ? new HashSet<>(postLikeRepository.findLikedPostIds(currentUserId, postIds))
                : Set.of();

        return posts.map(p -> PostDto.preview(p, currentUserId, likedPostIds.contains(p.getId())));
    }

    // ================== Comments ==================

    /**
     * Add a comment to a post
     */
    @Transactional
    public CommentDto addComment(UUID userId, UUID postId, CreateCommentRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getAllowComments()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Comments are disabled for this post");
        }

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        }

        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .parent(parent)
                .content(request.getContent())
                .build();

        comment = commentRepository.save(comment);

        // Update counts
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        if (parent != null) {
            parent.setReplyCount(parent.getReplyCount() + 1);
            commentRepository.save(parent);
        }

        log.info("Added comment {} to post {} by user {}", comment.getId(), postId, userId);
        return CommentDto.from(comment, userId, false);
    }

    /**
     * Get comments for a post
     */
    @Transactional(readOnly = true)
    public Page<CommentDto> getComments(UUID postId, UUID currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findTopLevelByPostId(postId, pageable);

        List<UUID> commentIds = comments.getContent().stream().map(Comment::getId).collect(Collectors.toList());
        Set<UUID> likedCommentIds = currentUserId != null 
                ? new HashSet<>(commentLikeRepository.findLikedCommentIds(currentUserId, commentIds))
                : Set.of();

        return comments.map(c -> CommentDto.withReplies(c, currentUserId, likedCommentIds.contains(c.getId())));
    }

    /**
     * Edit a comment
     */
    @Transactional
    public CommentDto editComment(UUID userId, UUID commentId, String newContent) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        comment.edit(newContent);
        comment = commentRepository.save(comment);
        return CommentDto.from(comment, userId, false);
    }

    /**
     * Delete a comment
     */
    @Transactional
    public void deleteComment(UUID userId, UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        comment.softDelete();
        commentRepository.save(comment);

        // Update post comment count
        Post post = comment.getPost();
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);
    }

    // ================== Likes ==================

    /**
     * Like a post
     */
    @Transactional
    public void likePost(UUID userId, UUID postId) {
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        PostLike like = PostLike.builder()
                .post(post)
                .user(user)
                .build();
        postLikeRepository.save(like);

        post.incrementLikes();
        postRepository.save(post);
    }

    /**
     * Unlike a post
     */
    @Transactional
    public void unlikePost(UUID userId, UUID postId) {
        if (!postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new BusinessException(ErrorCode.NOT_LIKED_YET);
        }

        postLikeRepository.deleteByPostIdAndUserId(postId, userId);

        Post post = postRepository.findById(postId).orElse(null);
        if (post != null) {
            post.decrementLikes();
            postRepository.save(post);
        }
    }

    /**
     * Like a comment
     */
    @Transactional
    public void likeComment(UUID userId, UUID commentId) {
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        CommentLike like = CommentLike.builder()
                .comment(comment)
                .user(user)
                .build();
        commentLikeRepository.save(like);

        comment.incrementLikes();
        commentRepository.save(comment);
    }

    /**
     * Unlike a comment
     */
    @Transactional
    public void unlikeComment(UUID userId, UUID commentId) {
        if (!commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw new BusinessException(ErrorCode.NOT_LIKED_YET);
        }

        commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);

        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null) {
            comment.decrementLikes();
            commentRepository.save(comment);
        }
    }

    // ================== Follows ==================

    /**
     * Follow a user
     */
    @Transactional
    public void follow(UUID followerId, UUID followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Cannot follow yourself");
        }

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            return;  // Already following
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();
        followRepository.save(follow);

        log.info("User {} followed user {}", followerId, followingId);
    }

    /**
     * Unfollow a user
     */
    @Transactional
    public void unfollow(UUID followerId, UUID followingId) {
        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    /**
     * Get followers
     */
    @Transactional(readOnly = true)
    public Page<UserSummaryDto> getFollowers(UUID userId, UUID currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UUID> followerIds = followRepository.findFollowerIds(userId, pageable);
        
        return followerIds.map(id -> getUserSummary(id, currentUserId));
    }

    /**
     * Get following
     */
    @Transactional(readOnly = true)
    public Page<UserSummaryDto> getFollowing(UUID userId, UUID currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UUID> followingIds = followRepository.findFollowingIds(userId, pageable);
        
        return followingIds.map(id -> getUserSummary(id, currentUserId));
    }

    /**
     * Check if following
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    // ================== Dashboard ==================

    /**
     * Get community summary for dashboard
     */
    @Transactional(readOnly = true)
    public CommunitySummary getSummary(UUID userId) {
        long myPostCount = postRepository.countByAuthorIdAndStatus(userId, PostStatus.PUBLISHED);
        long myCommentCount = commentRepository.countByAuthorIdAndIsDeletedFalse(userId);
        long followerCount = followRepository.countByFollowingId(userId);
        long followingCount = followRepository.countByFollowerId(userId);

        Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long postsToday = postRepository.countInDateRange(today, Instant.now());

        // Category distribution
        Map<PostCategory, Long> categoryDist = postRepository.getCategoryDistribution().stream()
                .collect(Collectors.toMap(
                        arr -> (PostCategory) arr[0],
                        arr -> (Long) arr[1]
                ));

        // Trending posts
        List<PostDto> trending = postRepository.findTrending(
                        Instant.now().minus(7, ChronoUnit.DAYS), PageRequest.of(0, 5))
                .map(p -> PostDto.preview(p, userId, false))
                .getContent();

        // Recent posts
        List<PostDto> recent = postRepository.findByStatusOrderByCreatedAtDesc(
                        PostStatus.PUBLISHED, PageRequest.of(0, 5))
                .map(p -> PostDto.preview(p, userId, false))
                .getContent();

        return CommunitySummary.builder()
                .myPostCount(myPostCount)
                .myCommentCount(myCommentCount)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .postsToday(postsToday)
                .categoryDistribution(categoryDist)
                .trendingPosts(trending)
                .recentPosts(recent)
                .build();
    }

    // ================== Private Helper Methods ==================

    private Post getPostForUser(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return post;
    }

    private UserSummaryDto getUserSummary(UUID userId, UUID currentUserId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        long postCount = postRepository.countByAuthorIdAndStatus(userId, PostStatus.PUBLISHED);
        long followerCount = followRepository.countByFollowingId(userId);
        long followingCount = followRepository.countByFollowerId(userId);

        boolean isFollowing = currentUserId != null && 
                followRepository.existsByFollowerIdAndFollowingId(currentUserId, userId);
        boolean isFollower = currentUserId != null && 
                followRepository.existsByFollowerIdAndFollowingId(userId, currentUserId);

        return UserSummaryDto.builder()
                .id(user.getId())
                .name(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .postCount(postCount)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .isFollowing(isFollowing)
                .isFollower(isFollower)
                .joinedAt(user.getCreatedAt())
                .build();
    }
}
