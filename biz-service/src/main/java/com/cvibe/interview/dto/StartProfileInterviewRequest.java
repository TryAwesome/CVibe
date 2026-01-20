package com.cvibe.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to start a profile interview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartProfileInterviewRequest {
    private String language;  // "zh" or "en"
}
