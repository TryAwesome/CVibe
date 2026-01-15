package com.cvibe.biz.profile.dto;

import com.cvibe.biz.profile.entity.ProfileExperience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ExperienceDto {

    private UUID id;

    @NotBlank
    @Size(max = 100)
    private String company;

    @NotBlank
    @Size(max = 100)
    private String title;

    @Size(max = 100)
    private String location;

    private ProfileExperience.EmploymentType employmentType;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean isCurrent;

    private String description;

    private List<String> achievements;

    private List<String> technologies;
}
