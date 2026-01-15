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
 * Comment Entity
 * 
 * Represents a comment on a post, supporting nested replies.
 */
@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_post_id", columnList = "post_id"),
        @Index(name = "idx_comments_author_id", columnList = "author_id"),
        @Index(name = "idx_comments_parent_id", columnList = "parent_id"),
        @Index(name = "idx_comments_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @ToString.Exclude
    private User author;

    /**
     * Parent comment (for nested replies)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    private Comment parent;

    /**
     * Comment content
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Like count
     */
    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    /**
     * Reply count
     */
    @Column(name = "reply_count")
    @Builder.Default
    private Integer replyCount = 0;

    /**
     * Is edited
     */
    @Column(name = "is_edited")
    @Builder.Default
    private Boolean isEdited = false;

    /**
     * Is deleted (soft delete)
     */
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ================== Relationships ==================

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    private List<Comment> replies = new ArrayList<>();

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<CommentLike> likes = new ArrayList<>();

    // ================== Helper Methods ==================

    public void addReply(Comment reply) {
        replies.add(reply);
        reply.setParent(this);
        this.replyCount++;
    }

    public void incrementLikes() {
        this.likeCount++;
    }

    public void decrementLikes() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    public void edit(String newContent) {
        this.content = newContent;
        this.isEdited = true;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.content = "[已删除]";
    }

    public boolean belongsToUser(UUID userId) {
        return author != null && author.getId().equals(userId);
    }

    public boolean isTopLevel() {
        return parent == null;
    }

    public int getDepth() {
        int depth = 0;
        Comment current = this.parent;
        while (current != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }
}
