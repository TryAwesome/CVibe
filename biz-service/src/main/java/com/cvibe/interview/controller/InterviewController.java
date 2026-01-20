package com.cvibe.interview.controller;

import com.cvibe.common.dto.ApiResponse;
import com.cvibe.common.security.UserPrincipal;
import com.cvibe.interview.dto.*;
import com.cvibe.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for interview sessions
 * 
 * API Base Path: /api/v1/interviews
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    /**
     * Create a new interview session
     * POST /api/v1/interviews/sessions
     */
    @PostMapping("/sessions")
    public ApiResponse<SessionStateResponse> createSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateSessionRequest request) {
        log.info("Creating interview session for user: {}", principal.getId());
        SessionStateResponse response = interviewService.createSession(principal.getId(), request);
        return ApiResponse.success(response);
    }

    /**
     * Get all sessions for current user
     * GET /api/v1/interviews/sessions
     */
    @GetMapping("/sessions")
    public ApiResponse<List<InterviewSessionDto>> getSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Getting interview sessions for user: {}", principal.getId());
        
        if (page == 0 && size >= 100) {
            // Return all sessions without pagination
            List<InterviewSessionDto> sessions = interviewService.getSessions(principal.getId());
            return ApiResponse.success(sessions);
        }
        
        Page<InterviewSessionDto> sessionPage = interviewService.getSessions(principal.getId(), page, size);
        return ApiResponse.success(sessionPage.getContent());
    }

    /**
     * Get session by ID
     * GET /api/v1/interviews/sessions/{sessionId}
     */
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<SessionStateResponse> getSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        log.info("Getting interview session {} for user: {}", sessionId, principal.getId());
        SessionStateResponse response = interviewService.getSession(principal.getId(), sessionId);
        return ApiResponse.success(response);
    }

    /**
     * Submit an answer to a question
     * POST /api/v1/interviews/sessions/{sessionId}/answers
     */
    @PostMapping("/sessions/{sessionId}/answers")
    public ApiResponse<AnswerSubmitResponse> submitAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        log.info("Submitting answer for session {} question {}", sessionId, request.getQuestionId());
        AnswerSubmitResponse response = interviewService.submitAnswer(principal.getId(), sessionId, request);
        return ApiResponse.success(response);
    }

    /**
     * Get all answers for a session
     * GET /api/v1/interviews/sessions/{sessionId}/answers
     */
    @GetMapping("/sessions/{sessionId}/answers")
    public ApiResponse<List<InterviewQuestionDto>> getSessionAnswers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        log.info("Getting answers for session {}", sessionId);
        List<InterviewQuestionDto> answers = interviewService.getSessionAnswers(principal.getId(), sessionId);
        return ApiResponse.success(answers);
    }

    /**
     * Pause a session
     * POST /api/v1/interviews/sessions/{sessionId}/pause
     */
    @PostMapping("/sessions/{sessionId}/pause")
    public ApiResponse<InterviewSessionDto> pauseSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        log.info("Pausing session {}", sessionId);
        InterviewSessionDto session = interviewService.pauseSession(principal.getId(), sessionId);
        return ApiResponse.success(session);
    }

    /**
     * Resume a paused session
     * POST /api/v1/interviews/sessions/{sessionId}/resume
     */
    @PostMapping("/sessions/{sessionId}/resume")
    public ApiResponse<SessionStateResponse> resumeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        log.info("Resuming session {}", sessionId);
        SessionStateResponse response = interviewService.resumeSession(principal.getId(), sessionId);
        return ApiResponse.success(response);
    }

    /**
     * Delete a session
     * DELETE /api/v1/interviews/sessions/{sessionId}
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        log.info("Deleting session {}", sessionId);
        interviewService.deleteSession(principal.getId(), sessionId);
        return ApiResponse.success(null);
    }

    // ==================== Profile Interview (AI-powered) ====================

    /**
     * Start a profile collection interview session
     * POST /api/v1/interviews/profile/start
     */
    @PostMapping("/profile/start")
    public ApiResponse<ProfileInterviewStartResponse> startProfileInterview(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) StartProfileInterviewRequest request) {
        log.info("Starting profile interview for user: {}", principal.getId());
        if (request == null) {
            request = new StartProfileInterviewRequest();
        }
        ProfileInterviewStartResponse response = interviewService.startProfileInterview(principal.getId(), request);
        return ApiResponse.success(response);
    }

    /**
     * Send a message in profile interview session
     * POST /api/v1/interviews/profile/{sessionId}/message
     */
    @PostMapping("/profile/{sessionId}/message")
    public ApiResponse<ProfileInterviewMessageResponse> sendProfileInterviewMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody ProfileInterviewMessageRequest request) {
        log.info("Sending profile interview message for session: {}", sessionId);
        ProfileInterviewMessageResponse response = interviewService.sendProfileInterviewMessage(
                principal.getId(), sessionId, request);
        return ApiResponse.success(response);
    }

    /**
     * Get profile interview state
     * GET /api/v1/interviews/profile/{sessionId}/state
     */
    @GetMapping("/profile/{sessionId}/state")
    public ApiResponse<ProfileInterviewStateResponse> getProfileInterviewState(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        log.info("Getting profile interview state for session: {}", sessionId);
        ProfileInterviewStateResponse response = interviewService.getProfileInterviewState(
                principal.getId(), sessionId);
        return ApiResponse.success(response);
    }

    /**
     * Finish profile interview and sync to profile
     * POST /api/v1/interviews/profile/{sessionId}/finish
     */
    @PostMapping("/profile/{sessionId}/finish")
    public ApiResponse<ProfileInterviewFinishResponse> finishProfileInterview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        log.info("Finishing profile interview for session: {}", sessionId);
        ProfileInterviewFinishResponse response = interviewService.finishProfileInterview(
                principal.getId(), sessionId);
        return ApiResponse.success(response);
    }
}
