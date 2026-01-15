package com.cvibe.biz.resume.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for generating a tailored resume
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResumeRequest {

    /**
     * Template ID to use for generation
     */
    @NotNull(message = "Template ID is required")
    private UUID templateId;

    /**
     * Target job title
     */
    private String targetJobTitle;

    /**
     * Target company name
     */
    private String targetCompany;

    /**
     * Job description text (direct paste)
     */
    private String jobDescription;

    /**
     * If JD was uploaded as file, this contains the file path
     * (mutually exclusive with jobDescription)
     */
    private String jdFilePath;

    /**
     * Whether to emphasize certain skills
     */
    private String[] emphasizeSkills;

    /**
     * Whether to de-emphasize certain experiences
     */
    private String[] excludeExperiences;

    /**
     * Custom instructions for AI tailoring
     */
    private String customInstructions;

    /**
     * Output language preference (default: en)
     */
    @Builder.Default
    private String language = "en";
}
