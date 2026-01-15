package com.cvibe.biz.community.repository;

import com.cvibe.biz.community.entity.Post;
import com.cvibe.biz.community.entity.Post.PostCategory;
import com.cvibe.biz.community.entity.Post.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Post entity
 */
@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    // ================== Basic Queries ==================

    /**
     * Find posts by author
     */
    Page<Post> findByAuthorIdAndStatusOrderByCreatedAtDesc(UUID authorId, PostStatus status, Pageable pageable);

    /**
     * Find published posts by author
     */
    default Page<Post> findPublishedByAuthor(UUID authorId, Pageable pageable) {
        return findByAuthorIdAndStatusOrderByCreatedAtDesc(authorId, PostStatus.PUBLISHED, pageable);
    }

    /**
     * Find posts by category
     */
    Page<Post> findByCategoryAndStatusOrderByCreatedAtDesc(PostCategory category, PostStatus status, Pageable pageable);

    /**
     * Find all published posts
     */
    Page<Post> findByStatusOrderByCreatedAtDesc(PostStatus status, Pageable pageable);

    /**
     * Find pinned posts
     */
    List<Post> findByIsPinnedTrueAndStatusOrderByCreatedAtDesc(PostStatus status);

    // ================== Feed Queries ==================

    /**
     * Get feed for user (posts from followed users)
     */
    @Query("SELECT p FROM Post p WHERE p.author.id IN " +
           "(SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId) " +
           "AND p.status = 'PUBLISHED' ORDER BY p.createdAt DESC")
    Page<Post> findFeedForUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Get trending posts (by engagement)
     */
    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' " +
           "AND p.createdAt > :since " +
           "ORDER BY (p.likeCount * 2 + p.commentCount * 3 + p.viewCount) DESC")
    Page<Post> findTrending(@Param("since") Instant since, Pageable pageable);

    /**
     * Get popular posts (most liked)
     */
    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' " +
           "ORDER BY p.likeCount DESC, p.createdAt DESC")
    Page<Post> findPopular(Pageable pageable);

    // ================== Search ==================

    /**
     * Search posts by keyword
     */
    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' " +
           "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> search(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Search by tag
     */
    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' " +
           "AND LOWER(p.tags) LIKE LOWER(CONCAT('%', :tag, '%')) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findByTag(@Param("tag") String tag, Pageable pageable);

    // ================== Statistics ==================

    /**
     * Count posts by author
     */
    long countByAuthorIdAndStatus(UUID authorId, PostStatus status);

    /**
     * Count posts by category
     */
    long countByCategoryAndStatus(PostCategory category, PostStatus status);

    /**
     * Get popular tags
     */
    @Query(value = "SELECT UNNEST(STRING_TO_ARRAY(tags, ',')) as tag, COUNT(*) as cnt " +
                   "FROM posts WHERE status = 'PUBLISHED' AND tags IS NOT NULL " +
                   "GROUP BY tag ORDER BY cnt DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> getPopularTags(@Param("limit") int limit);

    /**
     * Get category distribution
     */
    @Query("SELECT p.category, COUNT(p) FROM Post p WHERE p.status = 'PUBLISHED' GROUP BY p.category")
    List<Object[]> getCategoryDistribution();

    /**
     * Count posts in date range
     */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.status = 'PUBLISHED' " +
           "AND p.createdAt BETWEEN :start AND :end")
    long countInDateRange(@Param("start") Instant start, @Param("end") Instant end);
}
