package com.cvibe.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Unified error codes for the application.
 * 
 * Error code ranges:
 * - 1000-1999: System errors
 * - 10000-10999: Authentication errors
 * - 20000-20999: Authorization errors
 * - 30000-30999: Profile errors
 * - 40000-40999: Resume errors
 * - 50000-50999: Resume Builder errors
 * - 60000-60999: Interview errors
 * - 70000-70999: Mock Interview errors
 * - 80000-80999: Growth errors
 * - 90000-90999: Job errors
 * - 100000-100999: Community errors
 * - 110000-110999: Notification errors
 * - 120000-120999: Settings errors
 * - 130000-130999: AI Service errors
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==================== System Errors (1000-1999) ====================
    INTERNAL_ERROR(1000, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    SERVICE_UNAVAILABLE(1001, HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable"),
    INVALID_REQUEST(1002, HttpStatus.BAD_REQUEST, "Invalid request"),
    RATE_LIMITED(1003, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"),
    REQUEST_TIMEOUT(1004, HttpStatus.GATEWAY_TIMEOUT, "Request timeout"),

    // ==================== Authentication Errors (10000-10999) ====================
    UNAUTHORIZED(10000, HttpStatus.UNAUTHORIZED, "Unauthorized"),
    INVALID_CREDENTIALS(10001, HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    TOKEN_EXPIRED(10002, HttpStatus.UNAUTHORIZED, "Token expired"),
    TOKEN_INVALID(10003, HttpStatus.UNAUTHORIZED, "Invalid token"),
    EMAIL_ALREADY_EXISTS(10004, HttpStatus.BAD_REQUEST, "Email already exists"),
    USER_NOT_FOUND(10005, HttpStatus.NOT_FOUND, "User not found"),
    REFRESH_TOKEN_INVALID(10006, HttpStatus.UNAUTHORIZED, "Invalid refresh token"),
    ACCOUNT_DISABLED(10007, HttpStatus.FORBIDDEN, "Account is disabled"),

    // ==================== Authorization Errors (20000-20999) ====================
    FORBIDDEN(20000, HttpStatus.FORBIDDEN, "Access denied"),
    INSUFFICIENT_PERMISSIONS(20001, HttpStatus.FORBIDDEN, "Insufficient permissions"),

    // ==================== Profile Errors (30000-30999) ====================
    PROFILE_NOT_FOUND(30001, HttpStatus.NOT_FOUND, "Profile not found"),
    EXPERIENCE_NOT_FOUND(30002, HttpStatus.NOT_FOUND, "Experience not found"),
    SKILL_NOT_FOUND(30003, HttpStatus.NOT_FOUND, "Skill not found"),
    SKILL_ALREADY_EXISTS(30004, HttpStatus.BAD_REQUEST, "Skill already exists"),
    EDUCATION_NOT_FOUND(30005, HttpStatus.NOT_FOUND, "Education not found"),
    PROJECT_NOT_FOUND(30006, HttpStatus.NOT_FOUND, "Project not found"),

    // ==================== Resume Errors (40000-40999) ====================
    RESUME_NOT_FOUND(40001, HttpStatus.NOT_FOUND, "Resume not found"),
    FILE_EMPTY(40002, HttpStatus.BAD_REQUEST, "File is empty"),
    FILE_TOO_LARGE(40003, HttpStatus.BAD_REQUEST, "File too large (max 5MB)"),
    FILE_TYPE_NOT_ALLOWED(40004, HttpStatus.BAD_REQUEST, "File type not allowed"),
    FILE_UPLOAD_FAILED(40005, HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed"),
    RESUME_PARSE_FAILED(40006, HttpStatus.INTERNAL_SERVER_ERROR, "Resume parsing failed"),

    // ==================== Resume Builder Errors (50000-50999) ====================
    TEMPLATE_NOT_FOUND(50001, HttpStatus.NOT_FOUND, "Template not found"),
    PROFILE_EMPTY(50002, HttpStatus.BAD_REQUEST, "Profile is empty"),
    RESUME_GENERATION_FAILED(50003, HttpStatus.INTERNAL_SERVER_ERROR, "Resume generation failed"),

    // ==================== Interview Errors (60000-60999) ====================
    SESSION_NOT_FOUND(60001, HttpStatus.NOT_FOUND, "Session not found"),
    INVALID_INTERVIEW_TYPE(60002, HttpStatus.BAD_REQUEST, "Invalid interview type"),
    SESSION_NOT_ACTIVE(60003, HttpStatus.BAD_REQUEST, "Session is not active"),
    SESSION_ALREADY_ENDED(60004, HttpStatus.BAD_REQUEST, "Session already ended"),

    // ==================== Mock Interview Errors (70000-70999) ====================
    MOCK_SESSION_NOT_FOUND(70001, HttpStatus.NOT_FOUND, "Mock interview session not found"),
    QUESTION_INDEX_OUT_OF_RANGE(70002, HttpStatus.BAD_REQUEST, "Question index out of range"),
    FEEDBACK_NOT_READY(70003, HttpStatus.BAD_REQUEST, "Feedback not ready yet"),
    INVALID_MEDIA_FILE(70004, HttpStatus.BAD_REQUEST, "Invalid media file"),

    // ==================== Growth Errors (80000-80999) ====================
    GOAL_NOT_FOUND(80001, HttpStatus.NOT_FOUND, "Goal not found"),
    INVALID_GOAL_STATUS(80002, HttpStatus.BAD_REQUEST, "Invalid goal status"),
    INVALID_DATE_FORMAT(80003, HttpStatus.BAD_REQUEST, "Invalid date format"),
    LEARNING_PATH_NOT_FOUND(80004, HttpStatus.NOT_FOUND, "Learning path not found"),
    MILESTONE_NOT_FOUND(80005, HttpStatus.NOT_FOUND, "Milestone not found"),

    // ==================== Job Errors (90000-90999) ====================
    JOB_NOT_FOUND(90001, HttpStatus.NOT_FOUND, "Job not found"),
    INVALID_SEARCH_PARAMS(90002, HttpStatus.BAD_REQUEST, "Invalid search parameters"),
    SEARCH_SERVICE_UNAVAILABLE(90003, HttpStatus.SERVICE_UNAVAILABLE, "Search service unavailable"),
    JOB_MATCH_NOT_FOUND(90004, HttpStatus.NOT_FOUND, "Job match not found"),
    RESOURCE_NOT_FOUND(90005, HttpStatus.NOT_FOUND, "Resource not found"),

    // ==================== Community Errors (100000-100999) ====================
    POST_NOT_FOUND(100001, HttpStatus.NOT_FOUND, "Post not found"),
    COMMENT_NOT_FOUND(100002, HttpStatus.NOT_FOUND, "Comment not found"),
    INVALID_CATEGORY(100003, HttpStatus.BAD_REQUEST, "Invalid category"),
    CONTENT_TOO_LONG(100004, HttpStatus.BAD_REQUEST, "Content too long"),

    // ==================== Notification Errors (110000-110999) ====================
    NOTIFICATION_NOT_FOUND(110001, HttpStatus.NOT_FOUND, "Notification not found"),
    INVALID_NOTIFICATION_TYPE(110002, HttpStatus.BAD_REQUEST, "Invalid notification type"),

    // ==================== Settings Errors (120000-120999) ====================
    INVALID_PASSWORD(120001, HttpStatus.BAD_REQUEST, "Invalid current password"),
    PASSWORD_SAME_AS_OLD(120002, HttpStatus.BAD_REQUEST, "New password same as old"),
    INVALID_LANGUAGE(120003, HttpStatus.BAD_REQUEST, "Invalid language"),
    INVALID_RESPONSE_STYLE(120004, HttpStatus.BAD_REQUEST, "Invalid response style"),
    INVALID_DIFFICULTY(120005, HttpStatus.BAD_REQUEST, "Invalid difficulty"),

    // ==================== AI Service Errors (130000-130999) ====================
    AI_SERVICE_UNAVAILABLE(130001, HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable"),
    AI_REQUEST_TIMEOUT(130002, HttpStatus.GATEWAY_TIMEOUT, "AI request timeout"),
    AI_RESPONSE_INVALID(130003, HttpStatus.INTERNAL_SERVER_ERROR, "Invalid AI response");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
