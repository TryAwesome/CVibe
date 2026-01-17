package com.cvibe.community.entity;

import com.cvibe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Comment entity for community posts.
 */
@Entity
@Table(name = "community_comments", indexes = {
    @Index(name = "idx_comment_post", columnList = "post_id"),
    @Index(name = "idx_comment_author", columnList = "author_id"),
    @Index(name = "idx_comment_parent", columnList = "parent_id"),
    @Index(name = "idx_comment_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Parent comment for replies (null for top-level comments)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(name = "replies_count", nullable = false)
    @Builder.Default
    private Integer repliesCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Increment replies count
     */
    public void incrementReplies() {
        this.repliesCount = (this.repliesCount == null ? 0 : this.repliesCount) + 1;
    }

    /**
     * Decrement replies count
     */
    public void decrementReplies() {
        this.repliesCount = Math.max(0, (this.repliesCount == null ? 0 : this.repliesCount) - 1);
    }
}
