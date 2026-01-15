package com.cvibe.biz.job.dto;

import com.cvibe.biz.job.entity.JobMatch;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class JobMatchDto {
    private UUID id;
    private UUID jobId;
    private JobDto job;
    private Double matchScore;
    private String matchReason;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private Boolean isViewed;
    private Boolean isSaved;
    private Boolean isApplied;
    private Instant matchedAt;

    public static JobMatchDto from(JobMatch match) {
        return JobMatchDto.builder()
                .id(match.getId())
                .jobId(match.getJob().getId())
                .job(JobDto.from(match.getJob()))
                .matchScore(match.getMatchScore())
                .matchReason(match.getMatchReason())
                .matchedSkills(parseJsonArray(match.getMatchedSkills()))
                .missingSkills(parseJsonArray(match.getMissingSkills()))
                .isViewed(match.getIsViewed())
                .isSaved(match.getIsSaved())
                .isApplied(match.getIsApplied())
                .matchedAt(match.getMatchedAt())
                .build();
    }

    private static List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        // Simple JSON array parsing
        try {
            String cleaned = json.replace("[", "").replace("]", "").replace("\"", "");
            if (cleaned.isEmpty()) return List.of();
            return List.of(cleaned.split(",\\s*"));
        } catch (Exception e) {
            return List.of();
        }
    }
}
