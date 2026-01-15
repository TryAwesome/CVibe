package com.cvibe.biz.resume.dto;

import com.cvibe.biz.resume.entity.ResumeTemplate.TemplateCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating a resume template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private TemplateCategory category;

    @NotBlank(message = "LaTeX content is required")
    private String latexContent;

    /**
     * For admin: mark as featured
     */
    private Boolean isFeatured;
}
