package com.cvibe.mockinterview.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new mock interview session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMockInterviewRequest {

    /**
     * Interview type: VIDEO, AUDIO, or TEXT
     */
    @NotNull(message = "Interview type is required")
    private String type;

    /**
     * Interview settings
     */
    @NotNull(message = "Settings are required")
    private MockInterviewSettingsDto settings;
}
