package com.cvibe.biz.user.repository;

import com.cvibe.biz.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * User Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by Google Sub ID
     */
    Optional<User> findByGoogleSub(String googleSub);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if Google Sub exists
     */
    boolean existsByGoogleSub(String googleSub);

    /**
     * Find all enabled users with pagination
     */
    Page<User> findByEnabledTrue(Pageable pageable);

    /**
     * Search users by email or name
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Update last login time
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginAt WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") UUID userId, @Param("loginAt") Instant loginAt);

    /**
     * Count users by role
     */
    long countByRole(User.UserRole role);

    /**
     * Count users created after a date
     */
    long countByCreatedAtAfter(Instant date);

    /**
     * Count users created between dates
     */
    long countByCreatedAtBetween(Instant start, Instant end);

    /**
     * Count users by last login after date
     */
    long countByLastLoginAtAfter(Instant date);

    /**
     * Count users by enabled status
     */
    long countByEnabled(Boolean enabled);
}
