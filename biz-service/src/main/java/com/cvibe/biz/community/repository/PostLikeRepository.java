package com.cvibe.biz.community.repository;

import com.cvibe.biz.community.entity.PostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PostLike entity
 */
@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    /**
     * Find like by post and user
     */
    Optional<PostLike> findByPostIdAndUserId(UUID postId, UUID userId);

    /**
     * Check if user liked a post
     */
    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    /**
     * Delete like by post and user
     */
    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    /**
     * Count likes for a post
     */
    long countByPostId(UUID postId);

    /**
     * Find users who liked a post
     */
    @Query("SELECT pl.user.id FROM PostLike pl WHERE pl.post.id = :postId")
    List<UUID> findUserIdsWhoLiked(@Param("postId") UUID postId, Pageable pageable);

    /**
     * Find posts liked by user
     */
    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId ORDER BY pl.createdAt DESC")
    Page<UUID> findPostIdsLikedByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Check if user liked multiple posts
     */
    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id IN :postIds")
    List<UUID> findLikedPostIds(@Param("userId") UUID userId, @Param("postIds") List<UUID> postIds);
}
