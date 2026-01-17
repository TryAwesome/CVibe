package com.cvibe.community.repository;

import com.cvibe.community.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Comment entity.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /**
     * Find top-level comments for a post (parent is null).
     */
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parent IS NULL ORDER BY c.createdAt ASC")
    Page<Comment> findByPostIdAndParentIsNull(@Param("postId") UUID postId, Pageable pageable);

    /**
     * Find replies to a comment.
     */
    @Query("SELECT c FROM Comment c WHERE c.parent.id = :parentId ORDER BY c.createdAt ASC")
    List<Comment> findByParentId(@Param("parentId") UUID parentId);

    /**
     * Delete all comments for a post.
     */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id = :postId")
    void deleteByPostId(@Param("postId") UUID postId);

    /**
     * Count comments for a post.
     */
    long countByPostId(UUID postId);

    /**
     * Find all comments by author.
     */
    Page<Comment> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);
}
