package com.cvibe.auth.service;

import com.cvibe.auth.dto.*;
import com.cvibe.auth.entity.User;
import com.cvibe.auth.entity.UserRole;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import com.cvibe.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for authentication operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Check if email already exists
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 2. Create user
        String nickname = request.getNickname();
        if (nickname == null || nickname.isBlank()) {
            // Use email prefix as default nickname
            nickname = email.split("@")[0];
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(nickname)
                .role(UserRole.ROLE_USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // 3. Generate tokens and return
        return generateAuthResponse(user);
    }

    /**
     * Login with email and password.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. Find user
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 2. Check account status
        if (!user.getEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 3. Check if user has password (Google users may not have one)
        if (user.getPasswordHash() == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS,
                    "This account was created via Google. Please use Google to sign in.");
        }

        // 4. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 5. Update last login time
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        log.info("User logged in: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    /**
     * Login with Google OAuth.
     */
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        // TODO: Implement Google token verification
        // For now, throw unsupported operation
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Google login not yet implemented");
    }

    /**
     * Refresh access token.
     */
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        // 1. Validate token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 2. Check if it's a refresh token
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Not a refresh token");
        }

        // 3. Get user
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4. Check account status
        if (!user.getEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        return generateAuthResponse(user);
    }

    /**
     * Get current user info.
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserDto.fromEntity(user);
    }

    /**
     * Generate authentication response with tokens.
     */
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        long expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000;

        return AuthResponse.of(accessToken, refreshToken, expiresIn, UserDto.fromEntity(user));
    }
}
