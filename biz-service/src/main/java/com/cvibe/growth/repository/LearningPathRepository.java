package com.cvibe.growth.repository;

import com.cvibe.growth.entity.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for learning paths
 */
@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, UUID> {

    /**
     * Find all learning paths for a goal
     */
    List<LearningPath> findByGoalId(UUID goalId);

    /**
     * Find all learning paths for a user
     */
    List<LearningPath> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
