package com.cvibe.biz.resume.dto;

import com.cvibe.biz.resume.entity.ResumeGeneration;
import com.cvibe.biz.resume.entity.ResumeGeneration.GenerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * DTO for ResumeGeneration result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedResumeDto {

    private UUID id;
    private UUID templateId;
    private String templateName;
    private String targetJobTitle;
    private String targetCompany;
    private GenerationStatus status;
    private String tailoringNotes;
    private List<String> matchedKeywords;
    private String pdfUrl;
    private Boolean isExported;
    private Instant exportedAt;
    private Integer userRating;
    private Instant createdAt;

    // LaTeX content fields - only included when specifically requested
    private String generatedLatex;
    private String finalLatex;

    /**
     * Convert entity to DTO (without LaTeX content)
     */
    public static GeneratedResumeDto from(ResumeGeneration generation) {
        return from(generation, false);
    }

    /**
     * Convert entity to DTO with optional LaTeX content
     */
    public static GeneratedResumeDto from(ResumeGeneration generation, boolean includeLatex) {
        GeneratedResumeDtoBuilder builder = GeneratedResumeDto.builder()
                .id(generation.getId())
                .templateId(generation.getTemplate() != null ? generation.getTemplate().getId() : null)
                .templateName(generation.getTemplate() != null ? generation.getTemplate().getName() : null)
                .targetJobTitle(generation.getTargetJobTitle())
                .targetCompany(generation.getTargetCompany())
                .status(generation.getStatus())
                .tailoringNotes(generation.getTailoringNotes())
                .matchedKeywords(parseKeywords(generation.getMatchedKeywords()))
                .pdfUrl(generation.getPdfFilePath())
                .isExported(generation.getIsExported())
                .exportedAt(generation.getExportedAt())
                .userRating(generation.getUserRating())
                .createdAt(generation.getCreatedAt());

        if (includeLatex) {
            builder.generatedLatex(generation.getGeneratedLatex())
                    .finalLatex(generation.getFinalLatex());
        }

        return builder.build();
    }

    private static List<String> parseKeywords(String keywordsJson) {
        if (keywordsJson == null || keywordsJson.isBlank()) {
            return List.of();
        }
        // Simple parsing - assuming comma-separated or JSON array
        if (keywordsJson.startsWith("[")) {
            keywordsJson = keywordsJson.substring(1, keywordsJson.length() - 1);
        }
        return Arrays.stream(keywordsJson.split(","))
                .map(s -> s.trim().replace("\"", ""))
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
