package com.cvibe.auth.dto;

import com.cvibe.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a user in API responses.
 * 
 * IMPORTANT: Uses 'nickname' field to match frontend expectations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private String id;
    private String email;
    private String nickname;  // NOT fullName - matches frontend expectations
    private String role;
    private boolean hasPassword;
    private String createdAt;
    private boolean googleUser;

    /**
     * Create UserDto from User entity.
     */
    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .hasPassword(user.hasPassword())
                .createdAt(user.getCreatedAt().toString())
                .googleUser(user.isGoogleUser())
                .build();
    }
}
