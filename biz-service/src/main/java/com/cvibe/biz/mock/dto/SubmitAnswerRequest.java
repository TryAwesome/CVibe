package com.cvibe.biz.mock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for submitting an answer to a question
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {

    @NotNull(message = "Question ID is required")
    private UUID questionId;

    @NotBlank(message = "Answer text is required")
    private String answerText;

    /**
     * Code answer (for coding questions)
     */
    private String codeAnswer;

    /**
     * Programming language (for coding questions)
     */
    private String programmingLanguage;

    /**
     * When the user started answering (for time tracking)
     */
    private Instant startedAt;
}
