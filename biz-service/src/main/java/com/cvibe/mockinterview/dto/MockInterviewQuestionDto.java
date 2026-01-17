package com.cvibe.mockinterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for a single mock interview question
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewQuestionDto {

    /**
     * Question index (0-based)
     */
    private Integer index;

    /**
     * Question ID
     */
    private String questionId;

    /**
     * The question text
     */
    private String question;

    /**
     * Question category (e.g., "BEHAVIORAL", "TECHNICAL", "SITUATIONAL")
     */
    private String category;

    /**
     * Time limit for this question in seconds
     */
    private Integer timeLimit;

    /**
     * User's response to this question
     */
    private ResponseDto response;

    /**
     * Feedback for this question (available after submission)
     */
    private FeedbackDto feedback;

    /**
     * Inner class for user response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseDto {
        /**
         * Response type: VIDEO, AUDIO, or TEXT
         */
        private String type;

        /**
         * Text content of the response (for TEXT type or transcription)
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

        /**
         * Timestamp when response was submitted
         */
        private String submittedAt;
    }

    /**
     * Inner class for question feedback
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackDto {
        /**
         * Score for this question (0-100)
         */
        private Integer score;

        /**
         * Overall feedback text
         */
        private String overallFeedback;

        /**
         * Strengths identified in the response
         */
        private String[] strengths;

        /**
         * Areas for improvement
         */
        private String[] improvements;

        /**
         * Suggested better response
         */
        private String suggestedResponse;
    }
}
