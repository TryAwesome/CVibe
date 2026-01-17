package com.cvibe.mockinterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for mock interview session state
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewStateResponse {

    /**
     * Session information
     */
    private MockInterviewSessionDto session;

    /**
     * Current question
     */
    private MockInterviewQuestionDto currentQuestion;

    /**
     * All questions with their responses and feedback (for completed sessions)
     */
    private List<MockInterviewQuestionDto> questions;

    /**
     * Whether there are more questions
     */
    private boolean hasMoreQuestions;

    /**
     * Whether the session is completed
     */
    private boolean sessionCompleted;

    /**
     * Next action hint: START, ANSWER_QUESTION, VIEW_RESULTS
     */
    private String nextAction;
}
