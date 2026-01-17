package com.cvibe.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for submitting an answer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {

    /**
     * Question ID
     */
    @NotNull(message = "Question ID is required")
    private UUID questionId;

    /**
     * User's answer
     */
    @NotBlank(message = "Answer is required")
    private String answer;
}
