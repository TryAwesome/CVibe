package com.cvibe.mockinterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for mock interview summary statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewSummaryDto {

    /**
     * Total number of mock interviews
     */
    private Long totalInterviews;

    /**
     * Number of completed interviews
     */
    private Long completedInterviews;

    /**
     * Number of in-progress interviews
     */
    private Long inProgressInterviews;

    /**
     * Average score across completed interviews
     */
    private Double averageScore;

    /**
     * Best score achieved
     */
    private Integer bestScore;

    /**
     * Most recent interview date
     */
    private String lastInterviewDate;

    /**
     * Interview count by type
     */
    private TypeStats typeStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeStats {
        private Long videoCount;
        private Long audioCount;
        private Long textCount;
    }
}
