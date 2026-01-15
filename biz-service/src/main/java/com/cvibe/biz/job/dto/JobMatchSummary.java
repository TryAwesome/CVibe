package com.cvibe.biz.job.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JobMatchSummary {
    private long totalMatches;
    private long highScoreMatches;  // matches >= 80%
    private long unviewedMatches;
    private long savedJobs;
    private long appliedJobs;
    private Double averageMatchScore;
    private List<JobMatchDto> topMatches;  // Top 5 best matches
    private List<JobMatchDto> recentMatches;  // Most recent 5 matches
}
