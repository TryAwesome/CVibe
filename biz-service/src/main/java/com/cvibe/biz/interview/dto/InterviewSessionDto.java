package com.cvibe.biz.interview.dto;

import com.cvibe.biz.interview.entity.InterviewSession;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InterviewSessionDto {
    private UUID id;
    private String sessionType;
    private String status;
    private Integer currentQuestionIndex;
    private Integer totalQuestions;
    private String focusArea;
    private String targetRole;
    private String extractionStatus;
    private Instant startedAt;
    private Instant lastActivityAt;
    private Instant completedAt;
    private Integer answeredCount;
    private Double progressPercentage;

    public static InterviewSessionDto from(InterviewSession session) {
        return InterviewSessionDto.builder()
                .id(session.getId())
                .sessionType(session.getSessionType().name())
                .status(session.getStatus().name())
                .currentQuestionIndex(session.getCurrentQuestionIndex())
                .totalQuestions(session.getTotalQuestions())
                .focusArea(session.getFocusArea())
                .targetRole(session.getTargetRole())
                .extractionStatus(session.getExtractionStatus().name())
                .startedAt(session.getStartedAt())
                .lastActivityAt(session.getLastActivityAt())
                .completedAt(session.getCompletedAt())
                .build();
    }
}
