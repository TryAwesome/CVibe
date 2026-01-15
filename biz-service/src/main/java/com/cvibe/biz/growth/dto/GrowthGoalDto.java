package com.cvibe.biz.growth.dto;

import com.cvibe.biz.growth.entity.GrowthGoal;
import com.cvibe.biz.growth.entity.GrowthGoal.GoalStatus;
import com.cvibe.biz.growth.entity.GrowthGoal.TargetLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO for GrowthGoal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthGoalDto {

    private UUID id;
    private String targetRole;
    private String targetCompany;
    private TargetLevel targetLevel;
    private LocalDate targetDate;
    private Integer progressPercent;
    private GoalStatus status;
    private Boolean isActive;
    private String analysisSummary;
    private Double matchScore;
    private Instant lastAnalyzedAt;
    private Instant createdAt;

    // Nested data (optional, for detailed view)
    private List<SkillGapDto> skillGaps;
    private List<LearningPathDto> learningPaths;

    // Summary stats
    private Integer totalGaps;
    private Integer resolvedGaps;
    private Integer totalPaths;
    private Integer completedPaths;

    /**
     * Convert entity to simple DTO (without nested data)
     */
    public static GrowthGoalDto from(GrowthGoal goal) {
        return from(goal, false);
    }

    /**
     * Convert entity to DTO with optional nested data
     */
    public static GrowthGoalDto from(GrowthGoal goal, boolean includeDetails) {
        GrowthGoalDtoBuilder builder = GrowthGoalDto.builder()
                .id(goal.getId())
                .targetRole(goal.getTargetRole())
                .targetCompany(goal.getTargetCompany())
                .targetLevel(goal.getTargetLevel())
                .targetDate(goal.getTargetDate())
                .progressPercent(goal.getProgressPercent())
                .status(goal.getStatus())
                .isActive(goal.getIsActive())
                .analysisSummary(goal.getAnalysisSummary())
                .matchScore(goal.getMatchScore())
                .lastAnalyzedAt(goal.getLastAnalyzedAt())
                .createdAt(goal.getCreatedAt());

        if (includeDetails && goal.getSkillGaps() != null) {
            builder.skillGaps(goal.getSkillGaps().stream()
                    .map(SkillGapDto::from)
                    .collect(Collectors.toList()));
            builder.totalGaps(goal.getSkillGaps().size());
            builder.resolvedGaps((int) goal.getSkillGaps().stream()
                    .filter(g -> g.getStatus() == com.cvibe.biz.growth.entity.SkillGap.GapStatus.RESOLVED)
                    .count());
        }

        if (includeDetails && goal.getLearningPaths() != null) {
            builder.learningPaths(goal.getLearningPaths().stream()
                    .map(LearningPathDto::from)
                    .collect(Collectors.toList()));
            builder.totalPaths(goal.getLearningPaths().size());
            builder.completedPaths((int) goal.getLearningPaths().stream()
                    .filter(p -> p.getStatus() == com.cvibe.biz.growth.entity.LearningPath.PathStatus.COMPLETED)
                    .count());
        }

        return builder.build();
    }
}
