package com.cvibe.profile.dto;

import lombok.*;
import java.util.UUID;

/**
 * Language proficiency DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageDto {
    private UUID id;
    private String language;
    private String proficiency;  // Native, Fluent, Professional, Basic
}
