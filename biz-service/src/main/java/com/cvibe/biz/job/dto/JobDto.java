package com.cvibe.biz.job.dto;

import com.cvibe.biz.job.entity.Job;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class JobDto {
    private UUID id;
    private String title;
    private String company;
    private String location;
    private String salaryRange;
    private String employmentType;
    private String experienceLevel;
    private String descriptionMarkdown;
    private String sourceUrl;
    private String source;
    private Boolean isRemote;
    private Instant firstSeenAt;

    public static JobDto from(Job job) {
        return JobDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .location(job.getLocation())
                .salaryRange(job.getSalaryRange())
                .employmentType(job.getEmploymentType() != null ? job.getEmploymentType().name() : null)
                .experienceLevel(job.getExperienceLevel() != null ? job.getExperienceLevel().name() : null)
                .descriptionMarkdown(job.getDescriptionMarkdown())
                .sourceUrl(job.getSourceUrl())
                .source(job.getSource() != null ? job.getSource().name() : null)
                .isRemote(job.getIsRemote())
                .firstSeenAt(job.getFirstSeenAt())
                .build();
    }
}
