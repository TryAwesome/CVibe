package com.cvibe.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Business Error Code Definitions
 * 
 * Error Code Ranges:
 * - 10000 - 19999: General / System Errors
 * - 20000 - 29999: User & Auth
 * - 30000 - 39999: Resume & File
 * - 40000 - 49999: Job & Search
 * - 50000 - 59999: AI Engine
 * - 60000 - 69999: Community
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==================== System Errors (10000 - 19999) ====================
    INTERNAL_ERROR(10001, "Internal server error"),
    INVALID_REQUEST(10002, "Invalid request"),
    RESOURCE_NOT_FOUND(10003, "Resource not found"),
    METHOD_NOT_ALLOWED(10004, "Method not allowed"),
    SERVICE_UNAVAILABLE(10005, "Service temporarily unavailable"),
    RATE_LIMIT_EXCEEDED(10006, "Rate limit exceeded"),
    VALIDATION_FAILED(10007, "Validation failed"),
    INVALID_OPERATION(10008, "Invalid operation for current state"),

    // ==================== Auth Errors (20000 - 29999) ====================
    UNAUTHORIZED(20001, "Unauthorized - Please login"),
    INVALID_TOKEN(20002, "Invalid or expired token"),
    ACCESS_DENIED(20003, "Access denied - Insufficient permissions"),
    USER_NOT_FOUND(20004, "User not found"),
    EMAIL_ALREADY_EXISTS(20005, "Email already registered"),
    INVALID_CREDENTIALS(20006, "Invalid email or password"),
    ACCOUNT_DISABLED(20007, "Account has been disabled"),
    GOOGLE_AUTH_FAILED(20008, "Google authentication failed"),
    PASSWORD_MISMATCH(20009, "Current password is incorrect"),
    WEAK_PASSWORD(20010, "Password does not meet requirements"),
    GOOGLE_SUB_ALREADY_EXISTS(20011, "Google account already linked to another user"),

    // ==================== Resume & File Errors (30000 - 39999) ====================
    FILE_UPLOAD_FAILED(30001, "File upload failed"),
    FILE_NOT_FOUND(30002, "File not found"),
    INVALID_FILE_TYPE(30003, "Invalid file type"),
    FILE_TOO_LARGE(30004, "File size exceeds limit"),
    FILE_DELETE_FAILED(30005, "File deletion failed"),
    RESUME_PARSING_FAILED(30006, "Resume parsing failed"),
    RESUME_NOT_FOUND(30007, "Resume not found"),
    TEMPLATE_NOT_FOUND(30008, "Template not found"),
    LATEX_COMPILE_FAILED(30009, "LaTeX compilation failed"),
    PROFILE_NOT_FOUND(30010, "Profile not found"),

    // ==================== Job & Search Errors (40000 - 49999) ====================
    JOB_NOT_FOUND(40001, "Job not found"),
    SEARCH_FAILED(40002, "Search service error"),
    NO_MATCHING_JOBS(40003, "No matching jobs found"),

    // ==================== AI Engine Errors (50000 - 59999) ====================
    AI_SERVICE_ERROR(50001, "AI service error"),
    AI_QUOTA_EXCEEDED(50002, "AI quota exceeded"),
    AI_SAFETY_BLOCK(50003, "Content blocked by safety filters"),
    AI_CONFIG_INVALID(50004, "Invalid AI configuration"),
    INTERVIEW_SESSION_NOT_FOUND(50005, "Interview session not found"),
    INTERVIEW_ALREADY_COMPLETED(50006, "Interview session already completed"),

    // ==================== Community Errors (60000 - 69999) ====================
    POST_NOT_FOUND(60001, "Post not found"),
    COMMENT_NOT_FOUND(60002, "Comment not found"),
    ALREADY_LIKED(60003, "Already liked this post"),
    NOT_LIKED_YET(60004, "Not liked this post yet"),

    // ==================== Admin Errors (70000 - 79999) ====================
    NOT_FOUND(70001, "Resource not found"),
    DUPLICATE_ENTRY(70002, "Duplicate entry"),
    CONFIG_NOT_FOUND(70003, "Configuration not found"),
    ANNOUNCEMENT_NOT_FOUND(70004, "Announcement not found");

    private final int code;
    private final String message;

    /**
     * Get HTTP status code based on error code range
     */
    public int getHttpStatus() {
        return switch (this.code / 10000) {
            case 1 -> switch (this) {
                case INTERNAL_ERROR, SERVICE_UNAVAILABLE -> 500;
                case RESOURCE_NOT_FOUND -> 404;
                case METHOD_NOT_ALLOWED -> 405;
                case RATE_LIMIT_EXCEEDED -> 429;
                default -> 400;
            };
            case 2 -> switch (this) {
                case UNAUTHORIZED, INVALID_TOKEN -> 401;
                case ACCESS_DENIED -> 403;
                case USER_NOT_FOUND -> 404;
                default -> 400;
            };
            case 3 -> switch (this) {
                case FILE_NOT_FOUND, RESUME_NOT_FOUND, TEMPLATE_NOT_FOUND, PROFILE_NOT_FOUND -> 404;
                default -> 400;
            };
            case 4 -> switch (this) {
                case JOB_NOT_FOUND -> 404;
                default -> 400;
            };
            case 5 -> switch (this) {
                case AI_SERVICE_ERROR -> 503;
                case INTERVIEW_SESSION_NOT_FOUND -> 404;
                default -> 400;
            };
            case 6 -> switch (this) {
                case POST_NOT_FOUND, COMMENT_NOT_FOUND -> 404;
                default -> 400;
            };
            default -> 500;
        };
    }
}
