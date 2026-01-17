package com.cvibe.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for answer submission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmitResponse {

    /**
     * Whether the answer was accepted
     */
    private Boolean accepted;

    /**
     * Score for the answer (0-100)
     */
    private Integer score;

    /**
     * Feedback for the answer
     */
    private String feedback;

    /**
     * Next question (if any)
     */
    private InterviewQuestionDto nextQuestion;

    /**
     * Whether there are more questions
     */
    private Boolean hasMoreQuestions;

    /**
     * Whether the session is now completed
     */
    private Boolean sessionCompleted;
}
