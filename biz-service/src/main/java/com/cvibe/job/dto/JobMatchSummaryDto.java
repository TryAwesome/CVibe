package com.cvibe.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary DTO for job matches
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchSummaryDto {

    private Long totalMatches;
    private Long newMatches;
    private Long savedJobs;
    private Long appliedJobs;
    private Double averageMatchScore;
    private Long viewedMatches;
    
    /**
     * Create an empty summary
     */
    public static JobMatchSummaryDto empty() {
        return JobMatchSummaryDto.builder()
                .totalMatches(0L)
                .newMatches(0L)
                .savedJobs(0L)
                .appliedJobs(0L)
                .averageMatchScore(0.0)
                .viewedMatches(0L)
                .build();
    }
}
