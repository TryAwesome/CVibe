package com.cvibe.growth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual skill gap
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillGapDto {

    private String skillName;
    private String category;
    private Integer currentLevel;
    private Integer requiredLevel;
    private Integer gap;
    private String priority;
    private String recommendation;
}
