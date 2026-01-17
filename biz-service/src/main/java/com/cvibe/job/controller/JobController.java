package com.cvibe.job.controller;

import com.cvibe.common.dto.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import com.cvibe.job.dto.*;
import com.cvibe.job.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for job management
 * 
 * API Base Path: /api/v1/jobs
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /**
     * Search jobs with criteria
     * GET /api/v1/jobs
     */
    @GetMapping
    public ApiResponse<PagedResponse<JobDto>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) Integer salaryMin,
            @RequestParam(required = false) Integer salaryMax,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Searching jobs: keyword={}, location={}, type={}", keyword, location, type);
        
        JobSearchRequest request = JobSearchRequest.builder()
                .keyword(keyword)
                .location(location)
                .type(type)
                .experienceLevel(experienceLevel)
                .salaryMin(salaryMin)
                .salaryMax(salaryMax)
                .skills(skills)
                .page(page)
                .size(size)
                .build();
        
        PagedResponse<JobDto> response = jobService.searchJobs(request);
        return ApiResponse.success(response);
    }

    /**
     * Get latest jobs
     * GET /api/v1/jobs/latest
     */
    @GetMapping("/latest")
    public ApiResponse<PagedResponse<JobDto>> getLatestJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting latest jobs: page={}, size={}", page, size);
        PagedResponse<JobDto> response = jobService.getLatestJobs(page, size);
        return ApiResponse.success(response);
    }

    /**
     * Get remote jobs
     * GET /api/v1/jobs/remote
     */
    @GetMapping("/remote")
    public ApiResponse<PagedResponse<JobDto>> getRemoteJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting remote jobs: page={}, size={}", page, size);
        PagedResponse<JobDto> response = jobService.getRemoteJobs(page, size);
        return ApiResponse.success(response);
    }

    /**
     * Get job by ID
     * GET /api/v1/jobs/{jobId}
     */
    @GetMapping("/{jobId}")
    public ApiResponse<JobDto> getJob(@PathVariable UUID jobId) {
        log.info("Getting job: {}", jobId);
        JobDto job = jobService.getJob(jobId);
        return ApiResponse.success(job);
    }

    /**
     * Generate job matches for current user
     * POST /api/v1/jobs/matches/generate
     */
    @PostMapping("/matches/generate")
    public ApiResponse<List<JobMatchDto>> generateMatches(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Generating matches for user: {}", principal.getId());
        List<JobMatchDto> matches = jobService.generateMatches(principal.getId());
        return ApiResponse.success(matches);
    }

    /**
     * Get user's job matches
     * GET /api/v1/jobs/matches
     */
    @GetMapping("/matches")
    public ApiResponse<PagedResponse<JobMatchDto>> getMatches(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting matches for user: {}", principal.getId());
        PagedResponse<JobMatchDto> response = jobService.getMatches(principal.getId(), page, size);
        return ApiResponse.success(response);
    }

    /**
     * Get match summary for current user
     * GET /api/v1/jobs/matches/summary
     */
    @GetMapping("/matches/summary")
    public ApiResponse<JobMatchSummaryDto> getMatchSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Getting match summary for user: {}", principal.getId());
        JobMatchSummaryDto summary = jobService.getMatchSummary(principal.getId());
        return ApiResponse.success(summary);
    }

    /**
     * Mark match as viewed
     * POST /api/v1/jobs/matches/{matchId}/view
     */
    @PostMapping("/matches/{matchId}/view")
    public ApiResponse<JobMatchDto> markViewed(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID matchId) {
        
        log.info("Marking match {} as viewed for user: {}", matchId, principal.getId());
        JobMatchDto match = jobService.markViewed(principal.getId(), matchId);
        return ApiResponse.success(match);
    }

    /**
     * Save a job match
     * POST /api/v1/jobs/matches/{matchId}/save
     */
    @PostMapping("/matches/{matchId}/save")
    public ApiResponse<JobMatchDto> saveMatch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID matchId) {
        
        log.info("Saving match {} for user: {}", matchId, principal.getId());
        JobMatchDto match = jobService.saveMatch(principal.getId(), matchId);
        return ApiResponse.success(match);
    }

    /**
     * Mark match as applied
     * POST /api/v1/jobs/matches/{matchId}/apply
     */
    @PostMapping("/matches/{matchId}/apply")
    public ApiResponse<JobMatchDto> markApplied(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID matchId) {
        
        log.info("Marking match {} as applied for user: {}", matchId, principal.getId());
        JobMatchDto match = jobService.markApplied(principal.getId(), matchId);
        return ApiResponse.success(match);
    }

    /**
     * Get saved jobs
     * GET /api/v1/jobs/saved
     */
    @GetMapping("/saved")
    public ApiResponse<PagedResponse<JobDto>> getSavedJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting saved jobs for user: {}", principal.getId());
        PagedResponse<JobDto> response = jobService.getSavedJobs(principal.getId(), page, size);
        return ApiResponse.success(response);
    }

    /**
     * Get applied jobs
     * GET /api/v1/jobs/applied
     */
    @GetMapping("/applied")
    public ApiResponse<PagedResponse<JobMatchDto>> getAppliedJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting applied jobs for user: {}", principal.getId());
        PagedResponse<JobMatchDto> response = jobService.getAppliedJobs(principal.getId(), page, size);
        return ApiResponse.success(response);
    }

    /**
     * Unsave a job
     * DELETE /api/v1/jobs/saved/{jobId}
     */
    @DeleteMapping("/saved/{jobId}")
    public ApiResponse<Void> unsaveJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {
        
        log.info("Unsaving job {} for user: {}", jobId, principal.getId());
        jobService.unsaveJob(principal.getId(), jobId);
        return ApiResponse.success(null);
    }
}
