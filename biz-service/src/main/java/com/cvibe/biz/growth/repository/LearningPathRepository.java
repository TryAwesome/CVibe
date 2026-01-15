package com.cvibe.biz.growth.repository;

import com.cvibe.biz.growth.entity.LearningPath;
import com.cvibe.biz.growth.entity.LearningPath.PathStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for LearningPath entity
 */
@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, UUID> {

    // ==================== Goal Queries ====================

    /**
     * Find all paths for a goal
     */
    List<LearningPath> findByGoalIdOrderBySortOrderAsc(UUID goalId);

    /**
     * Find paths by status
     */
    List<LearningPath> findByGoalIdAndStatus(UUID goalId, PathStatus status);

    /**
     * Find in-progress paths
     */
    List<LearningPath> findByGoalIdAndStatusOrderBySortOrderAsc(UUID goalId, PathStatus status);

    // ==================== User Queries ====================

    /**
     * Find all paths for a user
     */
    @Query("SELECT lp FROM LearningPath lp WHERE lp.goal.user.id = :userId " +
           "ORDER BY lp.goal.createdAt DESC, lp.sortOrder ASC")
    List<LearningPath> findAllByUserId(@Param("userId") UUID userId);

    /**
     * Find in-progress paths for user
     */
    @Query("SELECT lp FROM LearningPath lp WHERE lp.goal.user.id = :userId " +
           "AND lp.status = 'IN_PROGRESS' ORDER BY lp.updatedAt DESC")
    List<LearningPath> findInProgressByUserId(@Param("userId") UUID userId);

    // ==================== Statistics ====================

    /**
     * Count paths by status for a goal
     */
    long countByGoalIdAndStatus(UUID goalId, PathStatus status);

    /**
     * Count total paths for a goal
     */
    long countByGoalId(UUID goalId);

    /**
     * Get average completion for a goal's paths
     */
    @Query("SELECT AVG(lp.completionPercent) FROM LearningPath lp WHERE lp.goal.id = :goalId")
    Double getAverageCompletion(@Param("goalId") UUID goalId);

    /**
     * Get total estimated hours for a goal
     */
    @Query("SELECT SUM(lp.estimatedHours) FROM LearningPath lp WHERE lp.goal.id = :goalId")
    Integer getTotalEstimatedHours(@Param("goalId") UUID goalId);

    // ==================== Cleanup ====================

    /**
     * Delete all paths for a goal
     */
    void deleteByGoalId(UUID goalId);
}
