package com.cvibe.job.dto;

import com.cvibe.job.entity.JobMatch;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * DTO for JobMatch entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchDto {

    private String id;
    private JobDto job;
    private Integer matchScore;
    private MatchDetailsDto matchDetails;
    private String status;
    private String createdAt;

    /**
     * Inner class for match details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchDetailsDto {
        private List<String> reasons;
        private List<String> matchingSkills;
        private Integer skillMatchPercentage;
        private Integer experienceMatchPercentage;
        private Integer locationMatchPercentage;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert entity to DTO
     */
    public static JobMatchDto fromEntity(JobMatch entity) {
        return JobMatchDto.builder()
                .id(entity.getId().toString())
                .job(JobDto.fromEntity(entity.getJob()))
                .matchScore(entity.getMatchScore())
                .matchDetails(parseMatchDetails(entity.getMatchReasonsJson()))
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .build();
    }

    /**
     * Convert entity to DTO without full job details
     */
    public static JobMatchDto fromEntityLight(JobMatch entity) {
        JobDto jobDto = JobDto.builder()
                .id(entity.getJob().getId().toString())
                .title(entity.getJob().getTitle())
                .company(entity.getJob().getCompany())
                .location(entity.getJob().getLocation())
                .type(entity.getJob().getType() != null ? entity.getJob().getType().name() : null)
                .build();

        return JobMatchDto.builder()
                .id(entity.getId().toString())
                .job(jobDto)
                .matchScore(entity.getMatchScore())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .build();
    }

    private static MatchDetailsDto parseMatchDetails(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MatchDetailsDto.class);
        } catch (Exception e) {
            // Try to parse as simple string array (reasons only)
            try {
                List<String> reasons = objectMapper.readValue(json, new TypeReference<List<String>>() {});
                return MatchDetailsDto.builder().reasons(reasons).build();
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
