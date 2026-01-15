package com.cvibe.biz.growth.controller;

import com.cvibe.biz.growth.dto.*;
import com.cvibe.biz.growth.service.GrowthService;
import com.cvibe.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Growth Controller
 * 
 * REST API for career growth tracking, gap analysis, and learning paths.
 */
@RestController
@RequestMapping("/api/v1/growth")
@RequiredArgsConstructor
@Tag(name = "Growth", description = "Career growth tracking and gap analysis APIs")
public class GrowthController {

    private final GrowthService growthService;

    // ================== Goals ==================

    @PostMapping("/goals")
    @Operation(summary = "Create growth goal", description = "Create a new career growth goal with target role and requirements")
    public ApiResponse<GrowthGoalDto> createGoal(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateGoalRequest request) {
        return ApiResponse.success(growthService.createGoal(userId, request));
    }

    @GetMapping("/goals")
    @Operation(summary = "Get user goals", description = "Get all growth goals for the current user")
    public ApiResponse<Page<GrowthGoalDto>> getUserGoals(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(growthService.getUserGoals(userId, page, size));
    }

    @GetMapping("/goals/{goalId}")
    @Operation(summary = "Get goal details", description = "Get detailed information about a specific goal including gaps and paths")
    public ApiResponse<GrowthGoalDto> getGoal(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Goal ID") @PathVariable UUID goalId) {
        return ApiResponse.success(growthService.getGoal(userId, goalId));
    }

    @PutMapping("/goals/{goalId}")
    @Operation(summary = "Update goal", description = "Update an existing growth goal")
    public ApiResponse<GrowthGoalDto> updateGoal(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Goal ID") @PathVariable UUID goalId,
            @Valid @RequestBody CreateGoalRequest request) {
        return ApiResponse.success(growthService.updateGoal(userId, goalId, request));
    }

    @PostMapping("/goals/{goalId}/set-primary")
    @Operation(summary = "Set as primary goal", description = "Set a goal as the primary active goal")
    public ApiResponse<GrowthGoalDto> setPrimaryGoal(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Goal ID") @PathVariable UUID goalId) {
        return ApiResponse.success(growthService.setAsPrimaryGoal(userId, goalId));
    }

    @PostMapping("/goals/{goalId}/achieve")
    @Operation(summary = "Mark goal achieved", description = "Mark a goal as achieved")
    public ApiResponse<GrowthGoalDto> achieveGoal(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Goal ID") @PathVariable UUID goalId) {
        return ApiResponse.success(growthService.achieveGoal(userId, goalId));
    }

    @DeleteMapping("/goals/{goalId}")
    @Operation(summary = "Delete goal", description = "Delete a growth goal")
    public ApiResponse<Void> deleteGoal(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Goal ID") @PathVariable UUID goalId) {
        growthService.deleteGoal(userId, goalId);
        return ApiResponse.success(null);
    }

    // ================== Gap Analysis ==================

    @PostMapping("/goals/{goalId}/analyze")
    @Operation(summary = "Analyze gaps", description = "Perform gap analysis for a goal by comparing profile against requirements")
    public ApiResponse<GrowthGoalDto> analyzeGaps(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Goal ID") @PathVariable UUID goalId) {
        return ApiResponse.success(growthService.analyzeGaps(userId, goalId));
    }

    @GetMapping("/goals/{goalId}/gaps")
    @Operation(summary = "Get skill gaps", description = "Get all identified skill gaps for a goal")
    public ApiResponse<List<SkillGapDto>> getSkillGaps(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Goal ID") @PathVariable UUID goalId) {
        return ApiResponse.success(growthService.getSkillGaps(userId, goalId));
    }

    @PatchMapping("/gaps/{gapId}/progress")
    @Operation(summary = "Update gap progress", description = "Update the progress level for a skill gap")
    public ApiResponse<SkillGapDto> updateGapProgress(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Gap ID") @PathVariable UUID gapId,
            @Parameter(description = "New skill level (0-100)") @RequestParam int level) {
        return ApiResponse.success(growthService.updateGapProgress(userId, gapId, level));
    }

    // ================== Learning Paths ==================

    @PostMapping("/goals/{goalId}/generate-paths")
    @Operation(summary = "Generate learning paths", description = "Generate learning paths based on identified gaps")
    public ApiResponse<List<LearningPathDto>> generateLearningPaths(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Goal ID") @PathVariable UUID goalId) {
        return ApiResponse.success(growthService.generateLearningPaths(userId, goalId));
    }

    @GetMapping("/goals/{goalId}/paths")
    @Operation(summary = "Get learning paths", description = "Get all learning paths for a goal")
    public ApiResponse<List<LearningPathDto>> getLearningPaths(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Goal ID") @PathVariable UUID goalId) {
        return ApiResponse.success(growthService.getLearningPaths(userId, goalId));
    }

    @PostMapping("/milestones/{milestoneId}/complete")
    @Operation(summary = "Complete milestone", description = "Mark a learning milestone as completed")
    public ApiResponse<LearningMilestoneDto> completeMilestone(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Milestone ID") @PathVariable UUID milestoneId) {
        return ApiResponse.success(growthService.completeMilestone(userId, milestoneId));
    }

    @PostMapping("/milestones/{milestoneId}/uncomplete")
    @Operation(summary = "Uncomplete milestone", description = "Mark a completed milestone as incomplete")
    public ApiResponse<LearningMilestoneDto> uncompleteMilestone(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Milestone ID") @PathVariable UUID milestoneId) {
        return ApiResponse.success(growthService.uncompleteMilestone(userId, milestoneId));
    }

    // ================== Dashboard ==================

    @GetMapping("/summary")
    @Operation(summary = "Get growth summary", description = "Get growth summary for dashboard including goals, gaps, and paths overview")
    public ApiResponse<GrowthSummary> getSummary(@AuthenticationPrincipal UUID userId) {
        return ApiResponse.success(growthService.getSummary(userId));
    }
}
