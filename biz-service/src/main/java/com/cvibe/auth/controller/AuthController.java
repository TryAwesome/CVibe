package com.cvibe.auth.controller;

import com.cvibe.auth.dto.*;
import com.cvibe.auth.service.AuthService;
import com.cvibe.common.dto.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * Base path: /api/auth
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register new user.
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.success(response);
    }

    /**
     * Login with email/password.
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    /**
     * Login with Google OAuth.
     * POST /api/auth/google
     */
    @PostMapping("/google")
    public ApiResponse<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.googleLogin(request);
        return ApiResponse.success(response);
    }

    /**
     * Refresh access token.
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ApiResponse.success(response);
    }

    /**
     * Get current user info.
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ApiResponse<UserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserDto user = authService.getCurrentUser(principal.getUserId());
        return ApiResponse.success(user);
    }

    /**
     * Logout (client-side token removal).
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // Frontend clears tokens - no server-side action needed
        // If blacklist mechanism needed, implement here
        return ApiResponse.success();
    }
}
