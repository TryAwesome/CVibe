package com.cvibe.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for profile interview state
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileInterviewStateResponse {
    private String sessionId;
    private String currentPhase;
    private String phaseName;
    private int turnCount;
    private String status;
    private String portraitSummary;
}
