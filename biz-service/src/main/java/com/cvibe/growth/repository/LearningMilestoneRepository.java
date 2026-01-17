package com.cvibe.growth.repository;

import com.cvibe.growth.entity.LearningMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for learning milestones
 */
@Repository
public interface LearningMilestoneRepository extends JpaRepository<LearningMilestone, UUID> {

    /**
     * Find all milestones for a learning path, ordered by order index
     */
    List<LearningMilestone> findByPathIdOrderByOrderIndexAsc(UUID pathId);

    /**
     * Count completed milestones for a path
     */
    long countByPathIdAndIsCompletedTrue(UUID pathId);

    /**
     * Count total milestones for a path
     */
    long countByPathId(UUID pathId);
}
