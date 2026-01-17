package com.cvibe.community.repository;

import com.cvibe.community.entity.Post;
import com.cvibe.community.entity.PostCategory;
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
 * Repository for Post entity.
 */
@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    /**
     * Find posts by category with pagination.
     */
    Page<Post> findByCategory(PostCategory category, Pageable pageable);

    /**
     * Find all posts ordered by creation date (newest first).
     */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find posts by author ID.
     */
    Page<Post> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    /**
     * Find trending posts (ordered by likes count).
     */
    @Query("SELECT p FROM Post p ORDER BY p.likesCount DESC, p.createdAt DESC")
    Page<Post> findTrendingPosts(Pageable pageable);

    /**
     * Search posts by content or tags containing keyword.
     */
    @Query("SELECT p FROM Post p WHERE " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.tags) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find post with author eagerly loaded.
     */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author WHERE p.id = :postId")
    Optional<Post> findByIdWithAuthor(@Param("postId") UUID postId);

    /**
     * Find posts by multiple IDs.
     */
    List<Post> findByIdIn(List<UUID> ids);
}
