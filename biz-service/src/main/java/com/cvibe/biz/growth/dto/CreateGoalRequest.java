package com.cvibe.biz.growth.dto;

import com.cvibe.biz.growth.entity.GrowthGoal.TargetLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating/updating a growth goal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGoalRequest {

    @NotBlank(message = "Target role is required")
    @Size(max = 200, message = "Target role must not exceed 200 characters")
    private String targetRole;

    @Size(max = 200, message = "Target company must not exceed 200 characters")
    private String targetCompany;

    private TargetLevel targetLevel;

    /**
     * Job description/requirements text
     */
    private String jobRequirements;

    /**
     * Path to uploaded JD file
     */
    private String jdFilePath;

    /**
     * Target date to achieve the goal
     */
    private LocalDate targetDate;
}
