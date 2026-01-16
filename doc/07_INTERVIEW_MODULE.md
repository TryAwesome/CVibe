# Interview 模块详细设计

> AI 驱动的面试准备与模拟模块

---

## 1. 模块结构

```
biz/interview/
├── controller/
│   └── InterviewController.java
├── service/
│   └── InterviewService.java
├── repository/
│   └── InterviewSessionRepository.java
├── entity/
│   └── InterviewSession.java
└── dto/
    ├── InterviewSessionDto.java
    ├── StartInterviewRequest.java
    ├── SubmitAnswerRequest.java
    └── InterviewMessageDto.java
```

---

## 2. API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/interview/sessions` | 开始面试会话 |
| GET | `/api/interview/sessions` | 获取会话列表 |
| GET | `/api/interview/sessions/{sessionId}` | 获取会话详情 |
| POST | `/api/interview/sessions/{sessionId}/answer` | 提交答案 |
| POST | `/api/interview/sessions/{sessionId}/end` | 结束面试 |

---

## 3. 前端期望的数据结构

### 3.1 InterviewSession

```typescript
interface InterviewSession {
  id: string;
  userId: string;
  type: 'TECHNICAL' | 'BEHAVIORAL' | 'CASE' | 'MIXED';
  status: 'IN_PROGRESS' | 'COMPLETED' | 'EXPIRED';
  targetPosition: string;
  targetCompany?: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  messages: InterviewMessage[];
  feedback?: InterviewFeedback;
  createdAt: string;
  completedAt?: string;
}

interface InterviewMessage {
  id: string;
  role: 'AI' | 'USER';
  content: string;
  timestamp: string;
  feedback?: MessageFeedback;  // AI 对用户答案的反馈
}

interface MessageFeedback {
  score: number;          // 0-100
  strengths: string[];
  improvements: string[];
  suggestedAnswer?: string;
}

interface InterviewFeedback {
  overallScore: number;   // 0-100
  summary: string;
  categoryScores: {
    communication: number;
    technicalDepth: number;
    problemSolving: number;
    confidence: number;
  };
  strengths: string[];
  areasToImprove: string[];
  recommendations: string[];
}
```

### 3.2 请求结构

```typescript
// 开始面试
interface StartInterviewRequest {
  type: 'TECHNICAL' | 'BEHAVIORAL' | 'CASE' | 'MIXED';
  targetPosition: string;
  targetCompany?: string;
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD';
  focusAreas?: string[];  // ["algorithms", "system-design", "leadership"]
}

// 提交答案
interface SubmitAnswerRequest {
  answer: string;
}
```

---

## 4. 详细实现

### 4.1 Controller

```java
@RestController
@RequestMapping("/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    /**
     * 开始面试会话
     * POST /api/interview/sessions
     */
    @PostMapping("/sessions")
    public ApiResponse<InterviewSessionDto> startSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StartInterviewRequest request) {
        InterviewSessionDto session = interviewService.startSession(
                principal.getId(), request);
        return ApiResponse.success(session);
    }

    /**
     * 获取会话列表
     * GET /api/interview/sessions
     */
    @GetMapping("/sessions")
    public ApiResponse<List<InterviewSessionDto>> getSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<InterviewSessionDto> sessions = interviewService.getSessions(
                principal.getId(), page, size);
        return ApiResponse.success(sessions);
    }

    /**
     * 获取会话详情
     * GET /api/interview/sessions/{sessionId}
     */
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<InterviewSessionDto> getSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        InterviewSessionDto session = interviewService.getSession(
                principal.getId(), sessionId);
        return ApiResponse.success(session);
    }

    /**
     * 提交答案
     * POST /api/interview/sessions/{sessionId}/answer
     */
    @PostMapping("/sessions/{sessionId}/answer")
    public ApiResponse<InterviewMessageDto> submitAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        InterviewMessageDto response = interviewService.submitAnswer(
                principal.getId(), sessionId, request);
        return ApiResponse.success(response);
    }

    /**
     * 结束面试
     * POST /api/interview/sessions/{sessionId}/end
     */
    @PostMapping("/sessions/{sessionId}/end")
    public ApiResponse<InterviewSessionDto> endSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        InterviewSessionDto session = interviewService.endSession(
                principal.getId(), sessionId);
        return ApiResponse.success(session);
    }
}
```

### 4.2 DTOs

```java
// InterviewSessionDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSessionDto {
    private String id;
    private String userId;
    private String type;          // TECHNICAL, BEHAVIORAL, CASE, MIXED
    private String status;        // IN_PROGRESS, COMPLETED, EXPIRED
    private String targetPosition;
    private String targetCompany;
    private String difficulty;
    private List<InterviewMessageDto> messages;
    private InterviewFeedbackDto feedback;
    private String createdAt;
    private String completedAt;

    public static InterviewSessionDto fromEntity(InterviewSession entity) {
        InterviewSessionDtoBuilder builder = InterviewSessionDto.builder()
                .id(entity.getId().toString())
                .userId(entity.getUser().getId().toString())
                .type(entity.getType().name())
                .status(entity.getStatus().name())
                .targetPosition(entity.getTargetPosition())
                .targetCompany(entity.getTargetCompany())
                .difficulty(entity.getDifficulty().name())
                .createdAt(entity.getCreatedAt().toString());

        if (entity.getCompletedAt() != null) {
            builder.completedAt(entity.getCompletedAt().toString());
        }

        // 解析消息
        builder.messages(parseMessages(entity.getMessagesJson()));
        
        // 解析反馈
        if (entity.getFeedbackJson() != null) {
            builder.feedback(parseFeedback(entity.getFeedbackJson()));
        }

        return builder.build();
    }

    private static List<InterviewMessageDto> parseMessages(String json) {
        if (json == null) return new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, 
                    new TypeReference<List<InterviewMessageDto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static InterviewFeedbackDto parseFeedback(String json) {
        if (json == null) return null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, InterviewFeedbackDto.class);
        } catch (Exception e) {
            return null;
        }
    }
}

// InterviewMessageDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewMessageDto {
    private String id;
    private String role;      // AI, USER
    private String content;
    private String timestamp;
    private MessageFeedbackDto feedback;
}

// MessageFeedbackDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageFeedbackDto {
    private Integer score;
    private List<String> strengths;
    private List<String> improvements;
    private String suggestedAnswer;
}

// InterviewFeedbackDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewFeedbackDto {
    private Integer overallScore;
    private String summary;
    private CategoryScores categoryScores;
    private List<String> strengths;
    private List<String> areasToImprove;
    private List<String> recommendations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryScores {
        private Integer communication;
        private Integer technicalDepth;
        private Integer problemSolving;
        private Integer confidence;
    }
}

// StartInterviewRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartInterviewRequest {
    @NotBlank(message = "Interview type is required")
    private String type;  // TECHNICAL, BEHAVIORAL, CASE, MIXED

    @NotBlank(message = "Target position is required")
    private String targetPosition;

    private String targetCompany;
    
    private String difficulty;  // EASY, MEDIUM, HARD
    
    private List<String> focusAreas;
}

// SubmitAnswerRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    @NotBlank(message = "Answer is required")
    private String answer;
}
```

### 4.3 Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final AiEngineClient aiEngineClient;
    private final ObjectMapper objectMapper;

    /**
     * 开始面试会话
     */
    @Transactional
    public InterviewSessionDto startSession(UUID userId, StartInterviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 获取用户资料以提供上下文
        ProfileDto profile = profileService.getOrCreateProfile(userId);

        // 创建会话
        InterviewSession session = InterviewSession.builder()
                .user(user)
                .type(InterviewType.valueOf(request.getType()))
                .status(InterviewStatus.IN_PROGRESS)
                .targetPosition(request.getTargetPosition())
                .targetCompany(request.getTargetCompany())
                .difficulty(request.getDifficulty() != null 
                        ? Difficulty.valueOf(request.getDifficulty()) 
                        : Difficulty.MEDIUM)
                .focusAreas(toJson(request.getFocusAreas()))
                .build();

        session = sessionRepository.save(session);

        // 生成第一个问题
        InterviewContext context = buildContext(session, profile);
        String firstQuestion = aiEngineClient.generateInterviewQuestion(context);

        // 添加第一条消息
        List<InterviewMessageDto> messages = new ArrayList<>();
        messages.add(InterviewMessageDto.builder()
                .id(UUID.randomUUID().toString())
                .role("AI")
                .content(firstQuestion)
                .timestamp(Instant.now().toString())
                .build());

        session.setMessagesJson(toJson(messages));
        session = sessionRepository.save(session);

        log.info("Started interview session {} for user: {}", session.getId(), userId);
        return InterviewSessionDto.fromEntity(session);
    }

    /**
     * 获取会话列表
     */
    @Transactional(readOnly = true)
    public List<InterviewSessionDto> getSessions(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return sessionRepository.findByUserId(userId, pageable)
                .stream()
                .map(InterviewSessionDto::fromEntity)
                .toList();
    }

    /**
     * 获取会话详情
     */
    @Transactional(readOnly = true)
    public InterviewSessionDto getSession(UUID userId, UUID sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // 验证归属
        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return InterviewSessionDto.fromEntity(session);
    }

    /**
     * 提交答案
     */
    @Transactional
    public InterviewMessageDto submitAnswer(UUID userId, UUID sessionId, 
            SubmitAnswerRequest request) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // 验证归属
        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 验证状态
        if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.SESSION_NOT_ACTIVE);
        }

        // 获取现有消息
        List<InterviewMessageDto> messages = parseMessages(session.getMessagesJson());

        // 添加用户答案
        InterviewMessageDto userMessage = InterviewMessageDto.builder()
                .id(UUID.randomUUID().toString())
                .role("USER")
                .content(request.getAnswer())
                .timestamp(Instant.now().toString())
                .build();
        messages.add(userMessage);

        // 调用 AI 评估答案并生成下一个问题
        InterviewContext context = buildContext(session, null);
        context.setConversationHistory(messages);
        context.setLastAnswer(request.getAnswer());

        AiInterviewResponse aiResponse = aiEngineClient.evaluateAndContinue(context);

        // 添加 AI 反馈到用户消息
        userMessage.setFeedback(MessageFeedbackDto.builder()
                .score(aiResponse.getAnswerScore())
                .strengths(aiResponse.getStrengths())
                .improvements(aiResponse.getImprovements())
                .suggestedAnswer(aiResponse.getSuggestedAnswer())
                .build());

        // 添加 AI 的下一个问题或结束消息
        InterviewMessageDto aiMessage = InterviewMessageDto.builder()
                .id(UUID.randomUUID().toString())
                .role("AI")
                .content(aiResponse.getNextQuestion())
                .timestamp(Instant.now().toString())
                .build();
        messages.add(aiMessage);

        // 更新会话
        session.setMessagesJson(toJson(messages));
        sessionRepository.save(session);

        log.info("Submitted answer for session: {}", sessionId);
        return aiMessage;
    }

    /**
     * 结束面试
     */
    @Transactional
    public InterviewSessionDto endSession(UUID userId, UUID sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // 验证归属
        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 验证状态
        if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ENDED);
        }

        // 生成最终反馈
        List<InterviewMessageDto> messages = parseMessages(session.getMessagesJson());
        InterviewContext context = buildContext(session, null);
        context.setConversationHistory(messages);

        InterviewFeedbackDto feedback = aiEngineClient.generateFinalFeedback(context);

        // 更新会话
        session.setStatus(InterviewStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        session.setFeedbackJson(toJson(feedback));
        session = sessionRepository.save(session);

        log.info("Ended interview session: {}", sessionId);
        return InterviewSessionDto.fromEntity(session);
    }

    /**
     * 构建 AI 上下文
     */
    private InterviewContext buildContext(InterviewSession session, ProfileDto profile) {
        return InterviewContext.builder()
                .sessionId(session.getId().toString())
                .type(session.getType().name())
                .targetPosition(session.getTargetPosition())
                .targetCompany(session.getTargetCompany())
                .difficulty(session.getDifficulty().name())
                .focusAreas(parseJsonArray(session.getFocusAreas()))
                .userExperiences(profile != null ? profile.getExperiences() : null)
                .userSkills(profile != null ? profile.getSkills() : null)
                .build();
    }

    private List<InterviewMessageDto> parseMessages(String json) {
        if (json == null) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, 
                    new TypeReference<List<InterviewMessageDto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> parseJsonArray(String json) {
        if (json == null) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
```

### 4.4 Entity

```java
@Entity
@Table(name = "interview_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status = InterviewStatus.IN_PROGRESS;

    @Column(name = "target_position", nullable = false)
    private String targetPosition;

    @Column(name = "target_company")
    private String targetCompany;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Column(name = "focus_areas", columnDefinition = "TEXT")
    private String focusAreas;  // JSON array

    @Column(name = "messages_json", columnDefinition = "TEXT")
    private String messagesJson;

    @Column(name = "feedback_json", columnDefinition = "TEXT")
    private String feedbackJson;

    @Column(name = "completed_at")
    private Instant completedAt;
}

public enum InterviewType {
    TECHNICAL,
    BEHAVIORAL,
    CASE,
    MIXED
}

public enum InterviewStatus {
    IN_PROGRESS,
    COMPLETED,
    EXPIRED
}

public enum Difficulty {
    EASY,
    MEDIUM,
    HARD
}
```

### 4.5 Repository

```java
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    Page<InterviewSession> findByUserId(UUID userId, Pageable pageable);

    List<InterviewSession> findByUserIdAndStatus(UUID userId, InterviewStatus status);

    @Query("SELECT COUNT(s) FROM InterviewSession s " +
           "WHERE s.user.id = :userId AND s.status = 'COMPLETED'")
    long countCompletedByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE InterviewSession s SET s.status = 'EXPIRED' " +
           "WHERE s.status = 'IN_PROGRESS' AND s.createdAt < :cutoff")
    int expireOldSessions(@Param("cutoff") Instant cutoff);
}
```

---

## 5. AI 引擎集成

### 5.1 Interview Context

```java
@Data
@Builder
public class InterviewContext {
    private String sessionId;
    private String type;
    private String targetPosition;
    private String targetCompany;
    private String difficulty;
    private List<String> focusAreas;
    private List<ExperienceDto> userExperiences;
    private List<SkillDto> userSkills;
    private List<InterviewMessageDto> conversationHistory;
    private String lastAnswer;
}

@Data
@Builder
public class AiInterviewResponse {
    private Integer answerScore;
    private List<String> strengths;
    private List<String> improvements;
    private String suggestedAnswer;
    private String nextQuestion;
}
```

### 5.2 AI Engine Prompt

```python
# ai-engine/src/agents/interview/workflow.py

INTERVIEW_QUESTION_PROMPT = """
You are an expert interviewer at {target_company} for {target_position} position.

Interview Type: {interview_type}
Difficulty: {difficulty}
Focus Areas: {focus_areas}

Candidate Background:
{user_background}

Conversation so far:
{conversation_history}

Generate the next interview question. The question should:
1. Be relevant to the target position
2. Match the specified difficulty level
3. Build on previous answers if applicable
4. Test the specified focus areas

Output just the question, no extra text.
"""

ANSWER_EVALUATION_PROMPT = """
Evaluate the candidate's answer to the interview question.

Question: {question}
Candidate's Answer: {answer}

Target Position: {target_position}
Difficulty: {difficulty}

Evaluate and provide:
1. Score (0-100)
2. Strengths (list)
3. Areas for improvement (list)
4. A suggested better answer

Output in JSON format:
{{
    "score": 75,
    "strengths": ["...", "..."],
    "improvements": ["...", "..."],
    "suggestedAnswer": "..."
}}
"""

FINAL_FEEDBACK_PROMPT = """
Generate a comprehensive feedback report for this interview session.

Interview Details:
- Position: {target_position}
- Company: {target_company}
- Type: {interview_type}
- Difficulty: {difficulty}

Complete Conversation:
{conversation_history}

Generate a detailed feedback report in JSON format:
{{
    "overallScore": 75,
    "summary": "Overall performance summary...",
    "categoryScores": {{
        "communication": 80,
        "technicalDepth": 70,
        "problemSolving": 75,
        "confidence": 78
    }},
    "strengths": ["...", "..."],
    "areasToImprove": ["...", "..."],
    "recommendations": ["...", "..."]
}}
"""
```

---

## 6. 错误码

| 错误码 | HTTP 状态 | 描述 |
|--------|----------|------|
| 60001 | 404 | Session not found |
| 60002 | 400 | Invalid interview type |
| 60003 | 400 | Session not active |
| 60004 | 400 | Session already ended |
| 60005 | 403 | Forbidden |
| 60006 | 503 | AI service unavailable |

---

## 7. 常见 Bug 及修复

### Bug 1: messages 解析失败导致空数组
**问题**: JSON 格式变化导致解析失败

**修复**: 使用 try-catch 并返回空数组
```java
private List<InterviewMessageDto> parseMessages(String json) {
    if (json == null || json.isEmpty()) return new ArrayList<>();
    try {
        return objectMapper.readValue(json, ...);
    } catch (Exception e) {
        log.warn("Failed to parse messages", e);
        return new ArrayList<>();
    }
}
```

### Bug 2: 会话过期但状态未更新
**问题**: 长时间未操作的会话仍显示 IN_PROGRESS

**修复**: 使用定时任务清理
```java
@Scheduled(fixedRate = 3600000) // 每小时
public void expireOldSessions() {
    Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
    int count = sessionRepository.expireOldSessions(cutoff);
    log.info("Expired {} old sessions", count);
}
```

### Bug 3: 反馈为 null 导致前端报错
**问题**: 未结束的会话 `feedback` 为 null

**修复**: 前端处理 null，后端也可返回空对象

### Bug 4: 答案太长导致超时
**问题**: 用户提交很长的答案，AI 处理超时

**修复**: 限制答案长度
```java
@Size(max = 5000, message = "Answer too long")
private String answer;
```

---

## 8. 测试用例

```java
@SpringBootTest
@AutoConfigureMockMvc
class InterviewControllerTest {

    @Test
    @WithMockUser(userId = "test-user-id")
    void startSession_Success() throws Exception {
        StartInterviewRequest request = new StartInterviewRequest();
        request.setType("TECHNICAL");
        request.setTargetPosition("Software Engineer");
        request.setDifficulty("MEDIUM");

        mockMvc.perform(post("/api/interview/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.messages").isArray())
                .andExpect(jsonPath("$.data.messages[0].role").value("AI"));
    }

    @Test
    @WithMockUser(userId = "test-user-id")
    void submitAnswer_Success() throws Exception {
        // 先创建会话
        UUID sessionId = createTestSession();

        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setAnswer("My answer to the question...");

        mockMvc.perform(post("/api/interview/sessions/{sessionId}/answer", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("AI"));
    }
}
```
