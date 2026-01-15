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
public class EducationDto {

    private UUID id;

    @NotBlank
    @Size(max = 150)
    private String institution;

    @Size(max = 100)
    private String degree;

    @Size(max = 100)
    private String fieldOfStudy;

    @Size(max = 100)
    private String location;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean isCurrent;

    @Size(max = 20)
    private String gpa;

    private List<String> activities;

    private List<String> honors;
}
