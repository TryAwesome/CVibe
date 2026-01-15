package com.cvibe.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Unified API Response Wrapper
 * 
 * @param <T> Response data type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ErrorInfo error;
    private Meta meta;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        private int code;
        private String message;
        private String details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        private String traceId;
        private Instant timestamp;
    }

    /**
     * Success response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(Meta.builder()
                        .timestamp(Instant.now())
                        .build())
                .build();
    }

    /**
     * Success response without data
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * Error response with error code and message
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorInfo.builder()
                        .code(code)
                        .message(message)
                        .build())
                .meta(Meta.builder()
                        .timestamp(Instant.now())
                        .build())
                .build();
    }

    /**
     * Error response with error code, message and details
     */
    public static <T> ApiResponse<T> error(int code, String message, String details) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorInfo.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .build())
                .meta(Meta.builder()
                        .timestamp(Instant.now())
                        .build())
                .build();
    }

    /**
     * Set trace ID for distributed tracing
     */
    public ApiResponse<T> withTraceId(String traceId) {
        if (this.meta == null) {
            this.meta = Meta.builder().timestamp(Instant.now()).build();
        }
        this.meta.setTraceId(traceId);
        return this;
    }
}
