package com.cvibe.mockinterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for submitting a mock interview answer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockAnswerSubmitResponse {

    /**
     * Whether the answer was accepted
     */
    private boolean accepted;

    /**
     * Score for this answer (0-100)
     */
    private Integer score;

    /**
     * Feedback for the answer
     */
    private MockInterviewQuestionDto.FeedbackDto feedback;

    /**
     * Next question if available
     */
    private MockInterviewQuestionDto nextQuestion;

    /**
     * Whether there are more questions
     */
    private boolean hasMoreQuestions;

    /**
     * Whether the session is completed
     */
    private boolean sessionCompleted;
}
