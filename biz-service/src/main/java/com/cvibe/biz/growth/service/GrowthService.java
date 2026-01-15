package com.cvibe.biz.growth.service;

import com.cvibe.biz.growth.dto.*;
import com.cvibe.biz.growth.entity.*;
import com.cvibe.biz.growth.entity.GrowthGoal.GoalStatus;
import com.cvibe.biz.growth.entity.LearningMilestone.MilestoneType;
import com.cvibe.biz.growth.entity.LearningPath.DifficultyLevel;
import com.cvibe.biz.growth.entity.LearningPath.PathFocus;
import com.cvibe.biz.growth.entity.LearningPath.PathStatus;
import com.cvibe.biz.growth.entity.SkillGap.GapPriority;
import com.cvibe.biz.growth.entity.SkillGap.GapStatus;
import com.cvibe.biz.growth.entity.SkillGap.SkillCategory;
import com.cvibe.biz.growth.repository.*;
import com.cvibe.biz.profile.entity.ProfileSkill;
import com.cvibe.biz.profile.entity.UserProfile;
import com.cvibe.biz.profile.repository.ProfileSkillRepository;
import com.cvibe.biz.profile.repository.UserProfileRepository;
import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GrowthService
 * 
 * Handles career growth tracking, gap analysis, and learning path generation.
 * Compares user profile against target requirements and generates actionable roadmaps.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrowthService {

    private final GrowthGoalRepository goalRepository;
    private final SkillGapRepository skillGapRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningMilestoneRepository milestoneRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final ProfileSkillRepository skillRepository;

    // ================== Goal Management ==================

    /**
     * Create a new growth goal
     */
    @Transactional
    public GrowthGoalDto createGoal(UUID userId, CreateGoalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Deactivate other active goals (only one primary goal at a time)
        List<GrowthGoal> activeGoals = goalRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
        activeGoals.forEach(g -> {
            g.setIsActive(false);
            goalRepository.save(g);
        });

        GrowthGoal goal = GrowthGoal.builder()
                .user(user)
                .targetRole(request.getTargetRole())
                .targetCompany(request.getTargetCompany())
                .targetLevel(request.getTargetLevel())
                .jobRequirements(request.getJobRequirements())
                .jdFilePath(request.getJdFilePath())
                .targetDate(request.getTargetDate())
                .isActive(true)
                .status(GoalStatus.ACTIVE)
                .progressPercent(0)
                .build();

        goal = goalRepository.save(goal);
        log.info("Created growth goal {} for user {}", goal.getId(), userId);

        // Trigger initial gap analysis
        analyzeGaps(userId, goal.getId());

        return GrowthGoalDto.from(goal, true);
    }

    /**
     * Get user's goals
     */
    @Transactional(readOnly = true)
    public Page<GrowthGoalDto> getUserGoals(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(g -> GrowthGoalDto.from(g, false));
    }

    /**
     * Get goal details
     */
    @Transactional(readOnly = true)
    public GrowthGoalDto getGoal(UUID userId, UUID goalId) {
        GrowthGoal goal = getGoalForUser(userId, goalId);
        return GrowthGoalDto.from(goal, true);
    }

    /**
     * Update goal
     */
    @Transactional
    public GrowthGoalDto updateGoal(UUID userId, UUID goalId, CreateGoalRequest request) {
        GrowthGoal goal = getGoalForUser(userId, goalId);

        goal.setTargetRole(request.getTargetRole());
        goal.setTargetCompany(request.getTargetCompany());
        goal.setTargetLevel(request.getTargetLevel());
        goal.setJobRequirements(request.getJobRequirements());
        goal.setTargetDate(request.getTargetDate());

        goal = goalRepository.save(goal);

        // Re-analyze gaps if requirements changed
        if (request.getJobRequirements() != null) {
            analyzeGaps(userId, goalId);
        }

        return GrowthGoalDto.from(goal, true);
    }

    /**
     * Set goal as primary active goal
     */
    @Transactional
    public GrowthGoalDto setAsPrimaryGoal(UUID userId, UUID goalId) {
        // Deactivate all other goals
        List<GrowthGoal> activeGoals = goalRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
        activeGoals.forEach(g -> {
            g.setIsActive(false);
            goalRepository.save(g);
        });

        // Activate this goal
        GrowthGoal goal = getGoalForUser(userId, goalId);
        goal.setAsActive();
        goal = goalRepository.save(goal);

        return GrowthGoalDto.from(goal, true);
    }

    /**
     * Mark goal as achieved
     */
    @Transactional
    public GrowthGoalDto achieveGoal(UUID userId, UUID goalId) {
        GrowthGoal goal = getGoalForUser(userId, goalId);
        goal.markAsAchieved();
        goal = goalRepository.save(goal);
        log.info("Goal {} marked as achieved for user {}", goalId, userId);
        return GrowthGoalDto.from(goal);
    }

    /**
     * Delete goal
     */
    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {
        GrowthGoal goal = getGoalForUser(userId, goalId);
        goal.abandon();
        goalRepository.save(goal);
        log.info("Deleted goal {} for user {}", goalId, userId);
    }

    // ================== Gap Analysis ==================

    /**
     * Analyze gaps for a goal
     * TODO: Replace with AI-powered analysis via gRPC
     */
    @Transactional
    public GrowthGoalDto analyzeGaps(UUID userId, UUID goalId) {
        GrowthGoal goal = getGoalForUser(userId, goalId);

        // Get user profile and skills
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
        List<ProfileSkill> userSkills = skillRepository.findByProfileId(profile.getId());

        // Extract required skills from job requirements
        Set<String> requiredSkills = extractRequiredSkills(goal.getJobRequirements());
        Set<String> preferredSkills = extractPreferredSkills(goal.getJobRequirements());

        // Map user skills for lookup
        Map<String, ProfileSkill> userSkillMap = userSkills.stream()
                .collect(Collectors.toMap(
                        s -> s.getName().toLowerCase(),
                        s -> s,
                        (a, b) -> a
                ));

        // Clear existing gaps
        skillGapRepository.deleteByGoalId(goalId);

        // Analyze gaps
        List<SkillGap> gaps = new ArrayList<>();
        int totalRequired = requiredSkills.size() + preferredSkills.size();
        int matched = 0;

        // Check required skills
        for (String skill : requiredSkills) {
            ProfileSkill userSkill = userSkillMap.get(skill.toLowerCase());
            if (userSkill == null) {
                SkillGap gap = createSkillGap(goal, skill, 0, 70, GapPriority.HIGH, true, false);
                gaps.add(gap);
            } else {
                int level = calculateProficiencyLevel(userSkill);
                if (level < 70) {
                    SkillGap gap = createSkillGap(goal, skill, level, 70, GapPriority.MEDIUM, true, false);
                    gaps.add(gap);
                } else {
                    matched++;
                }
            }
        }

        // Check preferred skills
        for (String skill : preferredSkills) {
            if (requiredSkills.contains(skill)) continue;  // Already processed
            ProfileSkill userSkill = userSkillMap.get(skill.toLowerCase());
            if (userSkill == null) {
                SkillGap gap = createSkillGap(goal, skill, 0, 50, GapPriority.LOW, false, true);
                gaps.add(gap);
            } else {
                matched++;
            }
        }

        // Save gaps
        gaps.forEach(g -> goal.addSkillGap(g));
        
        // Calculate match score
        double matchScore = totalRequired > 0 ? (double) matched / totalRequired * 100 : 50;
        String summary = generateAnalysisSummary(gaps, matchScore);
        goal.recordAnalysis(summary, matchScore);

        goalRepository.save(goal);
        log.info("Analyzed {} gaps for goal {}, match score: {}", gaps.size(), goalId, matchScore);

        // Generate learning paths based on gaps
        generateLearningPaths(userId, goalId);

        return GrowthGoalDto.from(goal, true);
    }

    /**
     * Get skill gaps for a goal
     */
    @Transactional(readOnly = true)
    public List<SkillGapDto> getSkillGaps(UUID userId, UUID goalId) {
        getGoalForUser(userId, goalId);  // Verify access
        return skillGapRepository.findByGoalIdOrderByPriorityAscCreatedAtDesc(goalId)
                .stream()
                .map(SkillGapDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Update skill gap progress
     */
    @Transactional
    public SkillGapDto updateGapProgress(UUID userId, UUID gapId, int newLevel) {
        SkillGap gap = skillGapRepository.findById(gapId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!gap.getGoal().belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        gap.updateProgress(newLevel);
        gap = skillGapRepository.save(gap);

        // Update goal progress
        updateGoalProgress(gap.getGoal().getId());

        return SkillGapDto.from(gap);
    }

    // ================== Learning Paths ==================

    /**
     * Generate learning paths for a goal
     * TODO: Replace with AI-powered generation
     */
    @Transactional
    public List<LearningPathDto> generateLearningPaths(UUID userId, UUID goalId) {
        GrowthGoal goal = getGoalForUser(userId, goalId);

        // Clear existing paths
        learningPathRepository.deleteByGoalId(goalId);

        List<SkillGap> unresolvedGaps = skillGapRepository.findUnresolvedByGoalId(goalId);

        if (unresolvedGaps.isEmpty()) {
            return List.of();
        }

        // Group gaps by category and create paths
        Map<SkillCategory, List<SkillGap>> gapsByCategory = unresolvedGaps.stream()
                .filter(g -> g.getCategory() != null)
                .collect(Collectors.groupingBy(SkillGap::getCategory));

        List<LearningPath> paths = new ArrayList<>();
        int sortOrder = 0;

        for (Map.Entry<SkillCategory, List<SkillGap>> entry : gapsByCategory.entrySet()) {
            LearningPath path = createLearningPathForCategory(goal, entry.getKey(), entry.getValue(), sortOrder++);
            paths.add(path);
        }

        // Handle gaps without category
        List<SkillGap> uncategorized = unresolvedGaps.stream()
                .filter(g -> g.getCategory() == null)
                .collect(Collectors.toList());
        if (!uncategorized.isEmpty()) {
            LearningPath path = createGenericLearningPath(goal, uncategorized, sortOrder);
            paths.add(path);
        }

        paths.forEach(p -> goal.addLearningPath(p));
        goalRepository.save(goal);

        log.info("Generated {} learning paths for goal {}", paths.size(), goalId);

        return paths.stream()
                .map(p -> LearningPathDto.from(p, true))
                .collect(Collectors.toList());
    }

    /**
     * Get learning paths for a goal
     */
    @Transactional(readOnly = true)
    public List<LearningPathDto> getLearningPaths(UUID userId, UUID goalId) {
        getGoalForUser(userId, goalId);
        return learningPathRepository.findByGoalIdOrderBySortOrderAsc(goalId)
                .stream()
                .map(p -> LearningPathDto.from(p, true))
                .collect(Collectors.toList());
    }

    /**
     * Complete a milestone
     */
    @Transactional
    public LearningMilestoneDto completeMilestone(UUID userId, UUID milestoneId) {
        LearningMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!milestone.getLearningPath().getGoal().belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        milestone.markCompleted();
        milestone = milestoneRepository.save(milestone);

        // Update goal progress
        UUID goalId = milestone.getLearningPath().getGoal().getId();
        updateGoalProgress(goalId);

        return LearningMilestoneDto.from(milestone);
    }

    /**
     * Uncomplete a milestone
     */
    @Transactional
    public LearningMilestoneDto uncompleteMilestone(UUID userId, UUID milestoneId) {
        LearningMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!milestone.getLearningPath().getGoal().belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        milestone.markIncomplete();
        milestone = milestoneRepository.save(milestone);

        UUID goalId = milestone.getLearningPath().getGoal().getId();
        updateGoalProgress(goalId);

        return LearningMilestoneDto.from(milestone);
    }

    // ================== Dashboard ==================

    /**
     * Get growth summary for dashboard
     */
    @Transactional(readOnly = true)
    public GrowthSummary getSummary(UUID userId) {
        long activeGoals = goalRepository.countByUserIdAndStatus(userId, GoalStatus.ACTIVE);
        long achievedGoals = goalRepository.countByUserIdAndStatus(userId, GoalStatus.ACHIEVED);
        Double avgProgress = goalRepository.getAverageProgress(userId);

        GrowthGoal primaryGoal = goalRepository.findFirstByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId)
                .orElse(null);

        List<SkillGap> unresolvedGaps = skillGapRepository.findAllUnresolvedByUserId(userId);
        long criticalGaps = unresolvedGaps.stream()
                .filter(g -> g.getPriority() == GapPriority.CRITICAL || g.getPriority() == GapPriority.HIGH)
                .count();

        List<LearningPath> inProgressPaths = learningPathRepository.findInProgressByUserId(userId);
        long completedPaths = learningPathRepository.findAllByUserId(userId).stream()
                .filter(p -> p.getStatus() == PathStatus.COMPLETED)
                .count();

        List<LearningMilestone> recentMilestones = milestoneRepository.findRecentCompletedByUserId(userId);

        Integer remainingHours = unresolvedGaps.stream()
                .mapToInt(g -> g.getEstimatedHours() != null ? g.getEstimatedHours() : 0)
                .sum();

        return GrowthSummary.builder()
                .activeGoals(activeGoals)
                .achievedGoals(achievedGoals)
                .averageProgress(avgProgress != null ? avgProgress : 0.0)
                .totalGaps(unresolvedGaps.size())
                .criticalGaps(criticalGaps)
                .inProgressPaths(inProgressPaths.size())
                .completedPaths(completedPaths)
                .remainingHours(remainingHours)
                .primaryGoal(primaryGoal != null ? GrowthGoalDto.from(primaryGoal, false) : null)
                .recentGaps(unresolvedGaps.stream()
                        .limit(5)
                        .map(SkillGapDto::from)
                        .collect(Collectors.toList()))
                .currentPaths(inProgressPaths.stream()
                        .limit(3)
                        .map(p -> LearningPathDto.from(p, false))
                        .collect(Collectors.toList()))
                .recentMilestones(recentMilestones.stream()
                        .limit(5)
                        .map(LearningMilestoneDto::from)
                        .collect(Collectors.toList()))
                .build();
    }

    // ================== Private Helper Methods ==================

    private GrowthGoal getGoalForUser(UUID userId, UUID goalId) {
        GrowthGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Goal not found"));

        if (!goal.belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return goal;
    }

    private Set<String> extractRequiredSkills(String requirements) {
        if (requirements == null || requirements.isBlank()) {
            return Set.of();
        }

        Set<String> skills = new HashSet<>();
        String lower = requirements.toLowerCase();

        TECH_SKILLS.forEach(skill -> {
            if (lower.contains(skill.toLowerCase())) {
                skills.add(skill);
            }
        });

        return skills;
    }

    private Set<String> extractPreferredSkills(String requirements) {
        // In production, use NLP to distinguish required vs preferred
        return Set.of();  // Simplified for now
    }

    private int calculateProficiencyLevel(ProfileSkill skill) {
        if (skill.getYearsOfExperience() == null) return 30;
        int years = skill.getYearsOfExperience();
        if (years >= 5) return 90;
        if (years >= 3) return 70;
        if (years >= 1) return 50;
        return 30;
    }

    private SkillGap createSkillGap(GrowthGoal goal, String skillName, int current, int required,
                                     GapPriority priority, boolean isRequired, boolean isPreferred) {
        SkillCategory category = categorizeSkill(skillName);
        int estimatedHours = estimateLearningHours(current, required, category);

        return SkillGap.builder()
                .goal(goal)
                .skillName(skillName)
                .category(category)
                .currentLevel(current)
                .requiredLevel(required)
                .priority(priority)
                .status(GapStatus.IDENTIFIED)
                .isRequired(isRequired)
                .isPreferred(isPreferred)
                .estimatedHours(estimatedHours)
                .recommendation(generateRecommendation(skillName, category))
                .learningResources(suggestResources(skillName))
                .build();
    }

    private SkillCategory categorizeSkill(String skill) {
        String lower = skill.toLowerCase();
        if (LANGUAGES.stream().anyMatch(l -> lower.contains(l.toLowerCase()))) {
            return SkillCategory.PROGRAMMING_LANGUAGE;
        }
        if (FRAMEWORKS.stream().anyMatch(f -> lower.contains(f.toLowerCase()))) {
            return SkillCategory.FRAMEWORK;
        }
        if (DATABASES.stream().anyMatch(d -> lower.contains(d.toLowerCase()))) {
            return SkillCategory.DATABASE;
        }
        if (CLOUD.stream().anyMatch(c -> lower.contains(c.toLowerCase()))) {
            return SkillCategory.CLOUD;
        }
        if (DEVOPS.stream().anyMatch(d -> lower.contains(d.toLowerCase()))) {
            return SkillCategory.DEVOPS;
        }
        return SkillCategory.OTHER;
    }

    private int estimateLearningHours(int current, int required, SkillCategory category) {
        int gap = required - current;
        int baseHours = switch (category) {
            case PROGRAMMING_LANGUAGE -> 40;
            case FRAMEWORK -> 30;
            case DATABASE -> 20;
            case CLOUD -> 25;
            case DEVOPS -> 30;
            case SYSTEM_DESIGN -> 50;
            default -> 20;
        };
        return (int) (baseHours * gap / 100.0);
    }

    private String generateRecommendation(String skill, SkillCategory category) {
        return switch (category) {
            case PROGRAMMING_LANGUAGE -> "Start with official documentation and build small projects";
            case FRAMEWORK -> "Complete a tutorial project and read the framework's best practices";
            case DATABASE -> "Practice with sample datasets and learn optimization techniques";
            case CLOUD -> "Get hands-on with free tier and work towards certification";
            case DEVOPS -> "Set up a CI/CD pipeline for a personal project";
            default -> "Study fundamentals and apply in real projects";
        };
    }

    private String suggestResources(String skill) {
        return "Official docs, YouTube tutorials, Udemy/Coursera courses";
    }

    private String generateAnalysisSummary(List<SkillGap> gaps, double matchScore) {
        StringBuilder summary = new StringBuilder();

        if (matchScore >= 80) {
            summary.append("Excellent match! You have most required skills. ");
        } else if (matchScore >= 60) {
            summary.append("Good potential. Some skill gaps to address. ");
        } else if (matchScore >= 40) {
            summary.append("Moderate match. Several areas need development. ");
        } else {
            summary.append("Significant gaps identified. Focused learning recommended. ");
        }

        long critical = gaps.stream().filter(g -> g.getPriority() == GapPriority.CRITICAL || g.getPriority() == GapPriority.HIGH).count();
        if (critical > 0) {
            summary.append(String.format("Focus on %d high-priority skills first.", critical));
        }

        return summary.toString();
    }

    private void updateGoalProgress(UUID goalId) {
        GrowthGoal goal = goalRepository.findById(goalId).orElse(null);
        if (goal == null) return;

        // Calculate progress from learning paths
        Double avgCompletion = learningPathRepository.getAverageCompletion(goalId);
        if (avgCompletion != null) {
            goal.updateProgress(avgCompletion.intValue());
            goalRepository.save(goal);
        }
    }

    private LearningPath createLearningPathForCategory(GrowthGoal goal, SkillCategory category,
                                                        List<SkillGap> gaps, int sortOrder) {
        String title = category.name().replace("_", " ") + " Skills";
        PathFocus focus = mapCategoryToFocus(category);

        int totalHours = gaps.stream()
                .mapToInt(g -> g.getEstimatedHours() != null ? g.getEstimatedHours() : 10)
                .sum();

        LearningPath path = LearningPath.builder()
                .goal(goal)
                .title(title)
                .description("Learn and improve " + title.toLowerCase())
                .focus(focus)
                .difficulty(DifficultyLevel.INTERMEDIATE)
                .estimatedHours(totalHours)
                .targetDate(LocalDate.now().plusMonths(3))
                .sortOrder(sortOrder)
                .status(PathStatus.NOT_STARTED)
                .completionPercent(0)
                .build();

        // Create milestones for each gap
        int milestoneOrder = 0;
        for (SkillGap gap : gaps) {
            LearningMilestone milestone = LearningMilestone.builder()
                    .learningPath(path)
                    .title("Learn " + gap.getSkillName())
                    .description(gap.getRecommendation())
                    .type(MilestoneType.COURSE)
                    .estimatedHours(gap.getEstimatedHours())
                    .resourceUrl(gap.getLearningResources())
                    .sortOrder(milestoneOrder++)
                    .isCompleted(false)
                    .build();
            path.addMilestone(milestone);
        }

        return path;
    }

    private LearningPath createGenericLearningPath(GrowthGoal goal, List<SkillGap> gaps, int sortOrder) {
        return createLearningPathForCategory(goal, SkillCategory.OTHER, gaps, sortOrder);
    }

    private PathFocus mapCategoryToFocus(SkillCategory category) {
        return switch (category) {
            case PROGRAMMING_LANGUAGE, FRAMEWORK -> PathFocus.TECHNICAL_SKILLS;
            case DATABASE, CLOUD, DEVOPS -> PathFocus.SYSTEM_DESIGN;
            case DATA_STRUCTURE, ALGORITHM -> PathFocus.CODING_PRACTICE;
            case SOFT_SKILL -> PathFocus.SOFT_SKILLS;
            default -> PathFocus.TECHNICAL_SKILLS;
        };
    }

    // Skill categorization lists
    private static final List<String> LANGUAGES = List.of(
            "Java", "Python", "JavaScript", "TypeScript", "Go", "Rust", "C++", "C#", "Ruby", "Kotlin", "Swift", "Scala"
    );
    private static final List<String> FRAMEWORKS = List.of(
            "Spring", "React", "Angular", "Vue", "Node.js", "Django", "Flask", "FastAPI", "Rails", "Next.js"
    );
    private static final List<String> DATABASES = List.of(
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Elasticsearch", "Cassandra", "DynamoDB"
    );
    private static final List<String> CLOUD = List.of(
            "AWS", "GCP", "Azure", "S3", "EC2", "Lambda", "CloudFormation"
    );
    private static final List<String> DEVOPS = List.of(
            "Docker", "Kubernetes", "Terraform", "Jenkins", "CI/CD", "Ansible", "Prometheus", "Grafana"
    );
    private static final List<String> TECH_SKILLS = new ArrayList<>();

    static {
        TECH_SKILLS.addAll(LANGUAGES);
        TECH_SKILLS.addAll(FRAMEWORKS);
        TECH_SKILLS.addAll(DATABASES);
        TECH_SKILLS.addAll(CLOUD);
        TECH_SKILLS.addAll(DEVOPS);
        TECH_SKILLS.addAll(List.of("System Design", "Microservices", "REST", "GraphQL", "gRPC", "Kafka", "RabbitMQ",
                "Machine Learning", "Deep Learning", "NLP", "Computer Vision", "Agile", "Scrum"));
    }
}
