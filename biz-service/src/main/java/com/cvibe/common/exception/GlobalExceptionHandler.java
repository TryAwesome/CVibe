package com.cvibe.common.exception;

import com.cvibe.common.response.ApiResponse;
import com.cvibe.common.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * Global Exception Handler
 * Catches and formats all exceptions into unified API responses
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle Business Exceptions
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        
        log.warn("Business exception: code={}, message={}, details={}, path={}",
                ex.getCode(), ex.getMessage(), ex.getDetails(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.<Void>error(
                ex.getCode(),
                ex.getMessage(),
                ex.getDetails()
        ).withTraceId(getTraceId(request));

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * Handle Validation Exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Validation failed: {}, path={}", details, request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.<Void>error(
                ErrorCode.VALIDATION_FAILED.getCode(),
                ErrorCode.VALIDATION_FAILED.getMessage(),
                details
        ).withTraceId(getTraceId(request));

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle Authentication Exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("Authentication failed: {}, path={}", ex.getMessage(), request.getRequestURI());

        ErrorCode errorCode = ex instanceof BadCredentialsException
                ? ErrorCode.INVALID_CREDENTIALS
                : ErrorCode.UNAUTHORIZED;

        ApiResponse<Void> response = ApiResponse.<Void>error(
                errorCode.getCode(),
                errorCode.getMessage()
        ).withTraceId(getTraceId(request));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle Access Denied Exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied: {}, path={}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.<Void>error(
                ErrorCode.ACCESS_DENIED.getCode(),
                ErrorCode.ACCESS_DENIED.getMessage()
        ).withTraceId(getTraceId(request));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle Missing Request Parameter
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        log.warn("Missing parameter: {}, path={}", ex.getParameterName(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.<Void>error(
                ErrorCode.INVALID_REQUEST.getCode(),
                "Missing required parameter: " + ex.getParameterName()
        ).withTraceId(getTraceId(request));

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle Invalid JSON
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Invalid JSON: {}, path={}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.<Void>error(
                ErrorCode.INVALID_REQUEST.getCode(),
                "Invalid request body format"
        ).withTraceId(getTraceId(request));

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle Method Not Supported
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("Method not supported: {} for path={}", ex.getMethod(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.<Void>error(
                ErrorCode.METHOD_NOT_ALLOWED.getCode(),
                "Method " + ex.getMethod() + " not supported"
        ).withTraceId(getTraceId(request));

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Handle Resource Not Found (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            NoHandlerFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found: {}", request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.<Void>error(
                ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                ErrorCode.RESOURCE_NOT_FOUND.getMessage()
        ).withTraceId(getTraceId(request));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle File Size Exceeded
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        log.warn("File too large: {}, path={}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.<Void>error(
                ErrorCode.FILE_TOO_LARGE.getCode(),
                ErrorCode.FILE_TOO_LARGE.getMessage()
        ).withTraceId(getTraceId(request));

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle All Other Exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error: path={}", request.getRequestURI(), ex);

        ApiResponse<Void> response = ApiResponse.<Void>error(
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getMessage()
        ).withTraceId(getTraceId(request));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Extract trace ID from request header
     */
    private String getTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-ID");
        return traceId != null ? traceId : java.util.UUID.randomUUID().toString();
    }
}
