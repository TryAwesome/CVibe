package com.cvibe.biz.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SubmitAnswerRequest {
    @NotNull(message = "Answer ID is required")
    private UUID answerId;

    @NotBlank(message = "Answer text is required")
    private String answerText;
}
