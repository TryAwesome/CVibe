package com.cvibe.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for job search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchRequest {

    private String keyword;
    private String location;
    private String type;
    private String experienceLevel;
    private Integer salaryMin;
    private Integer salaryMax;
    private List<String> skills;
    
    @Builder.Default
    private Integer page = 0;
    
    @Builder.Default
    private Integer size = 20;
}
