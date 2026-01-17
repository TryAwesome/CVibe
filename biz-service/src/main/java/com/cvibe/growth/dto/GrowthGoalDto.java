package com.cvibe.growth.dto;

import com.cvibe.growth.entity.GrowthGoal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for growth goal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthGoalDto {

    private String id;
    private String title;
    private String description;
    private String targetRole;
    private String targetDate;
    private String status;
    private Integer progress;
    private List<MilestoneDto> milestones;
    private Boolean isPrimary;
    private String createdAt;
    private String updatedAt;

    /**
     * Convert entity to DTO
     */
    public static GrowthGoalDto fromEntity(GrowthGoal entity) {
        return GrowthGoalDto.builder()
                .id(entity.getId().toString())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .targetRole(entity.getTargetRole())
                .targetDate(entity.getTargetDate() != null ? entity.getTargetDate().toString() : null)
                .status(entity.getStatus().name())
                .progress(entity.getProgress())
                .isPrimary(entity.getIsPrimary())
                .createdAt(formatInstant(entity.getCreatedAt()))
                .updatedAt(formatInstant(entity.getUpdatedAt()))
                .build();
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
