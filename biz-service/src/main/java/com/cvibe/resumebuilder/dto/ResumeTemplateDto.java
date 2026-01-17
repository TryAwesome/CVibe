package com.cvibe.resumebuilder.dto;

import com.cvibe.resumebuilder.entity.ResumeTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历模板 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeTemplateDto {

    private String id;
    private String name;
    private String description;
    private String thumbnail;
    private String category;
    private String previewUrl;
    private Boolean isPremium;
    private Boolean isFeatured;
    private String createdAt;

    /**
     * 从实体转换为 DTO
     */
    public static ResumeTemplateDto fromEntity(ResumeTemplate entity) {
        return ResumeTemplateDto.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .description(entity.getDescription())
                .thumbnail(entity.getThumbnailUrl())
                .category(entity.getCategory() != null ? entity.getCategory().name() : null)
                .isPremium(entity.getIsPremium())
                .isFeatured(entity.getIsFeatured())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .build();
    }
}
