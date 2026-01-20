package com.cvibe.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from sending a message in profile interview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileInterviewMessageResponse {
    private String response;
    private String currentPhase;
    private String phaseName;
    private int turnCount;
    private boolean isComplete;
}
