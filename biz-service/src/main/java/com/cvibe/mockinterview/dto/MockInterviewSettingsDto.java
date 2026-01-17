package com.cvibe.mockinterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for mock interview settings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewSettingsDto {

    /**
     * Target job position (e.g., "Software Engineer", "Product Manager")
     */
    private String targetPosition;

    /**
     * Target company (e.g., "Google", "Amazon")
     */
    private String targetCompany;

    /**
     * Interview type: VIDEO, AUDIO, or TEXT
     */
    private String interviewType;

    /**
     * Difficulty level: EASY, MEDIUM, HARD
     */
    @Builder.Default
    private String difficulty = "MEDIUM";

    /**
     * Total interview duration in minutes
     */
    @Builder.Default
    private Integer duration = 30;

    /**
     * Number of questions
     */
    @Builder.Default
    private Integer questionCount = 5;

    /**
     * Time limit per question in seconds
     */
    @Builder.Default
    private Integer timePerQuestion = 120;

    /**
     * Allow retaking questions
     */
    @Builder.Default
    private Boolean allowRetake = false;

    /**
     * Focus areas for questions
     */
    private String[] focusAreas;
}
