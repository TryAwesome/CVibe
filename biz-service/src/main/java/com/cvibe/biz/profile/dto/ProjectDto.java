package com.cvibe.biz.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {

    private UUID id;

    @NotBlank
    @Size(max = 150)
    private String name;

    private String description;

    @Size(max = 100)
    private String role;

    private LocalDate startDate;

    private LocalDate endDate;

    private String projectUrl;

    private String sourceUrl;

    private List<String> technologies;

    private List<String> highlights;
}
