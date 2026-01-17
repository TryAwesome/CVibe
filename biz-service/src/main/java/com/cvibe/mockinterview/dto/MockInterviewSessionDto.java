package com.cvibe.mockinterview.dto;

import com.cvibe.mockinterview.entity.MockInterviewSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for mock interview session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewSessionDto {

    private String id;
    private String type;
    private String status;
    private Integer currentQuestionIndex;
    private Integer totalQuestions;
    private Integer progressPercentage;
    private Integer overallScore;
    private MockInterviewSettingsDto settings;
    private String startedAt;
    private String completedAt;
    private String createdAt;
    private String updatedAt;

    /**
     * Convert entity to DTO
     */
    public static MockInterviewSessionDto fromEntity(MockInterviewSession entity) {
        return fromEntity(entity, null);
    }

    /**
     * Convert entity to DTO with parsed settings
     */
    public static MockInterviewSessionDto fromEntity(MockInterviewSession entity, MockInterviewSettingsDto settings) {
        return MockInterviewSessionDto.builder()
                .id(entity.getId().toString())
                .type(entity.getType().name())
                .status(entity.getStatus().name())
                .currentQuestionIndex(entity.getCurrentQuestionIndex())
                .totalQuestions(entity.getTotalQuestions())
                .progressPercentage(entity.getProgressPercentage())
                .overallScore(entity.getOverallScore())
                .settings(settings)
                .startedAt(formatInstant(entity.getStartedAt()))
                .completedAt(formatInstant(entity.getCompletedAt()))
                .createdAt(formatInstant(entity.getCreatedAt()))
                .updatedAt(formatInstant(entity.getUpdatedAt()))
                .build();
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
