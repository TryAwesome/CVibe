package com.cvibe.growth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for gap analysis results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapAnalysisDto {

    private String targetRole;
    private String currentLevel;
    private String targetLevel;
    private Integer overallReadiness;
    private List<SkillGapDto> skillGaps;
    private List<String> recommendations;
    private String analyzedAt;
}
