package com.cvibe.biz.community.repository;

import com.cvibe.biz.community.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CommentLike entity
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

    /**
     * Find like by comment and user
     */
    Optional<CommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId);

    /**
     * Check if user liked a comment
     */
    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    /**
     * Delete like by comment and user
     */
    void deleteByCommentIdAndUserId(UUID commentId, UUID userId);

    /**
     * Count likes for a comment
     */
    long countByCommentId(UUID commentId);

    /**
     * Check if user liked multiple comments
     */
    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id IN :commentIds")
    List<UUID> findLikedCommentIds(@Param("userId") UUID userId, @Param("commentIds") List<UUID> commentIds);
}
