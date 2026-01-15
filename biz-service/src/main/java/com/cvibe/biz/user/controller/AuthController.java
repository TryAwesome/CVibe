package com.cvibe.biz.user.controller;

import com.cvibe.biz.user.dto.*;
import com.cvibe.biz.user.service.AuthService;
import com.cvibe.common.response.ApiResponse;
import com.cvibe.common.security.JwtTokenProvider;
import com.cvibe.common.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles user registration, login, logout, and token refresh
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String COOKIE_NAME = "CA_SESSION";
    private static final String REFRESH_COOKIE_NAME = "CA_REFRESH";

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Register a new user
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        
        AuthResponse authResponse = authService.register(request);
        setAuthCookies(response, authResponse);
        return ApiResponse.success(authResponse);
    }

    /**
     * Login with email and password
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        
        AuthResponse authResponse = authService.login(request);
        setAuthCookies(response, authResponse);
        return ApiResponse.success(authResponse);
    }

    /**
     * Login with Google OAuth
     * POST /api/auth/google
     */
    @PostMapping("/google")
    public ApiResponse<AuthResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse response) {
        
        AuthResponse authResponse = authService.googleLogin(request);
        setAuthCookies(response, authResponse);
        return ApiResponse.success(authResponse);
    }

    /**
     * Refresh access token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletResponse response) {
        
        // Try to get refresh token from cookie first, then from body
        String token = refreshToken != null ? refreshToken : (body != null ? body.getRefreshToken() : null);
        
        if (token == null) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        
        AuthResponse authResponse = authService.refreshToken(token);
        setAuthCookies(response, authResponse);
        return ApiResponse.success(authResponse);
    }

    /**
     * Get current user info
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse user = authService.getCurrentUser(principal.getId());
        return ApiResponse.success(user);
    }

    /**
     * Logout
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        clearAuthCookies(response);
        return ApiResponse.success();
    }

    /**
     * Set authentication cookies
     */
    private void setAuthCookies(HttpServletResponse response, AuthResponse authResponse) {
        // Access token cookie
        Cookie accessCookie = new Cookie(COOKIE_NAME, authResponse.getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(isSecure());
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (jwtTokenProvider.getAccessTokenExpiration() / 1000));
        response.addCookie(accessCookie);

        // Refresh token cookie
        Cookie refreshCookie = new Cookie(REFRESH_COOKIE_NAME, authResponse.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(isSecure());
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge((int) (jwtTokenProvider.getRefreshTokenExpiration() / 1000));
        response.addCookie(refreshCookie);
    }

    /**
     * Clear authentication cookies
     */
    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie(COOKIE_NAME, "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(isSecure());
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie(REFRESH_COOKIE_NAME, "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(isSecure());
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }

    /**
     * Check if cookies should be secure (HTTPS)
     */
    private boolean isSecure() {
        return !allowedOrigins.contains("localhost");
    }

    /**
     * Refresh Token Request DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RefreshTokenRequest {
        private String refreshToken;
    }
}
