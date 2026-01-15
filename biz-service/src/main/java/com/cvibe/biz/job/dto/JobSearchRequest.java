package com.cvibe.biz.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchRequest {
    private String keyword;
    private String company;
    private String location;
    private String experienceLevel;
    private String employmentType;
    private Boolean remote;
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 20;
}
