package com.cvibe.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for project data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {

    private String id;
    
    private String name;
    
    private String description;
    
    private String url;
    
    private String repoUrl;
    
    private List<String> technologies;
    
    private String startDate;
    
    private String endDate;
    
    private Boolean isCurrent;
    
    private List<String> highlights;
}
