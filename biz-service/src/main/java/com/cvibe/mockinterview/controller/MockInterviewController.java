package com.cvibe.mockinterview.controller;

import com.cvibe.common.dto.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import com.cvibe.mockinterview.dto.*;
import com.cvibe.mockinterview.service.MockInterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for mock interview sessions
 * 
 * API Base Path: /api/v1/mock-interview
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mock-interview")
@RequiredArgsConstructor
public class MockInterviewController {

    private final MockInterviewService mockInterviewService;

    /**
     * Start a new mock interview session
     * POST /api/v1/mock-interview/start
     */
    @PostMapping("/start")
    public ApiResponse<MockInterviewStateResponse> startSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMockInterviewRequest request) {
        log.info("Starting mock interview for user: {}", principal.getId());
        MockInterviewStateResponse response = mockInterviewService.createSession(principal.getId(), request);
        return ApiResponse.success(response);
    }

    /**
     * Get session by ID
     * GET /api/v1/mock-interview/{interviewId}
     */
    @GetMapping("/{interviewId}")
    public ApiResponse<MockInterviewStateResponse> getSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID interviewId) {
        log.info("Getting mock interview {} for user: {}", interviewId, principal.getId());
        MockInterviewStateResponse response = mockInterviewService.getSession(principal.getId(), interviewId);
        return ApiResponse.success(response);
    }

    /**
     * Get interview history for current user
     * GET /api/v1/mock-interview/history
     */
    @GetMapping("/history")
    public ApiResponse<Page<MockInterviewSessionDto>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Getting mock interview history for user: {}", principal.getId());
        Page<MockInterviewSessionDto> history = mockInterviewService.getHistory(principal.getId(), page, size);
        return ApiResponse.success(history);
    }

    /**
     * Get next question for the session
     * GET /api/v1/mock-interview/{interviewId}/next-question
     */
    @GetMapping("/{interviewId}/next-question")
    public ApiResponse<MockInterviewQuestionDto> getNextQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID interviewId) {
        log.info("Getting next question for session {}", interviewId);
        MockInterviewQuestionDto question = mockInterviewService.getNextQuestion(principal.getId(), interviewId);
        return ApiResponse.success(question);
    }

    /**
     * Submit an answer to a question
     * POST /api/v1/mock-interview/{interviewId}/answer
     */
    @PostMapping("/{interviewId}/answer")
    public ApiResponse<MockAnswerSubmitResponse> submitAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID interviewId,
            @Valid @RequestBody SubmitMockAnswerRequest request) {
        log.info("Submitting answer for session {} question {}", interviewId, request.getQuestionId());
        MockAnswerSubmitResponse response = mockInterviewService.submitAnswer(principal.getId(), interviewId, request);
        return ApiResponse.success(response);
    }

    /**
     * Pause a session
     * POST /api/v1/mock-interview/{interviewId}/pause
     */
    @PostMapping("/{interviewId}/pause")
    public ApiResponse<MockInterviewSessionDto> pauseSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID interviewId) {
        log.info("Pausing session {}", interviewId);
        MockInterviewSessionDto session = mockInterviewService.pauseSession(principal.getId(), interviewId);
        return ApiResponse.success(session);
    }

    /**
     * Resume a paused session
     * POST /api/v1/mock-interview/{interviewId}/resume
     */
    @PostMapping("/{interviewId}/resume")
    public ApiResponse<MockInterviewStateResponse> resumeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID interviewId) {
        log.info("Resuming session {}", interviewId);
        MockInterviewStateResponse response = mockInterviewService.resumeSession(principal.getId(), interviewId);
        return ApiResponse.success(response);
    }

    /**
     * Complete a session and get final feedback
     * POST /api/v1/mock-interview/{interviewId}/complete
     */
    @PostMapping("/{interviewId}/complete")
    public ApiResponse<MockInterviewStateResponse> completeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID interviewId) {
        log.info("Completing session {}", interviewId);
        MockInterviewStateResponse response = mockInterviewService.completeSession(principal.getId(), interviewId);
        return ApiResponse.success(response);
    }

    /**
     * Get summary statistics for the current user
     * GET /api/v1/mock-interview/summary
     */
    @GetMapping("/summary")
    public ApiResponse<MockInterviewSummaryDto> getSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Getting mock interview summary for user: {}", principal.getId());
        MockInterviewSummaryDto summary = mockInterviewService.getSummary(principal.getId());
        return ApiResponse.success(summary);
    }
}
