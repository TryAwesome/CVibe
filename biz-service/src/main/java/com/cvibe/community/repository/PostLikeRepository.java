package com.cvibe.community.repository;

import com.cvibe.community.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PostLike entity.
 */
@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    /**
     * Check if user has liked a post.
     */
    boolean existsByUserIdAndPostId(UUID userId, UUID postId);

    /**
     * Find like by user and post.
     */
    Optional<PostLike> findByUserIdAndPostId(UUID userId, UUID postId);

    /**
     * Find all post IDs liked by a user.
     */
    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId")
    List<UUID> findLikedPostIdsByUserId(@Param("userId") UUID userId);

    /**
     * Delete like by user and post.
     */
    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id = :postId")
    void deleteByUserIdAndPostId(@Param("userId") UUID userId, @Param("postId") UUID postId);

    /**
     * Delete all likes for a post.
     */
    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.post.id = :postId")
    void deleteByPostId(@Param("postId") UUID postId);

    /**
     * Count likes for a post.
     */
    long countByPostId(UUID postId);
}
