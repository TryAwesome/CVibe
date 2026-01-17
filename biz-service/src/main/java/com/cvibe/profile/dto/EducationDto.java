package com.cvibe.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for education data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationDto {

    private String id;
    
    private String school;
    
    private String degree;
    
    private String fieldOfStudy;
    
    private String location;
    
    private String startDate;
    
    private String endDate;
    
    private Boolean isCurrent;
    
    private String gpa;
    
    private String description;
    
    private List<String> activities;
}
