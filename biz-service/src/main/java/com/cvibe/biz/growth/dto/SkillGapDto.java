package com.cvibe.biz.growth.dto;

import com.cvibe.biz.growth.entity.SkillGap;
import com.cvibe.biz.growth.entity.SkillGap.GapPriority;
import com.cvibe.biz.growth.entity.SkillGap.GapStatus;
import com.cvibe.biz.growth.entity.SkillGap.SkillCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for SkillGap
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillGapDto {

    private UUID id;
    private UUID goalId;
    private String skillName;
    private SkillCategory category;
    private Integer currentLevel;
    private Integer requiredLevel;
    private Integer gapSize;
    private Double gapPercentage;
    private GapPriority priority;
    private GapStatus status;
    private Boolean isRequired;
    private Boolean isPreferred;
    private Integer estimatedHours;
    private String recommendation;
    private String learningResources;
    private String userNotes;
    private Instant createdAt;

    /**
     * Convert entity to DTO
     */
    public static SkillGapDto from(SkillGap gap) {
        return SkillGapDto.builder()
                .id(gap.getId())
                .goalId(gap.getGoal() != null ? gap.getGoal().getId() : null)
                .skillName(gap.getSkillName())
                .category(gap.getCategory())
                .currentLevel(gap.getCurrentLevel())
                .requiredLevel(gap.getRequiredLevel())
                .gapSize(gap.getGapSize())
                .gapPercentage(gap.getGapPercentage())
                .priority(gap.getPriority())
                .status(gap.getStatus())
                .isRequired(gap.getIsRequired())
                .isPreferred(gap.getIsPreferred())
                .estimatedHours(gap.getEstimatedHours())
                .recommendation(gap.getRecommendation())
                .learningResources(gap.getLearningResources())
                .userNotes(gap.getUserNotes())
                .createdAt(gap.getCreatedAt())
                .build();
    }
}
