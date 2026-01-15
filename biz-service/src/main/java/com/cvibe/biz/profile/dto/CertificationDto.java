package com.cvibe.biz.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificationDto {

    private UUID id;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 100)
    private String issuingOrganization;

    private LocalDate issueDate;

    private LocalDate expirationDate;

    @Size(max = 100)
    private String credentialId;

    private String credentialUrl;
}
