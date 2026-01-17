package com.cvibe.interview.dto;

import com.cvibe.interview.entity.InterviewSessionAnswer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for a single question in the interview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionDto {

    private String id;
    private String questionId;
    private String question;
    private String answer;
    private String category;
    private Integer score;
    private String feedback;

    /**
     * Convert answer entity to question DTO
     */
    public static InterviewQuestionDto fromAnswer(InterviewSessionAnswer answer) {
        return InterviewQuestionDto.builder()
                .id(answer.getId().toString())
                .questionId(answer.getQuestionId().toString())
                .question(answer.getQuestion())
                .answer(answer.getAnswer())
                .category(answer.getCategory())
                .score(answer.getScore())
                .feedback(answer.getFeedback())
                .build();
    }
}
