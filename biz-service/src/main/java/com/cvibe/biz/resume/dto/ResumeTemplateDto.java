package com.cvibe.biz.resume.dto;

import com.cvibe.biz.resume.entity.ResumeTemplate;
import com.cvibe.biz.resume.entity.ResumeTemplate.TemplateCategory;
import com.cvibe.biz.resume.entity.ResumeTemplate.TemplateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for ResumeTemplate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeTemplateDto {

    private UUID id;
    private String name;
    private String description;
    private TemplateType templateType;
    private TemplateCategory category;
    private String thumbnailUrl;
    private Boolean isFeatured;
    private Integer usageCount;
    private Boolean isOwner;  // true if current user owns this template
    private Instant createdAt;

    // LaTeX content is not included by default (large field)
    // Use separate endpoint to fetch template content

    /**
     * Convert entity to DTO
     */
    public static ResumeTemplateDto from(ResumeTemplate template) {
        return from(template, null);
    }

    /**
     * Convert entity to DTO with ownership check
     */
    public static ResumeTemplateDto from(ResumeTemplate template, UUID currentUserId) {
        return ResumeTemplateDto.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .templateType(template.getTemplateType())
                .category(template.getCategory())
                .thumbnailUrl(template.getThumbnailUrl())
                .isFeatured(template.getIsFeatured())
                .usageCount(template.getUsageCount())
                .isOwner(currentUserId != null && template.belongsToUser(currentUserId))
                .createdAt(template.getCreatedAt())
                .build();
    }
}
