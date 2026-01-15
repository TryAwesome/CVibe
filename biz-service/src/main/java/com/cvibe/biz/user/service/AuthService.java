package com.cvibe.biz.user.service;

import com.cvibe.biz.user.dto.*;
import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.response.ErrorCode;
import com.cvibe.common.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Authentication Service
 * Handles user registration, login, and Google OAuth
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Value("${google.oauth2.client-id}")
    private String googleClientId;

    /**
     * Register a new user with email and password
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Validate password strength
        validatePassword(request.getPassword());

        // Create new user
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(User.UserRole.ROLE_USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    /**
     * Login with email and password
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // Check if account is enabled
        if (!user.getEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // Check if user has password (might be Google-only user)
        if (!user.hasPassword()) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, 
                    "This account was created via Google login. Please use Google to sign in.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Update last login time
        userRepository.updateLastLoginAt(user.getId(), Instant.now());
        log.info("User logged in: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    /**
     * Login or register with Google OAuth
     */
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        // Verify and decode Google ID token
        GoogleUserInfo googleUser = verifyGoogleToken(request.getIdToken());

        // Find existing user by Google Sub or email
        User user = userRepository.findByGoogleSub(googleUser.getSub())
                .orElseGet(() -> userRepository.findByEmail(googleUser.getEmail())
                        .map(existingUser -> {
                            // Link Google account to existing user
                            existingUser.setGoogleSub(googleUser.getSub());
                            if (existingUser.getAvatarUrl() == null) {
                                existingUser.setAvatarUrl(googleUser.getPicture());
                            }
                            return userRepository.save(existingUser);
                        })
                        .orElseGet(() -> createGoogleUser(googleUser)));

        // Check if account is enabled
        if (!user.getEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // Update last login time
        userRepository.updateLastLoginAt(user.getId(), Instant.now());
        log.info("User logged in via Google: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Not a refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.getEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        return generateAuthResponse(user);
    }

    /**
     * Get current user info
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.fromEntity(user);
    }

    /**
     * Generate auth response with tokens
     */
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration() / 1000,
                UserResponse.fromEntity(user)
        );
    }

    /**
     * Verify Google ID token and extract user info
     */
    private GoogleUserInfo verifyGoogleToken(String idToken) {
        try {
            // Decode JWT without verification first to get the payload
            // In production, you should use Google's tokeninfo endpoint or library
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED, "Invalid token format");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = objectMapper.readTree(payload);

            // Verify token with Google's tokeninfo API
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                log.warn("Google token verification failed: {}", response.body());
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED, "Token verification failed");
            }

            JsonNode verifiedClaims = objectMapper.readTree(response.body());
            
            // Verify audience matches our client ID
            String aud = verifiedClaims.has("aud") ? verifiedClaims.get("aud").asText() : "";
            if (!googleClientId.equals(aud)) {
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED, "Invalid token audience");
            }

            return GoogleUserInfo.builder()
                    .sub(verifiedClaims.get("sub").asText())
                    .email(verifiedClaims.get("email").asText())
                    .name(verifiedClaims.has("name") ? verifiedClaims.get("name").asText() : null)
                    .picture(verifiedClaims.has("picture") ? verifiedClaims.get("picture").asText() : null)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify Google token", e);
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED, e.getMessage());
        }
    }

    /**
     * Create new user from Google OAuth info
     */
    private User createGoogleUser(GoogleUserInfo googleUser) {
        User user = User.builder()
                .email(googleUser.getEmail().toLowerCase().trim())
                .googleSub(googleUser.getSub())
                .fullName(googleUser.getName())
                .avatarUrl(googleUser.getPicture())
                .role(User.UserRole.ROLE_USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("New user created via Google: {}", user.getEmail());
        return user;
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
     * Google User Info DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class GoogleUserInfo {
        private String sub;
        private String email;
        private String name;
        private String picture;
    }
}
