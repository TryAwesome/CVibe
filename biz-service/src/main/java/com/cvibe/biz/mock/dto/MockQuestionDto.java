package com.cvibe.biz.mock.dto;

import com.cvibe.biz.mock.entity.MockQuestion;
import com.cvibe.biz.mock.entity.MockQuestion.QuestionCategory;
import com.cvibe.biz.mock.entity.MockQuestion.QuestionDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for MockQuestion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockQuestionDto {

    private UUID id;
    private Integer questionNumber;
    private QuestionCategory category;
    private QuestionDifficulty difficulty;
    private String questionText;
    private String followUpQuestion;
    private String relatedSkill;
    private Integer timeLimitSeconds;
    private Boolean isAnswered;
    private Boolean isSkipped;
    
    // Only shown after answer or for review
    private String expectedPoints;
    private String sampleAnswer;
    
    // Answer info (if answered)
    private MockAnswerDto answer;

    public static MockQuestionDto from(MockQuestion question) {
        return from(question, false);
    }

    public static MockQuestionDto from(MockQuestion question, boolean includeExpected) {
        MockQuestionDtoBuilder builder = MockQuestionDto.builder()
                .id(question.getId())
                .questionNumber(question.getQuestionNumber())
                .category(question.getCategory())
                .difficulty(question.getDifficulty())
                .questionText(question.getQuestionText())
                .followUpQuestion(question.getFollowUpQuestion())
                .relatedSkill(question.getRelatedSkill())
                .timeLimitSeconds(question.getTimeLimitSeconds())
                .isAnswered(question.getIsAnswered())
                .isSkipped(question.getIsSkipped());

        if (includeExpected || question.getIsAnswered()) {
            builder.expectedPoints(question.getExpectedPoints());
            builder.sampleAnswer(question.getSampleAnswer());
        }

        if (question.getAnswer() != null) {
            builder.answer(MockAnswerDto.from(question.getAnswer()));
        }

        return builder.build();
    }

    /**
     * Create question-only view (for during interview, without expected answers)
     */
    public static MockQuestionDto forInterview(MockQuestion question) {
        return MockQuestionDto.builder()
                .id(question.getId())
                .questionNumber(question.getQuestionNumber())
                .category(question.getCategory())
                .difficulty(question.getDifficulty())
                .questionText(question.getQuestionText())
                .followUpQuestion(question.getFollowUpQuestion())
                .relatedSkill(question.getRelatedSkill())
                .timeLimitSeconds(question.getTimeLimitSeconds())
                .isAnswered(question.getIsAnswered())
                .isSkipped(question.getIsSkipped())
                .build();
    }
}
