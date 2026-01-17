package com.cvibe.profile.repository;

import com.cvibe.profile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserProfile entity.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /**
     * Find profile by user ID.
     */
    Optional<UserProfile> findByUserId(UUID userId);

    /**
     * Check if profile exists for user.
     */
    boolean existsByUserId(UUID userId);

    /**
     * Find profile with all details (experiences and skills) eagerly loaded.
     * Uses separate queries to avoid Cartesian product.
     */
    @Query("SELECT p FROM UserProfile p LEFT JOIN FETCH p.experiences WHERE p.user.id = :userId")
    Optional<UserProfile> findByUserIdWithExperiences(@Param("userId") UUID userId);

    /**
     * Find profile with skills eagerly loaded.
     */
    @Query("SELECT p FROM UserProfile p LEFT JOIN FETCH p.skills WHERE p.user.id = :userId")
    Optional<UserProfile> findByUserIdWithSkills(@Param("userId") UUID userId);
}
