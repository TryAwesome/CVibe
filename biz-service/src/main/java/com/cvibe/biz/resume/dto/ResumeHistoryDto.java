package com.cvibe.biz.resume.dto;

import com.cvibe.biz.resume.entity.ResumeHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeHistoryDto {

    private UUID id;
    private String fileName;
    private String originalName;
    private Long fileSize;
    private String contentType;
    private ResumeHistory.ResumeSource source;
    private UUID templateId;
    private String targetJobTitle;
    private String targetCompany;
    private Integer version;
    private Boolean isPrimary;
    private String notes;
    private Instant createdAt;
    private String downloadUrl;  // Presigned URL for download
}
