package com.cvibe.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response when starting a profile interview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileInterviewStartResponse {
    private String sessionId;
    private String aiSessionId;
    private String welcomeMessage;
    private String firstQuestion;
    private String currentPhase;
}
