package com.cvibe.biz.mock.dto;

import com.cvibe.biz.mock.entity.MockAnswer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for MockAnswer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockAnswerDto {

    private UUID id;
    private UUID questionId;
    private String answerText;
    private String codeAnswer;
    private String programmingLanguage;
    private Integer timeTakenSeconds;
    private Instant submittedAt;
    
    // Evaluation
    private Boolean isEvaluated;
    private Integer score;
    private Integer accuracyScore;
    private Integer completenessScore;
    private Integer clarityScore;
    private Integer relevanceScore;
    private String scoreGrade;
    
    // Feedback
    private String feedback;
    private String strengths;
    private String improvements;
    private String coveredPoints;
    private String missedPoints;
    private String suggestedAnswer;

    public static MockAnswerDto from(MockAnswer answer) {
        return MockAnswerDto.builder()
                .id(answer.getId())
                .questionId(answer.getQuestion().getId())
                .answerText(answer.getAnswerText())
                .codeAnswer(answer.getCodeAnswer())
                .programmingLanguage(answer.getProgrammingLanguage())
                .timeTakenSeconds(answer.getTimeTakenSeconds())
                .submittedAt(answer.getSubmittedAt())
                .isEvaluated(answer.getIsEvaluated())
                .score(answer.getScore())
                .accuracyScore(answer.getAccuracyScore())
                .completenessScore(answer.getCompletenessScore())
                .clarityScore(answer.getClarityScore())
                .relevanceScore(answer.getRelevanceScore())
                .scoreGrade(answer.getScoreGrade())
                .feedback(answer.getFeedback())
                .strengths(answer.getStrengths())
                .improvements(answer.getImprovements())
                .coveredPoints(answer.getCoveredPoints())
                .missedPoints(answer.getMissedPoints())
                .suggestedAnswer(answer.getSuggestedAnswer())
                .build();
    }

    /**
     * Create summary view (without full feedback)
     */
    public static MockAnswerDto summary(MockAnswer answer) {
        return MockAnswerDto.builder()
                .id(answer.getId())
                .questionId(answer.getQuestion().getId())
                .timeTakenSeconds(answer.getTimeTakenSeconds())
                .submittedAt(answer.getSubmittedAt())
                .isEvaluated(answer.getIsEvaluated())
                .score(answer.getScore())
                .scoreGrade(answer.getScoreGrade())
                .build();
    }
}
