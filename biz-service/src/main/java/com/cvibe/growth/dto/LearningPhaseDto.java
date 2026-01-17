package com.cvibe.growth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for a phase/section within a learning path
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPhaseDto {

    private String id;
    private String title;
    private String description;
    private String type; // LEARN, PRACTICE, PROJECT
    private List<String> resources;
    private Boolean isCompleted;
    private Integer orderIndex;
    private String completedAt;
}
