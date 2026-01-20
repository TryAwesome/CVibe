package com.cvibe.profile.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Certification DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificationDto {
    private UUID id;
    private String name;
    private String issuer;
    private LocalDate issueDate;
    private LocalDate expirationDate;
    private String credentialId;
    private String credentialUrl;
}
