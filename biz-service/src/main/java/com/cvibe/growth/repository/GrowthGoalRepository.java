package com.cvibe.growth.repository;

import com.cvibe.growth.entity.GrowthGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for growth goals
 */
@Repository
public interface GrowthGoalRepository extends JpaRepository<GrowthGoal, UUID> {

    /**
     * Find all goals for a user, ordered by creation date descending
     */
    List<GrowthGoal> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find a goal by ID and user ID (for ownership verification)
     */
    Optional<GrowthGoal> findByIdAndUserId(UUID id, UUID userId);
}
