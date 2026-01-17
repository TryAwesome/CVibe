package com.cvibe.mockinterview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting an answer to a mock interview question
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitMockAnswerRequest {

    /**
     * Question ID being answered
     */
    @NotBlank(message = "Question ID is required")
    private String questionId;

    /**
     * Question index
     */
    private Integer questionIndex;

    /**
     * Response type: VIDEO, AUDIO, or TEXT
     */
    private String responseType;

    /**
     * Text content of the answer
     */
    private String content;

    /**
     * Media URL for VIDEO or AUDIO responses
     */
    private String mediaUrl;

    /**
     * Duration of the response in seconds
     */
    private Integer durationSeconds;
}
