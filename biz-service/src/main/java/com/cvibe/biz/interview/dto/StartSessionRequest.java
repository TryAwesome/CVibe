package com.cvibe.biz.interview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartSessionRequest {
    @NotBlank(message = "Session type is required")
    private String sessionType;  // INITIAL_PROFILE, DEEP_DIVE, etc.

    private String focusArea;    // e.g., "WORK_EXPERIENCE", "SKILLS"
    private String targetRole;   // e.g., "Software Engineer"
    private String language;     // default "en"
}
