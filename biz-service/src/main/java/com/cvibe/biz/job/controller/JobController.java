package com.cvibe.biz.job.controller;

import com.cvibe.biz.job.dto.*;
import com.cvibe.biz.job.service.JobMatchingService;
import com.cvibe.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JobController
 * 
 * REST API endpoints for job search, matching, and user interactions.
 */
@RestController
@RequestMapping("/v1/jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Job Matching", description = "Job search and matching APIs")
public class JobController {

    private final JobMatchingService matchingService;

    // ================== Job Search ==================

    @GetMapping
    @Operation(summary = "Search jobs with filters")
    public ResponseEntity<ApiResponse<Page<JobDto>>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        JobSearchRequest request = JobSearchRequest.builder()
                .keyword(keyword)
                .company(company)
                .location(location)
                .experienceLevel(experienceLevel)
                .employmentType(employmentType)
                .remote(remote)
                .page(page)
                .size(size)
                .build();

        Page<JobDto> jobs = matchingService.searchJobs(request);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get job details by ID")
    public ResponseEntity<ApiResponse<JobDto>> getJob(@PathVariable UUID jobId) {
        JobDto job = matchingService.getJob(jobId);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest job postings")
    public ResponseEntity<ApiResponse<Page<JobDto>>> getLatestJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<JobDto> jobs = matchingService.getLatestJobs(page, size);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/remote")
    @Operation(summary = "Get remote job opportunities")
    public ResponseEntity<ApiResponse<Page<JobDto>>> getRemoteJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<JobDto> jobs = matchingService.getRemoteJobs(page, size);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    // ================== Job Matching ==================

    @PostMapping("/matches/generate")
    @Operation(summary = "Generate job matches based on user profile")
    public ResponseEntity<ApiResponse<List<JobMatchDto>>> generateMatches(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        List<JobMatchDto> matches = matchingService.generateMatchesForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(matches));
    }

    @GetMapping("/matches")
    @Operation(summary = "Get user's job matches")
    public ResponseEntity<ApiResponse<Page<JobMatchDto>>> getUserMatches(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "score") String sortBy) {

        UUID userId = extractUserId(userDetails);
        Page<JobMatchDto> matches = matchingService.getUserMatches(userId, page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success(matches));
    }

    @GetMapping("/matches/summary")
    @Operation(summary = "Get match summary for dashboard")
    public ResponseEntity<ApiResponse<JobMatchSummary>> getMatchSummary(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        JobMatchSummary summary = matchingService.getMatchSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // ================== Match Actions ==================

    @PostMapping("/matches/{matchId}/view")
    @Operation(summary = "Mark match as viewed")
    public ResponseEntity<ApiResponse<JobMatchDto>> markViewed(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID matchId) {

        UUID userId = extractUserId(userDetails);
        JobMatchDto match = matchingService.markMatchViewed(userId, matchId);
        return ResponseEntity.ok(ApiResponse.success(match));
    }

    @PostMapping("/matches/{matchId}/save")
    @Operation(summary = "Toggle save status for a match")
    public ResponseEntity<ApiResponse<JobMatchDto>> toggleSave(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID matchId) {

        UUID userId = extractUserId(userDetails);
        JobMatchDto match = matchingService.toggleSaved(userId, matchId);
        return ResponseEntity.ok(ApiResponse.success(match));
    }

    @PostMapping("/matches/{matchId}/apply")
    @Operation(summary = "Mark job as applied")
    public ResponseEntity<ApiResponse<JobMatchDto>> markApplied(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID matchId) {

        UUID userId = extractUserId(userDetails);
        JobMatchDto match = matchingService.markApplied(userId, matchId);
        return ResponseEntity.ok(ApiResponse.success(match));
    }

    @PostMapping("/matches/{matchId}/feedback")
    @Operation(summary = "Submit feedback for a match")
    public ResponseEntity<ApiResponse<JobMatchDto>> submitFeedback(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID matchId,
            @RequestBody MatchFeedbackRequest request) {

        UUID userId = extractUserId(userDetails);
        JobMatchDto match = matchingService.submitFeedback(userId, matchId, request);
        return ResponseEntity.ok(ApiResponse.success(match));
    }

    // ================== Saved & Applied Lists ==================

    @GetMapping("/saved")
    @Operation(summary = "Get saved jobs")
    public ResponseEntity<ApiResponse<List<JobMatchDto>>> getSavedJobs(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        List<JobMatchDto> saved = matchingService.getSavedJobs(userId);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @GetMapping("/applied")
    @Operation(summary = "Get applied jobs")
    public ResponseEntity<ApiResponse<List<JobMatchDto>>> getAppliedJobs(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        List<JobMatchDto> applied = matchingService.getAppliedJobs(userId);
        return ResponseEntity.ok(ApiResponse.success(applied));
    }

    // ================== Admin Endpoints ==================

    @GetMapping("/admin/statistics")
    @Operation(summary = "Get job statistics (admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getJobStatistics() {
        Map<String, Object> stats = matchingService.getJobStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ================== Helper Methods ==================

    private UUID extractUserId(UserDetails userDetails) {
        // UserDetails.getUsername() returns the user ID as string
        return UUID.fromString(userDetails.getUsername());
    }
}
