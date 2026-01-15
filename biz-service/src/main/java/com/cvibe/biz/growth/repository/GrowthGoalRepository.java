package com.cvibe.biz.growth.repository;

import com.cvibe.biz.growth.entity.GrowthGoal;
import com.cvibe.biz.growth.entity.GrowthGoal.GoalStatus;
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
 * Repository for GrowthGoal entity
 */
@Repository
public interface GrowthGoalRepository extends JpaRepository<GrowthGoal, UUID> {

    // ==================== User Queries ====================

    /**
     * Find all goals for a user
     */
    List<GrowthGoal> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find user's active goals
     */
    List<GrowthGoal> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId);

    /**
     * Find user's primary active goal
     */
    Optional<GrowthGoal> findFirstByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId);

    /**
     * Find goals by status
     */
    List<GrowthGoal> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, GoalStatus status);

    /**
     * Find achieved goals
     */
    List<GrowthGoal> findByUserIdAndStatusOrderByUpdatedAtDesc(UUID userId, GoalStatus status);

    /**
     * Paginated user goals
     */
    Page<GrowthGoal> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // ==================== Statistics ====================

    /**
     * Count user goals by status
     */
    long countByUserIdAndStatus(UUID userId, GoalStatus status);

    /**
     * Count active goals
     */
    long countByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Get average progress for active goals
     */
    @Query("SELECT AVG(g.progressPercent) FROM GrowthGoal g WHERE g.user.id = :userId AND g.isActive = true")
    Double getAverageProgress(@Param("userId") UUID userId);

    /**
     * Get goals needing analysis (not analyzed recently)
     */
    @Query("SELECT g FROM GrowthGoal g WHERE g.isActive = true " +
           "AND (g.lastAnalyzedAt IS NULL OR g.lastAnalyzedAt < :threshold)")
    List<GrowthGoal> findGoalsNeedingAnalysis(@Param("threshold") java.time.Instant threshold);

    // ==================== Admin Queries ====================

    /**
     * Count total active goals
     */
    long countByIsActiveTrue();

    /**
     * Get popular target roles
     */
    @Query("SELECT g.targetRole, COUNT(g) as cnt FROM GrowthGoal g " +
           "WHERE g.isActive = true GROUP BY g.targetRole ORDER BY cnt DESC")
    List<Object[]> getPopularTargetRoles();

    /**
     * Get popular target companies
     */
    @Query("SELECT g.targetCompany, COUNT(g) as cnt FROM GrowthGoal g " +
           "WHERE g.targetCompany IS NOT NULL AND g.isActive = true " +
           "GROUP BY g.targetCompany ORDER BY cnt DESC")
    List<Object[]> getPopularTargetCompanies();
}
