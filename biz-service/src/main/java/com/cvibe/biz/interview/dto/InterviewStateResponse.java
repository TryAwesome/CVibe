package com.cvibe.biz.interview.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class InterviewStateResponse {
    private InterviewSessionDto session;
    private InterviewAnswerDto currentQuestion;
    private List<InterviewAnswerDto> answeredQuestions;
    private boolean hasMoreQuestions;
    private boolean sessionCompleted;
    private String nextAction;  // "ANSWER", "REVIEW", "COMPLETE"
}
