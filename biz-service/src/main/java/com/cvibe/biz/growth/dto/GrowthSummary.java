package com.cvibe.biz.growth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Summary DTO for growth dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthSummary {

    /**
     * Total active goals
     */
    private long activeGoals;

    /**
     * Total achieved goals
     */
    private long achievedGoals;

    /**
     * Average progress across active goals
     */
    private Double averageProgress;

    /**
     * Total unresolved skill gaps
     */
    private long totalGaps;

    /**
     * Critical gaps needing attention
     */
    private long criticalGaps;

    /**
     * In-progress learning paths
     */
    private long inProgressPaths;

    /**
     * Completed learning paths
     */
    private long completedPaths;

    /**
     * Total learning hours remaining
     */
    private Integer remainingHours;

    /**
     * Primary active goal
     */
    private GrowthGoalDto primaryGoal;

    /**
     * Recent skill gaps
     */
    private List<SkillGapDto> recentGaps;

    /**
     * Current learning paths in progress
     */
    private List<LearningPathDto> currentPaths;

    /**
     * Recently completed milestones
     */
    private List<LearningMilestoneDto> recentMilestones;
}
