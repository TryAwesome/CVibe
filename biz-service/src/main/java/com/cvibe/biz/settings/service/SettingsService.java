package com.cvibe.biz.settings.service;

import com.cvibe.biz.settings.dto.AiConfigDto;
import com.cvibe.biz.settings.dto.ChangePasswordRequest;
import com.cvibe.biz.settings.dto.UpdateProfileRequest;
import com.cvibe.biz.user.dto.UserResponse;
import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.entity.UserAiConfig;
import com.cvibe.biz.user.repository.UserAiConfigRepository;
import com.cvibe.biz.user.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Settings Service
 * Handles user settings, password change, and AI configuration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserRepository userRepository;
    private final UserAiConfigRepository aiConfigRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Change user password
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Check if user has a password (might be Google-only user)
        if (!user.hasPassword()) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH, 
                    "Cannot change password for Google-only accounts. Set a password first.");
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        // Validate new password
        validatePassword(request.getNewPassword());

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getEmail());
    }

    /**
     * Set password for Google-only users
     */
    @Transactional
    public void setPassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Validate new password
        validatePassword(newPassword);

        // Set password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password set for user: {}", user.getEmail());
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());

        return UserResponse.fromEntity(user);
    }

    /**
     * Get AI configuration
     */
    @Transactional(readOnly = true)
    public AiConfigDto getAiConfig(UUID userId) {
        UserAiConfig config = aiConfigRepository.findByUserId(userId).orElse(null);
        return AiConfigDto.fromEntity(config);
    }

    /**
     * Update AI configuration
     */
    @Transactional
    public AiConfigDto updateAiConfig(UUID userId, AiConfigDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserAiConfig config = aiConfigRepository.findByUserId(userId)
                .orElseGet(() -> UserAiConfig.builder()
                        .user(user)
                        .build());

        // Update fields
        if (request.getBaseUrl() != null) {
            config.setBaseUrl(request.getBaseUrl().trim());
        }
        if (request.getApiKey() != null && !request.getApiKey().contains("****")) {
            // Only update if it's not the masked value
            config.setApiKeyEncrypted(encryptApiKey(request.getApiKey()));
        }
        if (request.getModelName() != null) {
            config.setModelName(request.getModelName().trim());
        }
        if (request.getProvider() != null) {
            config.setProvider(request.getProvider().trim());
        }

        config = aiConfigRepository.save(config);
        log.info("AI config updated for user: {}", user.getEmail());

        return AiConfigDto.fromEntity(config);
    }

    /**
     * Delete AI configuration
     */
    @Transactional
    public void deleteAiConfig(UUID userId) {
        aiConfigRepository.deleteByUserId(userId);
        log.info("AI config deleted for user ID: {}", userId);
    }

    /**
     * Validate password strength
     */
    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD, 
                    "Password must be at least 8 characters long");
        }
        // Add more password rules as needed
    }

    /**
     * Encrypt API key for storage
     * TODO: Implement proper encryption using KMS or similar
     */
    private String encryptApiKey(String apiKey) {
        // For now, store as-is. In production, use proper encryption
        // Example: AES-256 encryption with key from KMS
        return apiKey;
    }
}
