package com.cvibe.biz.growth.dto;

import com.cvibe.biz.growth.entity.LearningMilestone;
import com.cvibe.biz.growth.entity.LearningMilestone.MilestoneType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for LearningMilestone
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningMilestoneDto {

    private UUID id;
    private UUID learningPathId;
    private String title;
    private String description;
    private MilestoneType type;
    private Integer estimatedHours;
    private String resourceUrl;
    private Integer sortOrder;
    private Boolean isCompleted;
    private Instant completedAt;
    private String userNotes;
    private Instant createdAt;

    /**
     * Convert entity to DTO
     */
    public static LearningMilestoneDto from(LearningMilestone milestone) {
        return LearningMilestoneDto.builder()
                .id(milestone.getId())
                .learningPathId(milestone.getLearningPath() != null ? milestone.getLearningPath().getId() : null)
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .type(milestone.getType())
                .estimatedHours(milestone.getEstimatedHours())
                .resourceUrl(milestone.getResourceUrl())
                .sortOrder(milestone.getSortOrder())
                .isCompleted(milestone.getIsCompleted())
                .completedAt(milestone.getCompletedAt())
                .userNotes(milestone.getUserNotes())
                .createdAt(milestone.getCreatedAt())
                .build();
    }
}
