package com.cvibe.biz.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a comment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Comment must be less than 5000 characters")
    private String content;

    /**
     * Parent comment ID (for replies)
     */
    private UUID parentId;
}
