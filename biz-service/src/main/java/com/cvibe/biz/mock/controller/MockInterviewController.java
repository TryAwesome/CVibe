package com.cvibe.biz.mock.controller;

import com.cvibe.biz.mock.dto.*;
import com.cvibe.biz.mock.service.MockInterviewService;
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
 * Mock Interview Controller
 * 
 * REST API for mock interview practice sessions.
 */
@RestController
@RequestMapping("/v1/mock-interview")
@RequiredArgsConstructor
@Tag(name = "Mock Interview", description = "Mock interview practice and evaluation APIs")
public class MockInterviewController {

    private final MockInterviewService mockInterviewService;

    // ================== Interview Management ==================

    @PostMapping("/start")
    @Operation(summary = "Start mock interview", description = "Start a new mock interview session")
    public ApiResponse<MockInterviewDto> startInterview(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody StartInterviewRequest request) {
        return ApiResponse.success(mockInterviewService.startInterview(userId, request));
    }

    @GetMapping("/{interviewId}")
    @Operation(summary = "Get interview details", description = "Get details of a mock interview")
    public ApiResponse<MockInterviewDto> getInterview(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Interview ID") @PathVariable UUID interviewId) {
        return ApiResponse.success(mockInterviewService.getInterview(userId, interviewId));
    }

    @GetMapping("/history")
    @Operation(summary = "Get interview history", description = "Get user's mock interview history")
    public ApiResponse<Page<MockInterviewDto>> getInterviewHistory(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(mockInterviewService.getInterviewHistory(userId, page, size));
    }

    @PostMapping("/{interviewId}/resume")
    @Operation(summary = "Resume interview", description = "Resume a paused or created interview")
    public ApiResponse<MockInterviewDto> resumeInterview(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Interview ID") @PathVariable UUID interviewId) {
        return ApiResponse.success(mockInterviewService.resumeInterview(userId, interviewId));
    }

    @PostMapping("/{interviewId}/pause")
    @Operation(summary = "Pause interview", description = "Pause an in-progress interview")
    public ApiResponse<MockInterviewDto> pauseInterview(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Interview ID") @PathVariable UUID interviewId) {
        return ApiResponse.success(mockInterviewService.pauseInterview(userId, interviewId));
    }

    @DeleteMapping("/{interviewId}")
    @Operation(summary = "Cancel interview", description = "Cancel a mock interview")
    public ApiResponse<Void> cancelInterview(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Interview ID") @PathVariable UUID interviewId) {
        mockInterviewService.cancelInterview(userId, interviewId);
        return ApiResponse.success(null);
    }

    // ================== Questions ==================

    @GetMapping("/{interviewId}/next-question")
    @Operation(summary = "Get next question", description = "Get the next unanswered question")
    public ApiResponse<MockQuestionDto> getNextQuestion(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Interview ID") @PathVariable UUID interviewId) {
        return ApiResponse.success(mockInterviewService.getNextQuestion(userId, interviewId));
    }

    @GetMapping("/{interviewId}/questions/{questionNumber}")
    @Operation(summary = "Get specific question", description = "Get a specific question by number")
    public ApiResponse<MockQuestionDto> getQuestion(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Interview ID") @PathVariable UUID interviewId,
            @Parameter(description = "Question number") @PathVariable Integer questionNumber) {
        return ApiResponse.success(mockInterviewService.getQuestion(userId, interviewId, questionNumber));
    }

    @GetMapping("/{interviewId}/questions")
    @Operation(summary = "Get all questions", description = "Get all questions for an interview")
    public ApiResponse<List<MockQuestionDto>> getAllQuestions(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Interview ID") @PathVariable UUID interviewId) {
        return ApiResponse.success(mockInterviewService.getAllQuestions(userId, interviewId));
    }

    @PostMapping("/{interviewId}/questions/{questionId}/skip")
    @Operation(summary = "Skip question", description = "Skip a question")
    public ApiResponse<MockQuestionDto> skipQuestion(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Interview ID") @PathVariable UUID interviewId,
            @Parameter(description = "Question ID") @PathVariable UUID questionId) {
        return ApiResponse.success(mockInterviewService.skipQuestion(userId, interviewId, questionId));
    }

    // ================== Answers ==================

    @PostMapping("/{interviewId}/answer")
    @Operation(summary = "Submit answer", description = "Submit an answer to a question")
    public ApiResponse<MockAnswerDto> submitAnswer(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Interview ID") @PathVariable UUID interviewId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        return ApiResponse.success(mockInterviewService.submitAnswer(userId, interviewId, request));
    }

    @GetMapping("/answers/{questionId}")
    @Operation(summary = "Get answer", description = "Get the answer for a question")
    public ApiResponse<MockAnswerDto> getAnswer(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Question ID") @PathVariable UUID questionId) {
        return ApiResponse.success(mockInterviewService.getAnswer(userId, questionId));
    }

    // ================== Feedback ==================

    @PostMapping("/{interviewId}/feedback")
    @Operation(summary = "Generate feedback", description = "Generate overall feedback for a completed interview")
    public ApiResponse<MockInterviewDto> generateFeedback(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Interview ID") @PathVariable UUID interviewId) {
        return ApiResponse.success(mockInterviewService.generateFeedback(userId, interviewId));
    }

    // ================== Dashboard ==================

    @GetMapping("/summary")
    @Operation(summary = "Get mock interview summary", description = "Get summary statistics for mock interviews")
    public ApiResponse<MockInterviewSummary> getSummary(@AuthenticationPrincipal UUID userId) {
        return ApiResponse.success(mockInterviewService.getSummary(userId));
    }
}
