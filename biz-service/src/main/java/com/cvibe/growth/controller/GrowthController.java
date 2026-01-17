package com.cvibe.growth.controller;

import com.cvibe.common.dto.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import com.cvibe.growth.dto.*;
import com.cvibe.growth.service.GrowthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for growth goals and learning paths
 * 
 * API Base Path: /api/v1/growth
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/growth")
@RequiredArgsConstructor
public class GrowthController {

    private final GrowthService growthService;

    // ==================== Goal Endpoints ====================

    /**
     * Create a new growth goal
     * POST /api/v1/growth/goals
     */
    @PostMapping("/goals")
    public ApiResponse<GrowthGoalDto> createGoal(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGoalRequest request) {
        log.info("Creating growth goal for user: {}", principal.getId());
        GrowthGoalDto goal = growthService.createGoal(principal.getId(), request);
        return ApiResponse.success(goal);
    }

    /**
     * Get all goals for current user
     * GET /api/v1/growth/goals
     */
    @GetMapping("/goals")
    public ApiResponse<List<GrowthGoalDto>> getGoals(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Getting growth goals for user: {}", principal.getId());
        List<GrowthGoalDto> goals = growthService.getGoals(principal.getId());
        return ApiResponse.success(goals);
    }

    /**
     * Get a specific goal
     * GET /api/v1/growth/goals/{goalId}
     */
    @GetMapping("/goals/{goalId}")
    public ApiResponse<GrowthGoalDto> getGoal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID goalId) {
        log.info("Getting growth goal {} for user: {}", goalId, principal.getId());
        GrowthGoalDto goal = growthService.getGoal(principal.getId(), goalId);
        return ApiResponse.success(goal);
    }

    /**
     * Update a goal
     * PUT /api/v1/growth/goals/{goalId}
     */
    @PutMapping("/goals/{goalId}")
    public ApiResponse<GrowthGoalDto> updateGoal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID goalId,
            @Valid @RequestBody UpdateGoalRequest request) {
        log.info("Updating growth goal {} for user: {}", goalId, principal.getId());
        GrowthGoalDto goal = growthService.updateGoal(principal.getId(), goalId, request);
        return ApiResponse.success(goal);
    }

    /**
     * Delete a goal
     * DELETE /api/v1/growth/goals/{goalId}
     */
    @DeleteMapping("/goals/{goalId}")
    public ApiResponse<Void> deleteGoal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID goalId) {
        log.info("Deleting growth goal {} for user: {}", goalId, principal.getId());
        growthService.deleteGoal(principal.getId(), goalId);
        return ApiResponse.success(null);
    }

    // ==================== Gap Analysis Endpoints ====================

    /**
     * Analyze skill gaps for a goal
     * POST /api/v1/growth/goals/{goalId}/analyze
     */
    @PostMapping("/goals/{goalId}/analyze")
    public ApiResponse<GapAnalysisDto> analyzeGaps(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID goalId) {
        log.info("Analyzing gaps for goal {} user: {}", goalId, principal.getId());
        GapAnalysisDto analysis = growthService.analyzeGaps(principal.getId(), goalId);
        return ApiResponse.success(analysis);
    }

    /**
     * Get cached gap analysis for a goal
     * GET /api/v1/growth/goals/{goalId}/gaps
     */
    @GetMapping("/goals/{goalId}/gaps")
    public ApiResponse<GapAnalysisDto> getGaps(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID goalId) {
        log.info("Getting gaps for goal {} user: {}", goalId, principal.getId());
        GapAnalysisDto analysis = growthService.getGaps(principal.getId(), goalId);
        return ApiResponse.success(analysis);
    }

    // ==================== Learning Path Endpoints ====================

    /**
     * Generate learning paths for a goal
     * POST /api/v1/growth/goals/{goalId}/generate-paths
     */
    @PostMapping("/goals/{goalId}/generate-paths")
    public ApiResponse<List<LearningPathDto>> generateLearningPaths(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID goalId) {
        log.info("Generating learning paths for goal {} user: {}", goalId, principal.getId());
        List<LearningPathDto> paths = growthService.generateLearningPaths(principal.getId(), goalId);
        return ApiResponse.success(paths);
    }

    /**
     * Get learning paths for a goal
     * GET /api/v1/growth/goals/{goalId}/paths
     */
    @GetMapping("/goals/{goalId}/paths")
    public ApiResponse<List<LearningPathDto>> getLearningPaths(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID goalId) {
        log.info("Getting learning paths for goal {} user: {}", goalId, principal.getId());
        List<LearningPathDto> paths = growthService.getLearningPaths(principal.getId(), goalId);
        return ApiResponse.success(paths);
    }

    // ==================== Milestone Endpoints ====================

    /**
     * Mark a milestone as completed
     * POST /api/v1/growth/milestones/{milestoneId}/complete
     */
    @PostMapping("/milestones/{milestoneId}/complete")
    public ApiResponse<LearningPhaseDto> completeMilestone(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID milestoneId) {
        log.info("Completing milestone {} for user: {}", milestoneId, principal.getId());
        LearningPhaseDto milestone = growthService.completeMilestone(principal.getId(), milestoneId);
        return ApiResponse.success(milestone);
    }

    /**
     * Mark a milestone as not completed
     * POST /api/v1/growth/milestones/{milestoneId}/uncomplete
     */
    @PostMapping("/milestones/{milestoneId}/uncomplete")
    public ApiResponse<LearningPhaseDto> uncompleteMilestone(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID milestoneId) {
        log.info("Uncompleting milestone {} for user: {}", milestoneId, principal.getId());
        LearningPhaseDto milestone = growthService.uncompleteMilestone(principal.getId(), milestoneId);
        return ApiResponse.success(milestone);
    }

    // ==================== Summary Endpoint ====================

    /**
     * Get growth summary for current user
     * GET /api/v1/growth/summary
     */
    @GetMapping("/summary")
    public ApiResponse<GrowthSummaryDto> getSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Getting growth summary for user: {}", principal.getId());
        GrowthSummaryDto summary = growthService.getSummary(principal.getId());
        return ApiResponse.success(summary);
    }
}
