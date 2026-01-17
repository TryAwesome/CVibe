package com.cvibe.interview.dto;

import com.cvibe.interview.entity.InterviewSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for interview session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSessionDto {

    private String id;
    private String sessionType;
    private String status;
    private Integer currentQuestionIndex;
    private Integer totalQuestions;
    private String focusArea;
    private String targetRole;
    private String extractionStatus;
    private String startedAt;
    private String lastActivityAt;
    private String completedAt;
    private Integer answeredCount;
    private Integer progressPercentage;

    /**
     * Convert entity to DTO
     */
    public static InterviewSessionDto fromEntity(InterviewSession entity) {
        return InterviewSessionDto.builder()
                .id(entity.getId().toString())
                .sessionType(entity.getSessionType().name())
                .status(entity.getStatus().name())
                .currentQuestionIndex(entity.getCurrentQuestionIndex())
                .totalQuestions(entity.getTotalQuestions())
                .focusArea(entity.getFocusArea() != null ? entity.getFocusArea().name() : null)
                .targetRole(entity.getTargetRole())
                .extractionStatus(entity.getExtractionStatus().name())
                .startedAt(formatInstant(entity.getStartedAt()))
                .lastActivityAt(formatInstant(entity.getLastActivityAt()))
                .completedAt(formatInstant(entity.getCompletedAt()))
                .answeredCount(entity.getAnsweredCount())
                .progressPercentage(entity.getProgressPercentage())
                .build();
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
