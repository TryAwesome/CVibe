package com.cvibe.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for session state with current question
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStateResponse {

    /**
     * Session details
     */
    private InterviewSessionDto session;

    /**
     * Current question to answer
     */
    private InterviewQuestionDto currentQuestion;

    /**
     * List of answered questions
     */
    private List<InterviewQuestionDto> answeredQuestions;

    /**
     * Whether there are more questions
     */
    private Boolean hasMoreQuestions;

    /**
     * Whether the session is completed
     */
    private Boolean sessionCompleted;

    /**
     * Next action for the user
     */
    private String nextAction;
}
