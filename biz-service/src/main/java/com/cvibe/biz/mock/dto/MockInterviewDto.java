package com.cvibe.biz.mock.dto;

import com.cvibe.biz.mock.entity.MockInterview;
import com.cvibe.biz.mock.entity.MockInterview.DifficultyLevel;
import com.cvibe.biz.mock.entity.MockInterview.InterviewStatus;
import com.cvibe.biz.mock.entity.MockInterview.InterviewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO for MockInterview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewDto {

    private UUID id;
    private String targetPosition;
    private String targetCompany;
    private InterviewType interviewType;
    private DifficultyLevel difficulty;
    private InterviewStatus status;
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private Integer progressPercent;
    
    // Scores
    private Integer overallScore;
    private Integer technicalScore;
    private Integer communicationScore;
    private Integer problemSolvingScore;
    
    // Feedback
    private String feedbackSummary;
    private String strengths;
    private String improvements;
    
    // Timing
    private Instant startedAt;
    private Instant completedAt;
    private Integer durationSeconds;
    private Instant createdAt;
    
    // Nested (optional)
    private List<InterviewRoundDto> rounds;
    private List<MockQuestionDto> questions;

    public static MockInterviewDto from(MockInterview interview) {
        return from(interview, false);
    }

    public static MockInterviewDto from(MockInterview interview, boolean includeDetails) {
        MockInterviewDtoBuilder builder = MockInterviewDto.builder()
                .id(interview.getId())
                .targetPosition(interview.getTargetPosition())
                .targetCompany(interview.getTargetCompany())
                .interviewType(interview.getInterviewType())
                .difficulty(interview.getDifficulty())
                .status(interview.getStatus())
                .totalQuestions(interview.getTotalQuestions())
                .answeredQuestions(interview.getAnsweredQuestions())
                .progressPercent(interview.getProgressPercent())
                .overallScore(interview.getOverallScore())
                .technicalScore(interview.getTechnicalScore())
                .communicationScore(interview.getCommunicationScore())
                .problemSolvingScore(interview.getProblemSolvingScore())
                .feedbackSummary(interview.getFeedbackSummary())
                .strengths(interview.getStrengths())
                .improvements(interview.getImprovements())
                .startedAt(interview.getStartedAt())
                .completedAt(interview.getCompletedAt())
                .durationSeconds(interview.getDurationSeconds())
                .createdAt(interview.getCreatedAt());

        if (includeDetails) {
            if (interview.getRounds() != null) {
                builder.rounds(interview.getRounds().stream()
                        .map(InterviewRoundDto::from)
                        .collect(Collectors.toList()));
            }
            if (interview.getQuestions() != null) {
                builder.questions(interview.getQuestions().stream()
                        .map(MockQuestionDto::from)
                        .collect(Collectors.toList()));
            }
        }

        return builder.build();
    }
}
