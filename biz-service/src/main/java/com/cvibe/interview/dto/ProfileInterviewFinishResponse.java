package com.cvibe.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response when finishing a profile interview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileInterviewFinishResponse {
    private boolean success;
    private int completenessScore;
    private List<String> missingSections;
    private String message;
}
