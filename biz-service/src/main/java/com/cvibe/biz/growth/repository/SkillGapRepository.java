package com.cvibe.biz.growth.repository;

import com.cvibe.biz.growth.entity.SkillGap;
import com.cvibe.biz.growth.entity.SkillGap.GapPriority;
import com.cvibe.biz.growth.entity.SkillGap.GapStatus;
import com.cvibe.biz.growth.entity.SkillGap.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for SkillGap entity
 */
@Repository
public interface SkillGapRepository extends JpaRepository<SkillGap, UUID> {

    // ==================== Goal Queries ====================

    /**
     * Find all gaps for a goal, ordered by priority
     */
    List<SkillGap> findByGoalIdOrderByPriorityAscCreatedAtDesc(UUID goalId);

    /**
     * Find gaps by status
     */
    List<SkillGap> findByGoalIdAndStatus(UUID goalId, GapStatus status);

    /**
     * Find unresolved gaps
     */
    @Query("SELECT sg FROM SkillGap sg WHERE sg.goal.id = :goalId " +
           "AND sg.status != 'RESOLVED' ORDER BY sg.priority ASC")
    List<SkillGap> findUnresolvedByGoalId(@Param("goalId") UUID goalId);

    /**
     * Find critical gaps
     */
    List<SkillGap> findByGoalIdAndPriority(UUID goalId, GapPriority priority);

    /**
     * Find gaps by category
     */
    List<SkillGap> findByGoalIdAndCategory(UUID goalId, SkillCategory category);

    // ==================== Statistics ====================

    /**
     * Count gaps by status for a goal
     */
    long countByGoalIdAndStatus(UUID goalId, GapStatus status);

    /**
     * Count total gaps for a goal
     */
    long countByGoalId(UUID goalId);

    /**
     * Get total estimated hours for unresolved gaps
     */
    @Query("SELECT SUM(sg.estimatedHours) FROM SkillGap sg " +
           "WHERE sg.goal.id = :goalId AND sg.status != 'RESOLVED'")
    Integer getTotalEstimatedHours(@Param("goalId") UUID goalId);

    // ==================== User Level Queries ====================

    /**
     * Find all gaps for a user (across all goals)
     */
    @Query("SELECT sg FROM SkillGap sg WHERE sg.goal.user.id = :userId " +
           "AND sg.status != 'RESOLVED' ORDER BY sg.priority ASC")
    List<SkillGap> findAllUnresolvedByUserId(@Param("userId") UUID userId);

    /**
     * Find common skill gaps across users
     */
    @Query("SELECT sg.skillName, COUNT(sg) as cnt FROM SkillGap sg " +
           "GROUP BY sg.skillName ORDER BY cnt DESC")
    List<Object[]> getMostCommonGaps();

    // ==================== Cleanup ====================

    /**
     * Delete all gaps for a goal
     */
    void deleteByGoalId(UUID goalId);
}
