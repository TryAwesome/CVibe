package com.cvibe.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for work experience, used for both request and response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceDto {

    private String id;

    @NotBlank(message = "Company is required")
    @Size(max = 100, message = "Company must be at most 100 characters")
    private String company;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be at most 100 characters")
    private String title;

    @Size(max = 100, message = "Location must be at most 100 characters")
    private String location;

    /**
     * Employment type: FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP, FREELANCE
     */
    private String employmentType;

    /**
     * Start date in ISO format (e.g., "2022-01-01")
     */
    @NotNull(message = "Start date is required")
    private String startDate;

    /**
     * End date in ISO format (e.g., "2023-12-31"), null if current position
     */
    private String endDate;

    /**
     * Whether this is the current position
     */
    private Boolean isCurrent;

    /**
     * Job description
     */
    private String description;

    /**
     * List of achievements (returned as array, not JSON string)
     */
    private List<String> achievements;

    /**
     * List of technologies used (returned as array, not JSON string)
     */
    private List<String> technologies;
}
