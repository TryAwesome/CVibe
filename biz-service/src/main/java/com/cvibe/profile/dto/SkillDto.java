package com.cvibe.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for skills, used for both request and response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDto {

    private String id;

    @NotBlank(message = "Skill name is required")
    @Size(max = 50, message = "Skill name must be at most 50 characters")
    private String name;

    /**
     * Skill level: BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
     */
    @NotBlank(message = "Skill level is required")
    private String level;
}
