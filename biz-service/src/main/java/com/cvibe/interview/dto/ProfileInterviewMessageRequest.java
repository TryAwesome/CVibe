package com.cvibe.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to send a message in profile interview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileInterviewMessageRequest {
    private String message;
}
