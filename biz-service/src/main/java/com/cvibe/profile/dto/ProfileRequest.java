package com.cvibe.profile.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating basic profile information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {

    @Size(max = 200, message = "Headline must be at most 200 characters")
    private String headline;

    private String summary;

    @Size(max = 100, message = "Location must be at most 100 characters")
    private String location;
}
