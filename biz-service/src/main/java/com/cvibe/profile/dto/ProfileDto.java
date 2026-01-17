package com.cvibe.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for user profile response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {

    private String id;
    private String userId;
    private String headline;
    private String summary;
    private String location;
    private List<ExperienceDto> experiences;
    private List<SkillDto> skills;
    private String createdAt;
    private String updatedAt;
}
