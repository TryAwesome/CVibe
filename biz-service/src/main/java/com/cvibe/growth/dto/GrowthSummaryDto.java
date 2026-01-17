package com.cvibe.growth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for growth summary/overview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthSummaryDto {

    private Integer activeGoals;
    private Integer completedMilestones;
    private Integer totalMilestones;
    private Integer overallProgress;
}
