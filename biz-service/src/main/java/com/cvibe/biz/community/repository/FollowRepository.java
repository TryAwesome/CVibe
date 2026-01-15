package com.cvibe.biz.community.repository;

import com.cvibe.biz.community.entity.Follow;
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
 * Repository for Follow entity
 */
@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    /**
     * Find follow relationship
     */
    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    /**
     * Check if following
     */
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    /**
     * Delete follow relationship
     */
    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    /**
     * Get followers of a user
     */
    @Query("SELECT f.follower.id FROM Follow f WHERE f.following.id = :userId")
    Page<UUID> findFollowerIds(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Get users followed by a user
     */
    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId")
    Page<UUID> findFollowingIds(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Count followers
     */
    long countByFollowingId(UUID userId);

    /**
     * Count following
     */
    long countByFollowerId(UUID userId);

    /**
     * Get mutual followers (users who follow each other)
     */
    @Query("SELECT f1.following.id FROM Follow f1 " +
           "WHERE f1.follower.id = :userId AND EXISTS " +
           "(SELECT f2 FROM Follow f2 WHERE f2.follower.id = f1.following.id AND f2.following.id = :userId)")
    List<UUID> findMutualFollowers(@Param("userId") UUID userId);

    /**
     * Check if multiple users are followed
     */
    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId AND f.following.id IN :userIds")
    List<UUID> findFollowedUserIds(@Param("userId") UUID userId, @Param("userIds") List<UUID> userIds);

    /**
     * Get suggested users to follow (friends of friends)
     */
    @Query("SELECT DISTINCT f2.following.id FROM Follow f1 " +
           "JOIN Follow f2 ON f1.following.id = f2.follower.id " +
           "WHERE f1.follower.id = :userId " +
           "AND f2.following.id != :userId " +
           "AND NOT EXISTS (SELECT f3 FROM Follow f3 WHERE f3.follower.id = :userId AND f3.following.id = f2.following.id)")
    List<UUID> findSuggestedUsers(@Param("userId") UUID userId, Pageable pageable);
}
