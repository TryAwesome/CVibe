package com.cvibe.biz.community.repository;

import com.cvibe.biz.community.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Comment entity
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /**
     * Find top-level comments for a post
     */
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parent IS NULL " +
           "AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findTopLevelByPostId(@Param("postId") UUID postId, Pageable pageable);

    /**
     * Find replies for a comment
     */
    @Query("SELECT c FROM Comment c WHERE c.parent.id = :parentId " +
           "AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(@Param("parentId") UUID parentId);

    /**
     * Find all comments for a post (including replies)
     */
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId " +
           "AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findAllByPostId(@Param("postId") UUID postId);

    /**
     * Find comments by author
     */
    Page<Comment> findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    /**
     * Count comments for a post
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.isDeleted = false")
    long countByPostId(@Param("postId") UUID postId);

    /**
     * Count replies for a comment
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.parent.id = :parentId AND c.isDeleted = false")
    long countReplies(@Param("parentId") UUID parentId);

    /**
     * Count comments by author
     */
    long countByAuthorIdAndIsDeletedFalse(UUID authorId);

    /**
     * Find recent comments by user
     */
    @Query("SELECT c FROM Comment c WHERE c.author.id = :userId AND c.isDeleted = false " +
           "ORDER BY c.createdAt DESC")
    List<Comment> findRecentByAuthor(@Param("userId") UUID userId, Pageable pageable);
}
