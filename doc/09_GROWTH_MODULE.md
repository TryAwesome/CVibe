# Growth 模块详细设计

> 职业成长规划模块，提供目标设置、技能差距分析、学习路径

---

## 1. 模块结构

```
biz/growth/
├── controller/
│   └── GrowthController.java
├── service/
│   └── GrowthService.java
├── repository/
│   └── GrowthGoalRepository.java
├── entity/
│   └── GrowthGoal.java
└── dto/
    ├── GrowthGoalDto.java
    ├── GapAnalysisDto.java
    ├── LearningPathDto.java
    └── GrowthProgressDto.java
```

---

## 2. API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/growth/goals` | 获取成长目标列表 |
| POST | `/api/growth/goals` | 创建成长目标 |
| PUT | `/api/growth/goals/{goalId}` | 更新成长目标 |
| DELETE | `/api/growth/goals/{goalId}` | 删除成长目标 |
| GET | `/api/growth/gap-analysis` | 获取技能差距分析 |
| GET | `/api/growth/learning-path` | 获取学习路径 |
| GET | `/api/growth/progress` | 获取成长进度 |

---

## 3. 前端期望的数据结构

### 3.1 GrowthGoal

```typescript
interface GrowthGoal {
  id: string;
  userId: string;
  title: string;
  description?: string;
  targetRole: string;
  targetDate: string;           // ISO date
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'PAUSED';
  progress: number;             // 0-100
  milestones: Milestone[];
  createdAt: string;
  updatedAt: string;
}

interface Milestone {
  id: string;
  title: string;
  description?: string;
  dueDate?: string;
  isCompleted: boolean;
  completedAt?: string;
}
```

### 3.2 GapAnalysis

```typescript
interface GapAnalysis {
  targetRole: string;
  currentSkills: SkillAssessment[];
  requiredSkills: SkillRequirement[];
  gaps: SkillGap[];
  overallReadiness: number;     // 0-100
  summary: string;
}

interface SkillAssessment {
  skill: string;
  currentLevel: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';
  source: 'PROFILE' | 'RESUME' | 'SELF_ASSESSED';
}

interface SkillRequirement {
  skill: string;
  requiredLevel: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';
  importance: 'MUST_HAVE' | 'NICE_TO_HAVE';
}

interface SkillGap {
  skill: string;
  currentLevel: string;
  requiredLevel: string;
  gapLevel: number;             // 1-4
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  estimatedTimeToClose: string; // "3 months"
}
```

### 3.3 LearningPath

```typescript
interface LearningPath {
  targetRole: string;
  totalDuration: string;        // "6 months"
  phases: LearningPhase[];
}

interface LearningPhase {
  phase: number;
  title: string;
  description: string;
  duration: string;
  skills: string[];
  resources: LearningResource[];
}

interface LearningResource {
  title: string;
  type: 'COURSE' | 'BOOK' | 'PROJECT' | 'ARTICLE' | 'VIDEO';
  url?: string;
  platform?: string;
  estimatedTime: string;
  difficulty: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
}
```

### 3.4 GrowthProgress

```typescript
interface GrowthProgress {
  userId: string;
  overallProgress: number;
  goalsProgress: GoalProgress[];
  skillsImproved: string[];
  recentAchievements: Achievement[];
  weeklyStats: WeeklyStat[];
}

interface GoalProgress {
  goalId: string;
  goalTitle: string;
  progress: number;
  completedMilestones: number;
  totalMilestones: number;
}

interface Achievement {
  id: string;
  title: string;
  description: string;
  earnedAt: string;
  type: 'GOAL_COMPLETED' | 'MILESTONE_REACHED' | 'SKILL_IMPROVED' | 'STREAK';
}

interface WeeklyStat {
  week: string;
  activitiesCompleted: number;
  hoursSpent: number;
}
```

---

## 4. 详细实现

### 4.1 Controller

```java
@RestController
@RequestMapping("/growth")
@RequiredArgsConstructor
public class GrowthController {

    private final GrowthService growthService;

    /**
     * 获取成长目标列表
     * GET /api/growth/goals
     */
    @GetMapping("/goals")
    public ApiResponse<List<GrowthGoalDto>> getGoals(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status) {
        List<GrowthGoalDto> goals = growthService.getGoals(principal.getId(), status);
        return ApiResponse.success(goals);
    }

    /**
     * 创建成长目标
     * POST /api/growth/goals
     */
    @PostMapping("/goals")
    public ApiResponse<GrowthGoalDto> createGoal(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGoalRequest request) {
        GrowthGoalDto goal = growthService.createGoal(principal.getId(), request);
        return ApiResponse.success(goal);
    }

    /**
     * 更新成长目标
     * PUT /api/growth/goals/{goalId}
     */
    @PutMapping("/goals/{goalId}")
    public ApiResponse<GrowthGoalDto> updateGoal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID goalId,
            @Valid @RequestBody UpdateGoalRequest request) {
        GrowthGoalDto goal = growthService.updateGoal(principal.getId(), goalId, request);
        return ApiResponse.success(goal);
    }

    /**
     * 删除成长目标
     * DELETE /api/growth/goals/{goalId}
     */
    @DeleteMapping("/goals/{goalId}")
    public ApiResponse<Void> deleteGoal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID goalId) {
        growthService.deleteGoal(principal.getId(), goalId);
        return ApiResponse.success();
    }

    /**
     * 获取技能差距分析
     * GET /api/growth/gap-analysis
     */
    @GetMapping("/gap-analysis")
    public ApiResponse<GapAnalysisDto> getGapAnalysis(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String targetRole) {
        GapAnalysisDto analysis = growthService.getGapAnalysis(principal.getId(), targetRole);
        return ApiResponse.success(analysis);
    }

    /**
     * 获取学习路径
     * GET /api/growth/learning-path
     */
    @GetMapping("/learning-path")
    public ApiResponse<LearningPathDto> getLearningPath(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String targetRole) {
        LearningPathDto path = growthService.getLearningPath(principal.getId(), targetRole);
        return ApiResponse.success(path);
    }

    /**
     * 获取成长进度
     * GET /api/growth/progress
     */
    @GetMapping("/progress")
    public ApiResponse<GrowthProgressDto> getProgress(
            @AuthenticationPrincipal UserPrincipal principal) {
        GrowthProgressDto progress = growthService.getProgress(principal.getId());
        return ApiResponse.success(progress);
    }
}
```

### 4.2 DTOs

```java
// GrowthGoalDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthGoalDto {
    private String id;
    private String userId;
    private String title;
    private String description;
    private String targetRole;
    private String targetDate;
    private String status;
    private Integer progress;
    private List<MilestoneDto> milestones;
    private String createdAt;
    private String updatedAt;

    public static GrowthGoalDto fromEntity(GrowthGoal entity) {
        return GrowthGoalDto.builder()
                .id(entity.getId().toString())
                .userId(entity.getUser().getId().toString())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .targetRole(entity.getTargetRole())
                .targetDate(entity.getTargetDate().toString())
                .status(entity.getStatus().name())
                .progress(entity.getProgress())
                .milestones(parseMilestones(entity.getMilestonesJson()))
                .createdAt(entity.getCreatedAt().toString())
                .updatedAt(entity.getUpdatedAt().toString())
                .build();
    }

    private static List<MilestoneDto> parseMilestones(String json) {
        if (json == null) return new ArrayList<>();
        try {
            return new ObjectMapper().readValue(json, 
                    new TypeReference<List<MilestoneDto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

// MilestoneDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneDto {
    private String id;
    private String title;
    private String description;
    private String dueDate;
    private Boolean isCompleted;
    private String completedAt;
}

// CreateGoalRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGoalRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title too long")
    private String title;

    private String description;

    @NotBlank(message = "Target role is required")
    private String targetRole;

    @NotNull(message = "Target date is required")
    private String targetDate;  // ISO date

    private List<MilestoneDto> milestones;
}

// UpdateGoalRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGoalRequest {
    private String title;
    private String description;
    private String targetRole;
    private String targetDate;
    private String status;      // NOT_STARTED, IN_PROGRESS, COMPLETED, PAUSED
    private Integer progress;
    private List<MilestoneDto> milestones;
}

// GapAnalysisDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapAnalysisDto {
    private String targetRole;
    private List<SkillAssessmentDto> currentSkills;
    private List<SkillRequirementDto> requiredSkills;
    private List<SkillGapDto> gaps;
    private Integer overallReadiness;
    private String summary;
}

// SkillGapDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillGapDto {
    private String skill;
    private String currentLevel;
    private String requiredLevel;
    private Integer gapLevel;
    private String priority;
    private String estimatedTimeToClose;
}

// LearningPathDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathDto {
    private String targetRole;
    private String totalDuration;
    private List<LearningPhaseDto> phases;
}

// LearningPhaseDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPhaseDto {
    private Integer phase;
    private String title;
    private String description;
    private String duration;
    private List<String> skills;
    private List<LearningResourceDto> resources;
}

// LearningResourceDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningResourceDto {
    private String title;
    private String type;        // COURSE, BOOK, PROJECT, ARTICLE, VIDEO
    private String url;
    private String platform;
    private String estimatedTime;
    private String difficulty;
}

// GrowthProgressDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthProgressDto {
    private String userId;
    private Integer overallProgress;
    private List<GoalProgressDto> goalsProgress;
    private List<String> skillsImproved;
    private List<AchievementDto> recentAchievements;
    private List<WeeklyStatDto> weeklyStats;
}
```

### 4.3 Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthService {

    private final GrowthGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final ResumeService resumeService;
    private final AiEngineClient aiEngineClient;
    private final ObjectMapper objectMapper;

    /**
     * 获取目标列表
     */
    @Transactional(readOnly = true)
    public List<GrowthGoalDto> getGoals(UUID userId, String status) {
        List<GrowthGoal> goals;
        if (status != null && !status.isEmpty()) {
            goals = goalRepository.findByUserIdAndStatus(userId, 
                    GoalStatus.valueOf(status));
        } else {
            goals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return goals.stream()
                .map(GrowthGoalDto::fromEntity)
                .toList();
    }

    /**
     * 创建目标
     */
    @Transactional
    public GrowthGoalDto createGoal(UUID userId, CreateGoalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 初始化里程碑
        List<MilestoneDto> milestones = request.getMilestones();
        if (milestones == null) {
            milestones = new ArrayList<>();
        }
        // 为每个里程碑生成 ID
        for (MilestoneDto m : milestones) {
            if (m.getId() == null) {
                m.setId(UUID.randomUUID().toString());
            }
            if (m.getIsCompleted() == null) {
                m.setIsCompleted(false);
            }
        }

        GrowthGoal goal = GrowthGoal.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .targetRole(request.getTargetRole())
                .targetDate(LocalDate.parse(request.getTargetDate()))
                .status(GoalStatus.NOT_STARTED)
                .progress(0)
                .milestonesJson(toJson(milestones))
                .build();

        goal = goalRepository.save(goal);
        log.info("Created growth goal {} for user: {}", goal.getId(), userId);

        return GrowthGoalDto.fromEntity(goal);
    }

    /**
     * 更新目标
     */
    @Transactional
    public GrowthGoalDto updateGoal(UUID userId, UUID goalId, UpdateGoalRequest request) {
        GrowthGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOAL_NOT_FOUND));

        // 验证归属
        if (!goal.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 更新字段
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
            goal.setTargetDate(LocalDate.parse(request.getTargetDate()));
        }
        if (request.getStatus() != null) {
            goal.setStatus(GoalStatus.valueOf(request.getStatus()));
        }
        if (request.getProgress() != null) {
            goal.setProgress(request.getProgress());
        }
        if (request.getMilestones() != null) {
            goal.setMilestonesJson(toJson(request.getMilestones()));
        }

        goal = goalRepository.save(goal);
        log.info("Updated growth goal {} for user: {}", goalId, userId);

        return GrowthGoalDto.fromEntity(goal);
    }

    /**
     * 删除目标
     */
    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {
        GrowthGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOAL_NOT_FOUND));

        if (!goal.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        goalRepository.delete(goal);
        log.info("Deleted growth goal {} for user: {}", goalId, userId);
    }

    /**
     * 获取技能差距分析
     */
    @Transactional(readOnly = true)
    public GapAnalysisDto getGapAnalysis(UUID userId, String targetRole) {
        // 获取用户当前技能
        ProfileDto profile = profileService.getOrCreateProfile(userId);
        ResumeDto resume = resumeService.getLatestResume(userId);

        // 收集用户技能
        List<SkillAssessmentDto> currentSkills = collectCurrentSkills(profile, resume);

        // 调用 AI 引擎分析差距
        return aiEngineClient.analyzeSkillGap(targetRole, currentSkills);
    }

    /**
     * 获取学习路径
     */
    @Transactional(readOnly = true)
    public LearningPathDto getLearningPath(UUID userId, String targetRole) {
        // 获取用户当前技能
        ProfileDto profile = profileService.getOrCreateProfile(userId);
        ResumeDto resume = resumeService.getLatestResume(userId);

        List<SkillAssessmentDto> currentSkills = collectCurrentSkills(profile, resume);

        // 调用 AI 引擎生成学习路径
        return aiEngineClient.generateLearningPath(targetRole, currentSkills);
    }

    /**
     * 获取成长进度
     */
    @Transactional(readOnly = true)
    public GrowthProgressDto getProgress(UUID userId) {
        List<GrowthGoal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // 计算总体进度
        int overallProgress = calculateOverallProgress(goals);

        // 收集目标进度
        List<GoalProgressDto> goalsProgress = goals.stream()
                .map(g -> GoalProgressDto.builder()
                        .goalId(g.getId().toString())
                        .goalTitle(g.getTitle())
                        .progress(g.getProgress())
                        .completedMilestones(countCompletedMilestones(g))
                        .totalMilestones(countTotalMilestones(g))
                        .build())
                .toList();

        // TODO: 收集技能提升和成就
        
        return GrowthProgressDto.builder()
                .userId(userId.toString())
                .overallProgress(overallProgress)
                .goalsProgress(goalsProgress)
                .skillsImproved(List.of())
                .recentAchievements(List.of())
                .weeklyStats(List.of())
                .build();
    }

    // ==================== Helper ====================

    private List<SkillAssessmentDto> collectCurrentSkills(ProfileDto profile, ResumeDto resume) {
        List<SkillAssessmentDto> skills = new ArrayList<>();

        // 从 Profile 收集
        if (profile != null && profile.getSkills() != null) {
            for (SkillDto skill : profile.getSkills()) {
                skills.add(SkillAssessmentDto.builder()
                        .skill(skill.getName())
                        .currentLevel(skill.getLevel())
                        .source("PROFILE")
                        .build());
            }
        }

        // 从 Resume 收集
        if (resume != null && resume.getParsedContent() != null &&
                resume.getParsedContent().getSkills() != null) {
            for (String skillName : resume.getParsedContent().getSkills()) {
                // 检查是否已存在
                boolean exists = skills.stream()
                        .anyMatch(s -> s.getSkill().equalsIgnoreCase(skillName));
                if (!exists) {
                    skills.add(SkillAssessmentDto.builder()
                            .skill(skillName)
                            .currentLevel("INTERMEDIATE")  // 默认中级
                            .source("RESUME")
                            .build());
                }
            }
        }

        return skills;
    }

    private int calculateOverallProgress(List<GrowthGoal> goals) {
        if (goals.isEmpty()) return 0;
        return (int) goals.stream()
                .mapToInt(GrowthGoal::getProgress)
                .average()
                .orElse(0);
    }

    private int countCompletedMilestones(GrowthGoal goal) {
        List<MilestoneDto> milestones = parseMilestones(goal.getMilestonesJson());
        return (int) milestones.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsCompleted()))
                .count();
    }

    private int countTotalMilestones(GrowthGoal goal) {
        return parseMilestones(goal.getMilestonesJson()).size();
    }

    private List<MilestoneDto> parseMilestones(String json) {
        if (json == null) return List.of();
        try {
            return objectMapper.readValue(json, 
                    new TypeReference<List<MilestoneDto>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
```

### 4.4 Entity

```java
@Entity
@Table(name = "growth_goals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthGoal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_role", nullable = false)
    private String targetRole;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status = GoalStatus.NOT_STARTED;

    @Column(nullable = false)
    private Integer progress = 0;

    @Column(name = "milestones_json", columnDefinition = "TEXT")
    private String milestonesJson;
}

public enum GoalStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    PAUSED
}
```

---

## 5. AI 引擎集成

### 5.1 差距分析 Prompt

```python
GAP_ANALYSIS_PROMPT = """
Analyze the skill gap for a candidate targeting {target_role}.

Current Skills:
{current_skills}

Provide a comprehensive gap analysis including:
1. Required skills for the target role with importance levels
2. Skill gaps with priority and estimated time to close
3. Overall readiness percentage
4. Summary with actionable insights

Output in JSON format:
{{
    "targetRole": "...",
    "requiredSkills": [
        {{"skill": "...", "requiredLevel": "ADVANCED", "importance": "MUST_HAVE"}}
    ],
    "gaps": [
        {{
            "skill": "...",
            "currentLevel": "BEGINNER",
            "requiredLevel": "ADVANCED",
            "gapLevel": 2,
            "priority": "HIGH",
            "estimatedTimeToClose": "3 months"
        }}
    ],
    "overallReadiness": 65,
    "summary": "..."
}}
"""
```

### 5.2 学习路径 Prompt

```python
LEARNING_PATH_PROMPT = """
Generate a personalized learning path for someone targeting {target_role}.

Current Skills: {current_skills}
Skill Gaps: {skill_gaps}

Create a phased learning plan with specific resources.

Output in JSON format:
{{
    "targetRole": "...",
    "totalDuration": "6 months",
    "phases": [
        {{
            "phase": 1,
            "title": "Foundation",
            "description": "...",
            "duration": "4 weeks",
            "skills": ["..."],
            "resources": [
                {{
                    "title": "...",
                    "type": "COURSE",
                    "url": "...",
                    "platform": "Coursera",
                    "estimatedTime": "20 hours",
                    "difficulty": "BEGINNER"
                }}
            ]
        }}
    ]
}}
"""
```

---

## 6. 错误码

| 错误码 | HTTP 状态 | 描述 |
|--------|----------|------|
| 80001 | 404 | Goal not found |
| 80002 | 400 | Invalid goal status |
| 80003 | 400 | Target date in the past |
| 80004 | 403 | Forbidden |
| 80005 | 503 | AI service unavailable |

---

## 7. 常见 Bug 及修复

### Bug 1: milestones 为 null 导致解析失败
**问题**: 新建目标时没有里程碑

**修复**: 返回空数组而非 null
```java
private static List<MilestoneDto> parseMilestones(String json) {
    if (json == null || json.isEmpty()) return new ArrayList<>();
    // ...
}
```

### Bug 2: progress 超出范围
**问题**: progress 可以设置为负数或超过 100

**修复**: 添加验证
```java
if (request.getProgress() != null) {
    int p = request.getProgress();
    goal.setProgress(Math.max(0, Math.min(100, p)));
}
```

### Bug 3: targetDate 解析失败
**问题**: 前端发送的日期格式不正确

**修复**: 使用灵活的解析
```java
private LocalDate parseDate(String dateStr) {
    try {
        return LocalDate.parse(dateStr);
    } catch (DateTimeParseException e) {
        throw new BusinessException(ErrorCode.INVALID_DATE_FORMAT);
    }
}
```

---

## 8. 测试用例

```java
@SpringBootTest
@AutoConfigureMockMvc
class GrowthControllerTest {

    @Test
    @WithMockUser(userId = "test-user-id")
    void createGoal_Success() throws Exception {
        CreateGoalRequest request = new CreateGoalRequest();
        request.setTitle("Become Senior Engineer");
        request.setTargetRole("Senior Software Engineer");
        request.setTargetDate("2025-12-31");
        request.setMilestones(List.of(
                MilestoneDto.builder()
                        .title("Complete System Design Course")
                        .dueDate("2025-06-01")
                        .build()
        ));

        mockMvc.perform(post("/api/growth/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Become Senior Engineer"))
                .andExpect(jsonPath("$.data.milestones").isArray());
    }

    @Test
    @WithMockUser(userId = "test-user-id")
    void getGapAnalysis_Success() throws Exception {
        mockMvc.perform(get("/api/growth/gap-analysis")
                .param("targetRole", "Senior Software Engineer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gaps").isArray());
    }
}
```
