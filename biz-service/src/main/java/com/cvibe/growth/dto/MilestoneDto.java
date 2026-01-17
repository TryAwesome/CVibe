package com.cvibe.growth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for a milestone within a goal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneDto {

    private String id;
    private String title;
    private String description;
    private Boolean isCompleted;
    private Integer orderIndex;
    private String completedAt;
}
