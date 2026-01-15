package com.cvibe.biz.profile.dto;

import com.cvibe.biz.profile.entity.ProfileSkill;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDto {

    private UUID id;

    @NotBlank
    @Size(max = 100)
    private String name;

    private ProfileSkill.SkillCategory category;

    private ProfileSkill.ProficiencyLevel proficiencyLevel;

    private Integer yearsOfExperience;

    private Boolean isPrimary;
}
