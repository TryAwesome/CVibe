package com.cvibe.biz.mock.dto;

import com.cvibe.biz.mock.entity.MockInterview.DifficultyLevel;
import com.cvibe.biz.mock.entity.MockInterview.InterviewType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for starting a new mock interview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartInterviewRequest {

    @NotBlank(message = "Target position is required")
    private String targetPosition;

    private String targetCompany;

    @NotNull(message = "Interview type is required")
    private InterviewType interviewType;

    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    @Min(value = 1, message = "At least 1 question required")
    @Max(value = 20, message = "Maximum 20 questions allowed")
    @Builder.Default
    private Integer questionCount = 5;

    /**
     * Specific skills to focus on
     */
    private List<String> skills;

    /**
     * Job description for context
     */
    private String jobDescription;

    /**
     * Time limit per question in seconds (optional)
     */
    private Integer timeLimitPerQuestion;

    /**
     * Include follow-up questions
     */
    @Builder.Default
    private Boolean includeFollowUps = true;
}
