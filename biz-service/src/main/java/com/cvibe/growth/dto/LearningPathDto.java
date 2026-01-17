package com.cvibe.growth.dto;

import com.cvibe.growth.entity.LearningPath;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for learning path
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathDto {

    private String id;
    private String goalId;
    private String title;
    private String description;
    private String duration;
    private List<LearningPhaseDto> phases;
    private Integer totalMilestones;
    private Integer completedMilestones;
    private Integer progressPercentage;
    private String createdAt;

    /**
     * Convert entity to DTO (without phases - those need to be populated separately)
     */
    public static LearningPathDto fromEntity(LearningPath entity) {
        return LearningPathDto.builder()
                .id(entity.getId().toString())
                .goalId(entity.getGoal() != null ? entity.getGoal().getId().toString() : null)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .duration(entity.getDuration())
                .createdAt(formatInstant(entity.getCreatedAt()))
                .build();
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
