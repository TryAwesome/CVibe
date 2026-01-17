package com.cvibe.community.entity;

import com.cvibe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Like entity for community posts.
 * Represents a user liking a post.
 */
@Entity
@Table(name = "community_post_likes", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_like_user_post", columnNames = {"user_id", "post_id"})
    },
    indexes = {
        @Index(name = "idx_post_like_user", columnList = "user_id"),
        @Index(name = "idx_post_like_post", columnList = "post_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
