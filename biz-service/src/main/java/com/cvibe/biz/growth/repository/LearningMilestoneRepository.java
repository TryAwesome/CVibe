package com.cvibe.biz.growth.repository;

import com.cvibe.biz.growth.entity.LearningMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for LearningMilestone entity
 */
@Repository
public interface LearningMilestoneRepository extends JpaRepository<LearningMilestone, UUID> {

    // ==================== Path Queries ====================

    /**
     * Find all milestones for a path
     */
    List<LearningMilestone> findByLearningPathIdOrderBySortOrderAsc(UUID pathId);

    /**
     * Find completed milestones
     */
    List<LearningMilestone> findByLearningPathIdAndIsCompletedTrue(UUID pathId);

    /**
     * Find incomplete milestones
     */
    List<LearningMilestone> findByLearningPathIdAndIsCompletedFalseOrderBySortOrderAsc(UUID pathId);

    /**
     * Get next milestone to complete
     */
    @Query("SELECT m FROM LearningMilestone m WHERE m.learningPath.id = :pathId " +
           "AND m.isCompleted = false ORDER BY m.sortOrder ASC LIMIT 1")
    LearningMilestone findNextMilestone(@Param("pathId") UUID pathId);

    // ==================== Statistics ====================

    /**
     * Count completed milestones for a path
     */
    long countByLearningPathIdAndIsCompletedTrue(UUID pathId);

    /**
     * Count total milestones for a path
     */
    long countByLearningPathId(UUID pathId);

    /**
     * Get total estimated hours for remaining milestones
     */
    @Query("SELECT SUM(m.estimatedHours) FROM LearningMilestone m " +
           "WHERE m.learningPath.id = :pathId AND m.isCompleted = false")
    Integer getRemainingHours(@Param("pathId") UUID pathId);

    // ==================== User Queries ====================

    /**
     * Find recent completed milestones for user
     */
    @Query("SELECT m FROM LearningMilestone m " +
           "WHERE m.learningPath.goal.user.id = :userId " +
           "AND m.isCompleted = true " +
           "ORDER BY m.completedAt DESC")
    List<LearningMilestone> findRecentCompletedByUserId(@Param("userId") UUID userId);

    // ==================== Cleanup ====================

    /**
     * Delete all milestones for a path
     */
    void deleteByLearningPathId(UUID pathId);
}
