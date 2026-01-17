package com.cvibe.job.dto;

import com.cvibe.job.entity.Job;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * DTO for Job entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {

    private String id;
    private String title;
    private String company;
    private String companyLogo;
    private String location;
    private String type;
    private SalaryDto salary;
    private String description;
    private List<String> requirements;
    private List<String> responsibilities;
    private List<String> benefits;
    private List<String> skills;
    private String experienceLevel;
    private String postedAt;
    private String deadline;
    private String source;
    private String sourceUrl;
    private Boolean isRemote;
    private String createdAt;

    /**
     * Inner class for salary information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaryDto {
        private Integer min;
        private Integer max;
        private String currency;
        private String period;
        private String formatted;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert entity to DTO
     */
    public static JobDto fromEntity(Job entity) {
        return JobDto.builder()
                .id(entity.getId().toString())
                .title(entity.getTitle())
                .company(entity.getCompany())
                .companyLogo(entity.getCompanyLogo())
                .location(entity.getLocation())
                .type(entity.getType() != null ? entity.getType().name() : null)
                .salary(buildSalaryDto(entity))
                .description(entity.getDescription())
                .requirements(parseJsonArray(entity.getRequirements()))
                .responsibilities(parseJsonArray(entity.getResponsibilities()))
                .benefits(parseJsonArray(entity.getBenefits()))
                .skills(parseJsonArray(entity.getSkills()))
                .experienceLevel(entity.getExperienceLevel() != null ? entity.getExperienceLevel().name() : null)
                .postedAt(formatInstant(entity.getPostedAt()))
                .deadline(formatLocalDate(entity.getDeadline()))
                .source(entity.getSource())
                .sourceUrl(entity.getSourceUrl())
                .isRemote(entity.getIsRemote())
                .createdAt(formatInstant(entity.getCreatedAt()))
                .build();
    }

    private static SalaryDto buildSalaryDto(Job entity) {
        if (entity.getSalaryMin() == null && entity.getSalaryMax() == null) {
            return null;
        }
        
        String formatted = formatSalary(
                entity.getSalaryMin(),
                entity.getSalaryMax(),
                entity.getSalaryCurrency(),
                entity.getSalaryPeriod()
        );

        return SalaryDto.builder()
                .min(entity.getSalaryMin())
                .max(entity.getSalaryMax())
                .currency(entity.getSalaryCurrency())
                .period(entity.getSalaryPeriod())
                .formatted(formatted)
                .build();
    }

    private static String formatSalary(Integer min, Integer max, String currency, String period) {
        if (min == null && max == null) {
            return null;
        }

        String curr = currency != null ? currency : "USD";
        String per = period != null ? "/" + period.toLowerCase() : "/year";

        if (min != null && max != null) {
            return String.format("%s %,d - %,d%s", curr, min, max, per);
        } else if (min != null) {
            return String.format("%s %,d+%s", curr, min, per);
        } else {
            return String.format("Up to %s %,d%s", curr, max, per);
        }
    }

    private static List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? instant.toString() : null;
    }

    private static String formatLocalDate(LocalDate date) {
        return date != null ? date.toString() : null;
    }
}
