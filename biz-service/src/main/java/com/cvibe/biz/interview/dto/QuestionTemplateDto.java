package com.cvibe.biz.interview.dto;

import com.cvibe.biz.interview.entity.QuestionTemplate;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class QuestionTemplateDto {
    private UUID id;
    private String questionText;
    private String category;
    private String subcategory;
    private String questionType;
    private String difficultyLevel;
    private String expectedResponseType;
    private String exampleAnswer;
    private Integer orderWeight;
    private Boolean isRequired;
    private Boolean isActive;
    private String language;

    public static QuestionTemplateDto from(QuestionTemplate template) {
        return QuestionTemplateDto.builder()
                .id(template.getId())
                .questionText(template.getQuestionText())
                .category(template.getCategory().name())
                .subcategory(template.getSubcategory())
                .questionType(template.getQuestionType().name())
                .difficultyLevel(template.getDifficultyLevel().name())
                .expectedResponseType(template.getExpectedResponseType())
                .exampleAnswer(template.getExampleAnswer())
                .orderWeight(template.getOrderWeight())
                .isRequired(template.getIsRequired())
                .isActive(template.getIsActive())
                .language(template.getLanguage())
                .build();
    }
}
