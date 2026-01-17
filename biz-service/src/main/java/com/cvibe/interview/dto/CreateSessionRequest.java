package com.cvibe.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new interview session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {

    /**
     * Session type: INITIAL_PROFILE or DEEP_DIVE
     */
    @NotBlank(message = "Session type is required")
    private String sessionType;

    /**
     * Focus area for the session
     */
    private String focusArea;

    /**
     * Target role/position
     */
    private String targetRole;

    /**
     * Language for the interview (default: en)
     */
    private String language;
}
