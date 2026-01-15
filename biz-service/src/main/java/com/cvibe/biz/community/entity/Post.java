package com.cvibe.biz.community.entity;

import com.cvibe.biz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Post Entity
 * 
 * Represents a community post for sharing experiences, tips, and discussions.
 */
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_author_id", columnList = "author_id"),
        @Index(name = "idx_posts_category", columnList = "category"),
        @Index(name = "idx_posts_status", columnList = "status"),
        @Index(name = "idx_posts_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @ToString.Exclude
    private User author;

    /**
     * Post title
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Post content (supports markdown)
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Post category
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private PostCategory category;

    /**
     * Tags (comma-separated)
     */
    @Column(name = "tags")
    private String tags;

    /**
     * Cover image URL
     */
    @Column(name = "cover_image_url", length = 512)
    private String coverImageUrl;

    /**
     * Post status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PostStatus status = PostStatus.PUBLISHED;

    /**
     * View count
     */
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * Like count (denormalized for performance)
     */
    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    /**
     * Comment count (denormalized)
     */
    @Column(name = "comment_count")
    @Builder.Default
    private Integer commentCount = 0;

    /**
     * Share count
     */
    @Column(name = "share_count")
    @Builder.Default
    private Integer shareCount = 0;

    /**
     * Is pinned (featured)
     */
    @Column(name = "is_pinned")
    @Builder.Default
    private Boolean isPinned = false;

    /**
     * Allow comments
     */
    @Column(name = "allow_comments")
    @Builder.Default
    private Boolean allowComments = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ================== Relationships ==================

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<PostLike> likes = new ArrayList<>();

    // ================== Enums ==================

    public enum PostCategory {
        INTERVIEW_TIPS,      // Interview preparation tips
        CAREER_ADVICE,       // Career development advice
        JOB_SEARCH,          // Job search strategies
        RESUME_TIPS,         // Resume writing tips
        OFFER_NEGOTIATION,   // Salary/offer negotiation
        COMPANY_REVIEW,      // Company reviews
        SUCCESS_STORY,       // Success stories
        QUESTION,            // Questions seeking advice
        DISCUSSION,          // General discussion
        RESOURCE             // Shared resources
    }

    public enum PostStatus {
        DRAFT,
        PUBLISHED,
        HIDDEN,
        DELETED
    }

    // ================== Helper Methods ==================

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setPost(this);
        this.commentCount++;
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
        this.commentCount = Math.max(0, this.commentCount - 1);
    }

    public void incrementViews() {
        this.viewCount++;
    }

    public void incrementLikes() {
        this.likeCount++;
    }

    public void decrementLikes() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    public void incrementShares() {
        this.shareCount++;
    }

    public void publish() {
        this.status = PostStatus.PUBLISHED;
    }

    public void hide() {
        this.status = PostStatus.HIDDEN;
    }

    public void delete() {
        this.status = PostStatus.DELETED;
    }

    public void pin() {
        this.isPinned = true;
    }

    public void unpin() {
        this.isPinned = false;
    }

    public boolean isPublished() {
        return status == PostStatus.PUBLISHED;
    }

    public boolean belongsToUser(UUID userId) {
        return author != null && author.getId().equals(userId);
    }

    public List<String> getTagList() {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return List.of(tags.split(","));
    }

    public void setTagList(List<String> tagList) {
        this.tags = tagList != null ? String.join(",", tagList) : null;
    }
}
