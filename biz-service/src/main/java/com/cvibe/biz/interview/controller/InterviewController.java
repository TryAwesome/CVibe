package com.cvibe.biz.interview.controller;

import com.cvibe.common.response.ApiResponse;
import com.cvibe.biz.interview.dto.*;
import com.cvibe.biz.interview.entity.QuestionTemplate;
import com.cvibe.biz.interview.service.InterviewService;
import com.cvibe.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    // ================== User Interview APIs ==================

    /**
     * Start a new interview session
     */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<InterviewStateResponse>> startSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StartSessionRequest request) {
        InterviewStateResponse response = interviewService.startSession(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get current state of a session
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<InterviewStateResponse>> getSessionState(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        InterviewStateResponse response = interviewService.getSessionState(principal.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all sessions for current user
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<InterviewSessionDto>>> getUserSessions(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<InterviewSessionDto> sessions = interviewService.getUserSessions(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * Submit an answer
     */
    @PostMapping("/sessions/{sessionId}/answers")
    public ResponseEntity<ApiResponse<InterviewStateResponse>> submitAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        InterviewStateResponse response = interviewService.submitAnswer(principal.getId(), sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all answers for a session
     */
    @GetMapping("/sessions/{sessionId}/answers")
    public ResponseEntity<ApiResponse<List<InterviewAnswerDto>>> getSessionAnswers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        List<InterviewAnswerDto> answers = interviewService.getSessionAnswers(principal.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(answers));
    }

    /**
     * Pause a session
     */
    @PostMapping("/sessions/{sessionId}/pause")
    public ResponseEntity<ApiResponse<InterviewSessionDto>> pauseSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        InterviewSessionDto session = interviewService.pauseSession(principal.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    /**
     * Resume a paused session
     */
    @PostMapping("/sessions/{sessionId}/resume")
    public ResponseEntity<ApiResponse<InterviewStateResponse>> resumeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        InterviewStateResponse response = interviewService.resumeSession(principal.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Abandon a session
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> abandonSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        interviewService.abandonSession(principal.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ================== Admin APIs ==================

    /**
     * Get all question templates (admin)
     */
    @GetMapping("/admin/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<QuestionTemplateDto>>> getAllQuestionTemplates() {
        List<QuestionTemplateDto> templates = interviewService.getAllQuestionTemplates();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    /**
     * Create a new question template (admin)
     */
    @PostMapping("/admin/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<QuestionTemplateDto>> createQuestionTemplate(
            @Valid @RequestBody QuestionTemplate template) {
        QuestionTemplateDto created = interviewService.createQuestionTemplate(template);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    /**
     * Update a question template (admin)
     */
    @PutMapping("/admin/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<QuestionTemplateDto>> updateQuestionTemplate(
            @PathVariable UUID id,
            @RequestBody QuestionTemplate updates) {
        QuestionTemplateDto updated = interviewService.updateQuestionTemplate(id, updates);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /**
     * Deactivate a question template (admin)
     */
    @DeleteMapping("/admin/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateQuestionTemplate(@PathVariable UUID id) {
        interviewService.deactivateQuestionTemplate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
