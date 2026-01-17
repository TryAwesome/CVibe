package com.cvibe.growth.service;

import com.cvibe.auth.entity.User;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import com.cvibe.growth.dto.*;
import com.cvibe.growth.entity.*;
import com.cvibe.growth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing growth goals and learning paths
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthService {

    private final GrowthGoalRepository goalRepository;
    private final LearningPathRepository pathRepository;
    private final LearningMilestoneRepository milestoneRepository;
    private final UserRepository userRepository;

    // ==================== Goal CRUD ====================

    /**
     * Create a new growth goal
     */
    @Transactional
    public GrowthGoalDto createGoal(UUID userId, CreateGoalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDate targetDate = null;
        if (request.getTargetDate() != null && !request.getTargetDate().isEmpty()) {
            try {
                targetDate = LocalDate.parse(request.getTargetDate());
            } catch (DateTimeParseException e) {
                throw new BusinessException(ErrorCode.INVALID_DATE_FORMAT);
            }
        }

        GrowthGoal goal = GrowthGoal.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .targetRole(request.getTargetRole())
                .targetDate(targetDate)
                .status(GoalStatus.NOT_STARTED)
                .progress(0)
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .build();

        goal = goalRepository.save(goal);
        log.info("Created growth goal {} for user {}", goal.getId(), userId);

        return GrowthGoalDto.fromEntity(goal);
    }

    /**
     * Get all goals for a user
     */
    @Transactional(readOnly = true)
    public List<GrowthGoalDto> getGoals(UUID userId) {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(GrowthGoalDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific goal by ID
     */
    @Transactional(readOnly = true)
    public GrowthGoalDto getGoal(UUID userId, UUID goalId) {
        GrowthGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOAL_NOT_FOUND));
        return GrowthGoalDto.fromEntity(goal);
    }

    /**
     * Update a goal
     */
    @Transactional
    public GrowthGoalDto updateGoal(UUID userId, UUID goalId, UpdateGoalRequest request) {
        GrowthGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOAL_NOT_FOUND));

        if (request.getTitle() != null) {
            goal.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            goal.setDescription(request.getDescription());
        }
        if (request.getTargetRole() != null) {
            goal.setTargetRole(request.getTargetRole());
        }
        if (request.getTargetDate() != null) {
            try {
                goal.setTargetDate(LocalDate.parse(request.getTargetDate()));
            } catch (DateTimeParseException e) {
                throw new BusinessException(ErrorCode.INVALID_DATE_FORMAT);
            }
        }
        if (request.getStatus() != null) {
            try {
                goal.setStatus(GoalStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_GOAL_STATUS);
            }
        }
        if (request.getProgress() != null) {
            goal.setProgress(Math.min(100, Math.max(0, request.getProgress())));
        }
        if (request.getIsPrimary() != null) {
            goal.setIsPrimary(request.getIsPrimary());
        }

        goal = goalRepository.save(goal);
        log.info("Updated growth goal {}", goalId);

        return GrowthGoalDto.fromEntity(goal);
    }

    /**
     * Delete a goal
     */
    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {
        GrowthGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOAL_NOT_FOUND));

        // Delete associated learning paths and milestones
        List<LearningPath> paths = pathRepository.findByGoalId(goalId);
        for (LearningPath path : paths) {
            milestoneRepository.deleteAll(path.getMilestones());
        }
        pathRepository.deleteAll(paths);
        goalRepository.delete(goal);

        log.info("Deleted growth goal {}", goalId);
    }

    // ==================== Gap Analysis (Mock) ====================

    /**
     * Analyze skill gaps for a goal (mock implementation)
     */
    @Transactional(readOnly = true)
    public GapAnalysisDto analyzeGaps(UUID userId, UUID goalId) {
        GrowthGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOAL_NOT_FOUND));

        // Mock gap analysis
        List<SkillGapDto> skillGaps = Arrays.asList(
                SkillGapDto.builder()
                        .skillName("System Design")
                        .category("Technical")
                        .currentLevel(3)
                        .requiredLevel(5)
                        .gap(2)
                        .priority("HIGH")
                        .recommendation("Practice designing distributed systems and study common patterns")
                        .build(),
                SkillGapDto.builder()
                        .skillName("Leadership")
                        .category("Soft Skills")
                        .currentLevel(2)
                        .requiredLevel(4)
                        .gap(2)
                        .priority("MEDIUM")
                        .recommendation("Take on mentoring responsibilities and lead small projects")
                        .build(),
                SkillGapDto.builder()
                        .skillName("Cloud Architecture")
                        .category("Technical")
                        .currentLevel(3)
                        .requiredLevel(4)
                        .gap(1)
                        .priority("MEDIUM")
                        .recommendation("Get certified in AWS or GCP and build cloud-native applications")
                        .build()
        );

        return GapAnalysisDto.builder()
                .targetRole(goal.getTargetRole() != null ? goal.getTargetRole() : "Senior Software Engineer")
                .currentLevel("Mid-Level")
                .targetLevel("Senior")
                .overallReadiness(65)
                .skillGaps(skillGaps)
                .recommendations(Arrays.asList(
                        "Focus on system design practice for upcoming interviews",
                        "Consider leading a cross-team project to build leadership experience",
                        "Pursue cloud certification within the next 3 months"
                ))
                .analyzedAt(Instant.now().toString())
                .build();
    }

    /**
     * Get cached gap analysis for a goal (returns same mock data)
     */
    @Transactional(readOnly = true)
    public GapAnalysisDto getGaps(UUID userId, UUID goalId) {
        return analyzeGaps(userId, goalId);
    }

    // ==================== Learning Paths (Mock) ====================

    /**
     * Generate learning paths for a goal (mock implementation)
     */
    @Transactional
    public List<LearningPathDto> generateLearningPaths(UUID userId, UUID goalId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        GrowthGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOAL_NOT_FOUND));

        // Create mock learning path
        LearningPath path = LearningPath.builder()
                .user(user)
                .goal(goal)
                .title("Path to " + (goal.getTargetRole() != null ? goal.getTargetRole() : "Senior Engineer"))
                .description("A comprehensive learning path designed to help you achieve your career goal.")
                .duration("3-6 months")
                .build();

        path = pathRepository.save(path);

        // Create mock milestones
        List<LearningMilestone> milestones = Arrays.asList(
                LearningMilestone.builder()
                        .path(path)
                        .title("Foundation - Core Concepts Review")
                        .description("Review fundamental concepts and identify knowledge gaps")
                        .type("LEARN")
                        .orderIndex(0)
                        .isCompleted(false)
                        .resourcesJson("[\"Book: Designing Data-Intensive Applications\", \"Course: System Design Primer\"]")
                        .build(),
                LearningMilestone.builder()
                        .path(path)
                        .title("Practice - System Design Exercises")
                        .description("Practice designing real-world systems")
                        .type("PRACTICE")
                        .orderIndex(1)
                        .isCompleted(false)
                        .resourcesJson("[\"LeetCode System Design\", \"Pramp mock interviews\"]")
                        .build(),
                LearningMilestone.builder()
                        .path(path)
                        .title("Project - Build a Distributed System")
                        .description("Apply your knowledge by building a distributed application")
                        .type("PROJECT")
                        .orderIndex(2)
                        .isCompleted(false)
                        .resourcesJson("[\"GitHub: Sample distributed system projects\"]")
                        .build()
        );

        milestoneRepository.saveAll(milestones);

        log.info("Generated learning path {} for goal {}", path.getId(), goalId);

        return getLearningPaths(userId, goalId);
    }

    /**
     * Get learning paths for a goal
     */
    @Transactional(readOnly = true)
    public List<LearningPathDto> getLearningPaths(UUID userId, UUID goalId) {
        // Verify goal ownership
        goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOAL_NOT_FOUND));

        List<LearningPath> paths = pathRepository.findByGoalId(goalId);
        
        return paths.stream()
                .map(path -> {
                    List<LearningMilestone> milestones = milestoneRepository.findByPathIdOrderByOrderIndexAsc(path.getId());
                    long completed = milestones.stream().filter(LearningMilestone::getIsCompleted).count();
                    int total = milestones.size();
                    int progress = total > 0 ? (int) ((completed * 100) / total) : 0;

                    List<LearningPhaseDto> phases = milestones.stream()
                            .map(m -> LearningPhaseDto.builder()
                                    .id(m.getId().toString())
                                    .title(m.getTitle())
                                    .description(m.getDescription())
                                    .type(m.getType())
                                    .isCompleted(m.getIsCompleted())
                                    .orderIndex(m.getOrderIndex())
                                    .completedAt(m.getCompletedAt() != null ? m.getCompletedAt().toString() : null)
                                    .build())
                            .collect(Collectors.toList());

                    return LearningPathDto.builder()
                            .id(path.getId().toString())
                            .goalId(goalId.toString())
                            .title(path.getTitle())
                            .description(path.getDescription())
                            .duration(path.getDuration())
                            .phases(phases)
                            .totalMilestones(total)
                            .completedMilestones((int) completed)
                            .progressPercentage(progress)
                            .createdAt(path.getCreatedAt() != null ? path.getCreatedAt().toString() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ==================== Milestone Management ====================

    /**
     * Mark a milestone as completed
     */
    @Transactional
    public LearningPhaseDto completeMilestone(UUID userId, UUID milestoneId) {
        LearningMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MILESTONE_NOT_FOUND));

        // Verify ownership through path -> user
        if (!milestone.getPath().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        milestone.setIsCompleted(true);
        milestone.setCompletedAt(Instant.now());
        milestone = milestoneRepository.save(milestone);

        log.info("Milestone {} marked as completed", milestoneId);

        return LearningPhaseDto.builder()
                .id(milestone.getId().toString())
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .type(milestone.getType())
                .isCompleted(milestone.getIsCompleted())
                .orderIndex(milestone.getOrderIndex())
                .completedAt(milestone.getCompletedAt().toString())
                .build();
    }

    /**
     * Mark a milestone as not completed
     */
    @Transactional
    public LearningPhaseDto uncompleteMilestone(UUID userId, UUID milestoneId) {
        LearningMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MILESTONE_NOT_FOUND));

        // Verify ownership through path -> user
        if (!milestone.getPath().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        milestone.setIsCompleted(false);
        milestone.setCompletedAt(null);
        milestone = milestoneRepository.save(milestone);

        log.info("Milestone {} marked as not completed", milestoneId);

        return LearningPhaseDto.builder()
                .id(milestone.getId().toString())
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .type(milestone.getType())
                .isCompleted(milestone.getIsCompleted())
                .orderIndex(milestone.getOrderIndex())
                .completedAt(null)
                .build();
    }

    // ==================== Summary ====================

    /**
     * Get growth summary for a user
     */
    @Transactional(readOnly = true)
    public GrowthSummaryDto getSummary(UUID userId) {
        List<GrowthGoal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        int activeGoals = (int) goals.stream()
                .filter(g -> g.getStatus() == GoalStatus.IN_PROGRESS || g.getStatus() == GoalStatus.NOT_STARTED)
                .count();

        List<LearningPath> paths = pathRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        int totalMilestones = 0;
        int completedMilestones = 0;

        for (LearningPath path : paths) {
            List<LearningMilestone> milestones = milestoneRepository.findByPathIdOrderByOrderIndexAsc(path.getId());
            totalMilestones += milestones.size();
            completedMilestones += milestones.stream().filter(LearningMilestone::getIsCompleted).count();
        }

        int overallProgress = totalMilestones > 0 ? (completedMilestones * 100) / totalMilestones : 0;

        return GrowthSummaryDto.builder()
                .activeGoals(activeGoals)
                .completedMilestones(completedMilestones)
                .totalMilestones(totalMilestones)
                .overallProgress(overallProgress)
                .build();
    }
}
