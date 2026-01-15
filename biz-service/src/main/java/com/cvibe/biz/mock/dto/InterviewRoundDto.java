package com.cvibe.biz.mock.dto;

import com.cvibe.biz.mock.entity.InterviewRound;
import com.cvibe.biz.mock.entity.InterviewRound.RoundStatus;
import com.cvibe.biz.mock.entity.InterviewRound.RoundType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO for InterviewRound
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRoundDto {

    private UUID id;
    private Integer roundNumber;
    private String roundName;
    private RoundType roundType;
    private RoundStatus status;
    private Integer questionCount;
    private Integer score;
    private String feedback;
    private Integer durationSeconds;
    private Instant startedAt;
    private Instant completedAt;
    
    // Nested (optional)
    private List<MockQuestionDto> questions;

    public static InterviewRoundDto from(InterviewRound round) {
        return from(round, false);
    }

    public static InterviewRoundDto from(InterviewRound round, boolean includeQuestions) {
        InterviewRoundDtoBuilder builder = InterviewRoundDto.builder()
                .id(round.getId())
                .roundNumber(round.getRoundNumber())
                .roundName(round.getRoundName())
                .roundType(round.getRoundType())
                .status(round.getStatus())
                .questionCount(round.getQuestionCount())
                .score(round.getScore())
                .feedback(round.getFeedback())
                .durationSeconds(round.getDurationSeconds())
                .startedAt(round.getStartedAt())
                .completedAt(round.getCompletedAt());

        if (includeQuestions && round.getQuestions() != null) {
            builder.questions(round.getQuestions().stream()
                    .map(MockQuestionDto::from)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
