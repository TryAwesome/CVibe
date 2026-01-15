package com.cvibe.biz.growth.dto;

import com.cvibe.biz.growth.entity.LearningPath;
import com.cvibe.biz.growth.entity.LearningPath.DifficultyLevel;
import com.cvibe.biz.growth.entity.LearningPath.PathFocus;
import com.cvibe.biz.growth.entity.LearningPath.PathStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO for LearningPath
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathDto {

    private UUID id;
    private UUID goalId;
    private String title;
    private String description;
    private PathFocus focus;
    private DifficultyLevel difficulty;
    private Integer estimatedHours;
    private LocalDate targetDate;
    private Integer completionPercent;
    private PathStatus status;
    private Integer sortOrder;
    private Instant createdAt;

    // Nested milestones (optional)
    private List<LearningMilestoneDto> milestones;
    private Integer totalMilestones;
    private Integer completedMilestones;

    /**
     * Convert entity to simple DTO
     */
    public static LearningPathDto from(LearningPath path) {
        return from(path, false);
    }

    /**
     * Convert entity to DTO with optional milestones
     */
    public static LearningPathDto from(LearningPath path, boolean includeMilestones) {
        LearningPathDtoBuilder builder = LearningPathDto.builder()
                .id(path.getId())
                .goalId(path.getGoal() != null ? path.getGoal().getId() : null)
                .title(path.getTitle())
                .description(path.getDescription())
                .focus(path.getFocus())
                .difficulty(path.getDifficulty())
                .estimatedHours(path.getEstimatedHours())
                .targetDate(path.getTargetDate())
                .completionPercent(path.getCompletionPercent())
                .status(path.getStatus())
                .sortOrder(path.getSortOrder())
                .createdAt(path.getCreatedAt());

        if (path.getMilestones() != null) {
            builder.totalMilestones(path.getMilestones().size());
            builder.completedMilestones((int) path.getMilestones().stream()
                    .filter(m -> Boolean.TRUE.equals(m.getIsCompleted()))
                    .count());

            if (includeMilestones) {
                builder.milestones(path.getMilestones().stream()
                        .map(LearningMilestoneDto::from)
                        .collect(Collectors.toList()));
            }
        }

        return builder.build();
    }
}
