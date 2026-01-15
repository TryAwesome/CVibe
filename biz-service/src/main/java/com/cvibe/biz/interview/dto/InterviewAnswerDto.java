package com.cvibe.biz.interview.dto;

import com.cvibe.biz.interview.entity.InterviewAnswer;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InterviewAnswerDto {
    private UUID id;
    private Integer questionOrder;
    private String questionText;
    private String answerText;
    private Boolean isFollowUp;
    private Integer followUpDepth;
    private UUID parentAnswerId;
    private Double confidenceScore;
    private Boolean needsClarification;
    private Instant answeredAt;

    public static InterviewAnswerDto from(InterviewAnswer answer) {
        return InterviewAnswerDto.builder()
                .id(answer.getId())
                .questionOrder(answer.getQuestionOrder())
                .questionText(answer.getQuestionText())
                .answerText(answer.getAnswerText())
                .isFollowUp(answer.getIsFollowUp())
                .followUpDepth(answer.getFollowUpDepth())
                .parentAnswerId(answer.getParentAnswerId())
                .confidenceScore(answer.getConfidenceScore())
                .needsClarification(answer.getNeedsClarification())
                .answeredAt(answer.getAnsweredAt())
                .build();
    }
}
