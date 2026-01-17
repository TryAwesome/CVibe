package com.cvibe.growth.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a growth goal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGoalRequest {

    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    private String description;

    private String targetRole;

    /**
     * Target date in ISO format (yyyy-MM-dd)
     */
    private String targetDate;

    /**
     * Status: NOT_STARTED, IN_PROGRESS, COMPLETED, PAUSED
     */
    private String status;

    /**
     * Progress percentage (0-100)
     */
    private Integer progress;

    private Boolean isPrimary;
}
