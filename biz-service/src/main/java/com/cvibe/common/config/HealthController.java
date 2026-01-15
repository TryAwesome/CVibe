package com.cvibe.common.config;

import com.cvibe.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Health check endpoint
     * GET /api/health
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", Instant.now());
        healthInfo.put("service", "cvibe-biz-service");
        
        // Check database connection
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            healthInfo.put("database", "UP");
        } catch (Exception e) {
            healthInfo.put("database", "DOWN");
            healthInfo.put("database_error", e.getMessage());
        }

        return ApiResponse.success(healthInfo);
    }
}
