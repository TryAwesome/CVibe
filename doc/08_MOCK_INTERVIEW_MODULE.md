# Mock Interview 模块详细设计

> 视频/音频模拟面试模块，提供更真实的面试体验

---

## 1. 模块结构

```
biz/mockinterview/
├── controller/
│   └── MockInterviewController.java
├── service/
│   └── MockInterviewService.java
├── repository/
│   └── MockInterviewSessionRepository.java
├── entity/
│   └── MockInterviewSession.java
└── dto/
    ├── MockInterviewSessionDto.java
    ├── MockInterviewSettingsDto.java
    └── MockInterviewQuestionDto.java
```

---

## 2. API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/mock-interview/sessions` | 创建模拟面试会话 |
| GET | `/api/mock-interview/sessions` | 获取会话列表 |
| GET | `/api/mock-interview/sessions/{sessionId}` | 获取会话详情 |
| GET | `/api/mock-interview/sessions/{sessionId}/questions/{index}` | 获取当前问题 |
| POST | `/api/mock-interview/sessions/{sessionId}/submit` | 提交答案（录音/录像）|
| POST | `/api/mock-interview/sessions/{sessionId}/complete` | 完成面试 |
| GET | `/api/mock-interview/sessions/{sessionId}/feedback` | 获取反馈 |

---

## 3. 前端期望的数据结构

### 3.1 MockInterviewSession

```typescript
interface MockInterviewSession {
  id: string;
  userId: string;
  type: 'VIDEO' | 'AUDIO' | 'TEXT';
  status: 'SETUP' | 'IN_PROGRESS' | 'ANALYZING' | 'COMPLETED';
  settings: MockInterviewSettings;
  questions: MockInterviewQuestion[];
  feedback?: MockInterviewFeedback;
  createdAt: string;
  completedAt?: string;
}

interface MockInterviewSettings {
  targetPosition: string;
  targetCompany?: string;
  interviewType: 'TECHNICAL' | 'BEHAVIORAL' | 'MIXED';
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  duration: number;           // minutes
  questionCount: number;
  timePerQuestion: number;    // seconds
  allowRetake: boolean;
}

interface MockInterviewQuestion {
  index: number;
  question: string;
  category: string;           // "behavioral", "technical", "situational"
  timeLimit: number;          // seconds
  response?: {
    mediaUrl?: string;        // 录音/录像 URL
    transcript?: string;      // 语音转文字
    submittedAt: string;
  };
  feedback?: QuestionFeedback;
}

interface QuestionFeedback {
  score: number;              // 0-100
  contentAnalysis: {
    relevance: number;
    depth: number;
    structure: number;
  };
  deliveryAnalysis?: {        // 仅视频/音频
    pace: number;
    clarity: number;
    confidence: number;
    fillerWords: string[];
    fillerWordCount: number;
  };
  transcript?: string;
  improvements: string[];
  suggestedAnswer: string;
}

interface MockInterviewFeedback {
  overallScore: number;
  summary: string;
  categoryBreakdown: {
    content: number;
    communication: number;
    confidence: number;
    structure: number;
  };
  strengths: string[];
  areasToImprove: string[];
  detailedFeedback: QuestionFeedback[];
  recommendations: string[];
}
```

### 3.2 请求结构

```typescript
// 创建会话
interface CreateMockInterviewRequest {
  type: 'VIDEO' | 'AUDIO' | 'TEXT';
  settings: MockInterviewSettings;
}

// 提交答案
interface SubmitAnswerRequest {
  questionIndex: number;
  mediaFile?: File;           // 视频或音频文件
  textAnswer?: string;        // 文字答案（TEXT 模式）
}
```

---

## 4. 详细实现

### 4.1 Controller

```java
@RestController
@RequestMapping("/mock-interview")
@RequiredArgsConstructor
public class MockInterviewController {

    private final MockInterviewService mockInterviewService;

    /**
     * 创建模拟面试会话
     * POST /api/mock-interview/sessions
     */
    @PostMapping("/sessions")
    public ApiResponse<MockInterviewSessionDto> createSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMockInterviewRequest request) {
        MockInterviewSessionDto session = mockInterviewService.createSession(
                principal.getId(), request);
        return ApiResponse.success(session);
    }

    /**
     * 获取会话列表
     * GET /api/mock-interview/sessions
     */
    @GetMapping("/sessions")
    public ApiResponse<List<MockInterviewSessionDto>> getSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<MockInterviewSessionDto> sessions = mockInterviewService.getSessions(
                principal.getId(), page, size);
        return ApiResponse.success(sessions);
    }

    /**
     * 获取会话详情
     * GET /api/mock-interview/sessions/{sessionId}
     */
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<MockInterviewSessionDto> getSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        MockInterviewSessionDto session = mockInterviewService.getSession(
                principal.getId(), sessionId);
        return ApiResponse.success(session);
    }

    /**
     * 获取当前问题
     * GET /api/mock-interview/sessions/{sessionId}/questions/{index}
     */
    @GetMapping("/sessions/{sessionId}/questions/{index}")
    public ApiResponse<MockInterviewQuestionDto> getQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @PathVariable int index) {
        MockInterviewQuestionDto question = mockInterviewService.getQuestion(
                principal.getId(), sessionId, index);
        return ApiResponse.success(question);
    }

    /**
     * 提交答案（支持文件上传）
     * POST /api/mock-interview/sessions/{sessionId}/submit
     */
    @PostMapping(value = "/sessions/{sessionId}/submit", 
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MockInterviewQuestionDto> submitAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @RequestParam("questionIndex") int questionIndex,
            @RequestParam(value = "mediaFile", required = false) MultipartFile mediaFile,
            @RequestParam(value = "textAnswer", required = false) String textAnswer) {
        MockInterviewQuestionDto question = mockInterviewService.submitAnswer(
                principal.getId(), sessionId, questionIndex, mediaFile, textAnswer);
        return ApiResponse.success(question);
    }

    /**
     * 完成面试
     * POST /api/mock-interview/sessions/{sessionId}/complete
     */
    @PostMapping("/sessions/{sessionId}/complete")
    public ApiResponse<MockInterviewSessionDto> completeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        MockInterviewSessionDto session = mockInterviewService.completeSession(
                principal.getId(), sessionId);
        return ApiResponse.success(session);
    }

    /**
     * 获取反馈
     * GET /api/mock-interview/sessions/{sessionId}/feedback
     */
    @GetMapping("/sessions/{sessionId}/feedback")
    public ApiResponse<MockInterviewFeedbackDto> getFeedback(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        MockInterviewFeedbackDto feedback = mockInterviewService.getFeedback(
                principal.getId(), sessionId);
        return ApiResponse.success(feedback);
    }
}
```

### 4.2 DTOs

```java
// MockInterviewSessionDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewSessionDto {
    private String id;
    private String userId;
    private String type;          // VIDEO, AUDIO, TEXT
    private String status;        // SETUP, IN_PROGRESS, ANALYZING, COMPLETED
    private MockInterviewSettingsDto settings;
    private List<MockInterviewQuestionDto> questions;
    private MockInterviewFeedbackDto feedback;
    private String createdAt;
    private String completedAt;

    public static MockInterviewSessionDto fromEntity(MockInterviewSession entity) {
        MockInterviewSessionDtoBuilder builder = MockInterviewSessionDto.builder()
                .id(entity.getId().toString())
                .userId(entity.getUser().getId().toString())
                .type(entity.getType().name())
                .status(entity.getStatus().name())
                .settings(parseSettings(entity.getSettingsJson()))
                .questions(parseQuestions(entity.getQuestionsJson()))
                .createdAt(entity.getCreatedAt().toString());

        if (entity.getCompletedAt() != null) {
            builder.completedAt(entity.getCompletedAt().toString());
        }

        if (entity.getFeedbackJson() != null) {
            builder.feedback(parseFeedback(entity.getFeedbackJson()));
        }

        return builder.build();
    }

    private static MockInterviewSettingsDto parseSettings(String json) {
        if (json == null) return null;
        try {
            return new ObjectMapper().readValue(json, MockInterviewSettingsDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<MockInterviewQuestionDto> parseQuestions(String json) {
        if (json == null) return new ArrayList<>();
        try {
            return new ObjectMapper().readValue(json, 
                    new TypeReference<List<MockInterviewQuestionDto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static MockInterviewFeedbackDto parseFeedback(String json) {
        if (json == null) return null;
        try {
            return new ObjectMapper().readValue(json, MockInterviewFeedbackDto.class);
        } catch (Exception e) {
            return null;
        }
    }
}

// MockInterviewSettingsDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewSettingsDto {
    @NotBlank(message = "Target position is required")
    private String targetPosition;
    
    private String targetCompany;
    
    @NotBlank(message = "Interview type is required")
    private String interviewType;  // TECHNICAL, BEHAVIORAL, MIXED
    
    private String difficulty = "MEDIUM";
    
    private Integer duration = 30;           // minutes
    
    private Integer questionCount = 5;
    
    private Integer timePerQuestion = 120;   // seconds
    
    private Boolean allowRetake = true;
}

// MockInterviewQuestionDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewQuestionDto {
    private Integer index;
    private String question;
    private String category;
    private Integer timeLimit;
    private ResponseDto response;
    private QuestionFeedbackDto feedback;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseDto {
        private String mediaUrl;
        private String transcript;
        private String submittedAt;
    }
}

// QuestionFeedbackDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionFeedbackDto {
    private Integer score;
    private ContentAnalysis contentAnalysis;
    private DeliveryAnalysis deliveryAnalysis;
    private String transcript;
    private List<String> improvements;
    private String suggestedAnswer;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentAnalysis {
        private Integer relevance;
        private Integer depth;
        private Integer structure;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryAnalysis {
        private Integer pace;
        private Integer clarity;
        private Integer confidence;
        private List<String> fillerWords;
        private Integer fillerWordCount;
    }
}

// MockInterviewFeedbackDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewFeedbackDto {
    private Integer overallScore;
    private String summary;
    private CategoryBreakdown categoryBreakdown;
    private List<String> strengths;
    private List<String> areasToImprove;
    private List<QuestionFeedbackDto> detailedFeedback;
    private List<String> recommendations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBreakdown {
        private Integer content;
        private Integer communication;
        private Integer confidence;
        private Integer structure;
    }
}

// CreateMockInterviewRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMockInterviewRequest {
    @NotBlank(message = "Type is required")
    private String type;  // VIDEO, AUDIO, TEXT

    @NotNull(message = "Settings are required")
    @Valid
    private MockInterviewSettingsDto settings;
}
```

### 4.3 Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class MockInterviewService {

    private final MockInterviewSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final MediaStorageService mediaStorageService;
    private final AiEngineClient aiEngineClient;
    private final SpeechToTextService speechToTextService;
    private final ObjectMapper objectMapper;

    /**
     * 创建模拟面试会话
     */
    @Transactional
    public MockInterviewSessionDto createSession(UUID userId, 
            CreateMockInterviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 获取用户资料
        ProfileDto profile = profileService.getOrCreateProfile(userId);

        // 生成问题
        List<MockInterviewQuestionDto> questions = aiEngineClient.generateMockInterviewQuestions(
                request.getSettings().getTargetPosition(),
                request.getSettings().getInterviewType(),
                request.getSettings().getDifficulty(),
                request.getSettings().getQuestionCount(),
                profile
        );

        // 设置问题索引和时间限制
        int timePerQuestion = request.getSettings().getTimePerQuestion();
        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).setIndex(i);
            questions.get(i).setTimeLimit(timePerQuestion);
        }

        // 创建会话
        MockInterviewSession session = MockInterviewSession.builder()
                .user(user)
                .type(MockInterviewType.valueOf(request.getType()))
                .status(MockInterviewStatus.SETUP)
                .settingsJson(toJson(request.getSettings()))
                .questionsJson(toJson(questions))
                .build();

        session = sessionRepository.save(session);
        log.info("Created mock interview session {} for user: {}", session.getId(), userId);

        return MockInterviewSessionDto.fromEntity(session);
    }

    /**
     * 获取会话列表
     */
    @Transactional(readOnly = true)
    public List<MockInterviewSessionDto> getSessions(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return sessionRepository.findByUserId(userId, pageable)
                .stream()
                .map(MockInterviewSessionDto::fromEntity)
                .toList();
    }

    /**
     * 获取会话详情
     */
    @Transactional(readOnly = true)
    public MockInterviewSessionDto getSession(UUID userId, UUID sessionId) {
        MockInterviewSession session = getAndValidateSession(userId, sessionId);
        return MockInterviewSessionDto.fromEntity(session);
    }

    /**
     * 获取当前问题
     */
    @Transactional
    public MockInterviewQuestionDto getQuestion(UUID userId, UUID sessionId, int index) {
        MockInterviewSession session = getAndValidateSession(userId, sessionId);

        // 开始面试时更新状态
        if (session.getStatus() == MockInterviewStatus.SETUP) {
            session.setStatus(MockInterviewStatus.IN_PROGRESS);
            sessionRepository.save(session);
        }

        List<MockInterviewQuestionDto> questions = parseQuestions(session.getQuestionsJson());
        
        if (index < 0 || index >= questions.size()) {
            throw new BusinessException(ErrorCode.QUESTION_INDEX_OUT_OF_RANGE);
        }

        return questions.get(index);
    }

    /**
     * 提交答案
     */
    @Transactional
    public MockInterviewQuestionDto submitAnswer(UUID userId, UUID sessionId,
            int questionIndex, MultipartFile mediaFile, String textAnswer) {
        
        MockInterviewSession session = getAndValidateSession(userId, sessionId);

        if (session.getStatus() != MockInterviewStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.SESSION_NOT_ACTIVE);
        }

        List<MockInterviewQuestionDto> questions = parseQuestions(session.getQuestionsJson());
        
        if (questionIndex < 0 || questionIndex >= questions.size()) {
            throw new BusinessException(ErrorCode.QUESTION_INDEX_OUT_OF_RANGE);
        }

        MockInterviewQuestionDto question = questions.get(questionIndex);
        String mediaUrl = null;
        String transcript = null;

        // 处理媒体文件
        if (mediaFile != null && !mediaFile.isEmpty()) {
            // 上传到 MinIO
            mediaUrl = mediaStorageService.uploadMedia(
                    userId, sessionId, questionIndex, mediaFile);

            // 语音转文字
            transcript = speechToTextService.transcribe(mediaFile);
        } else if (textAnswer != null) {
            transcript = textAnswer;
        }

        // 更新问题的响应
        question.setResponse(MockInterviewQuestionDto.ResponseDto.builder()
                .mediaUrl(mediaUrl)
                .transcript(transcript)
                .submittedAt(Instant.now().toString())
                .build());

        // 实时评估答案（可选）
        QuestionFeedbackDto feedback = aiEngineClient.evaluateMockInterviewAnswer(
                question.getQuestion(),
                transcript,
                session.getType() == MockInterviewType.TEXT ? null : mediaUrl
        );
        question.setFeedback(feedback);

        // 更新会话
        session.setQuestionsJson(toJson(questions));
        sessionRepository.save(session);

        log.info("Submitted answer for session: {}, question: {}", sessionId, questionIndex);
        return question;
    }

    /**
     * 完成面试
     */
    @Transactional
    public MockInterviewSessionDto completeSession(UUID userId, UUID sessionId) {
        MockInterviewSession session = getAndValidateSession(userId, sessionId);

        if (session.getStatus() == MockInterviewStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }

        // 更新状态为分析中
        session.setStatus(MockInterviewStatus.ANALYZING);
        sessionRepository.save(session);

        // 生成综合反馈
        List<MockInterviewQuestionDto> questions = parseQuestions(session.getQuestionsJson());
        MockInterviewSettingsDto settings = parseSettings(session.getSettingsJson());

        MockInterviewFeedbackDto feedback = aiEngineClient.generateMockInterviewFeedback(
                settings, questions);

        // 更新会话
        session.setStatus(MockInterviewStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        session.setFeedbackJson(toJson(feedback));
        session = sessionRepository.save(session);

        log.info("Completed mock interview session: {}", sessionId);
        return MockInterviewSessionDto.fromEntity(session);
    }

    /**
     * 获取反馈
     */
    @Transactional(readOnly = true)
    public MockInterviewFeedbackDto getFeedback(UUID userId, UUID sessionId) {
        MockInterviewSession session = getAndValidateSession(userId, sessionId);

        if (session.getStatus() != MockInterviewStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.FEEDBACK_NOT_READY);
        }

        return parseFeedback(session.getFeedbackJson());
    }

    // ==================== Helper ====================

    private MockInterviewSession getAndValidateSession(UUID userId, UUID sessionId) {
        MockInterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return session;
    }

    private List<MockInterviewQuestionDto> parseQuestions(String json) {
        if (json == null) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, 
                    new TypeReference<List<MockInterviewQuestionDto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private MockInterviewSettingsDto parseSettings(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, MockInterviewSettingsDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    private MockInterviewFeedbackDto parseFeedback(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, MockInterviewFeedbackDto.class);
        } catch (Exception e) {
            return null;
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

### 4.4 语音转文字服务

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechToTextService {

    private final AiEngineClient aiEngineClient;

    /**
     * 将音频/视频转换为文字
     */
    public String transcribe(MultipartFile file) {
        try {
            return aiEngineClient.speechToText(file.getBytes(), 
                    getMediaType(file.getContentType()));
        } catch (Exception e) {
            log.error("Speech to text failed", e);
            return null;  // 转换失败不阻塞主流程
        }
    }

    private String getMediaType(String contentType) {
        if (contentType == null) return "audio";
        if (contentType.startsWith("video")) return "video";
        return "audio";
    }
}
```

### 4.5 Entity

```java
@Entity
@Table(name = "mock_interview_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MockInterviewType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MockInterviewStatus status = MockInterviewStatus.SETUP;

    @Column(name = "settings_json", columnDefinition = "TEXT")
    private String settingsJson;

    @Column(name = "questions_json", columnDefinition = "TEXT")
    private String questionsJson;

    @Column(name = "feedback_json", columnDefinition = "TEXT")
    private String feedbackJson;

    @Column(name = "completed_at")
    private Instant completedAt;
}

public enum MockInterviewType {
    VIDEO,
    AUDIO,
    TEXT
}

public enum MockInterviewStatus {
    SETUP,
    IN_PROGRESS,
    ANALYZING,
    COMPLETED
}
```

---

## 5. 错误码

| 错误码 | HTTP 状态 | 描述 |
|--------|----------|------|
| 70001 | 404 | Session not found |
| 70002 | 400 | Session not active |
| 70003 | 400 | Session already completed |
| 70004 | 400 | Question index out of range |
| 70005 | 400 | Feedback not ready |
| 70006 | 400 | Invalid media file |
| 70007 | 500 | Speech to text failed |

---

## 6. 常见 Bug 及修复

### Bug 1: 大文件上传超时
**问题**: 视频文件过大导致上传超时

**修复**: 配置上传限制
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

### Bug 2: 语音转文字失败导致整体失败
**问题**: STT 服务不可用时提交失败

**修复**: STT 失败时继续，transcript 为 null

### Bug 3: 反馈生成耗时长
**问题**: 用户等待反馈时间过长

**修复**: 使用异步处理 + 轮询
```java
// 返回 ANALYZING 状态，前端轮询直到 COMPLETED
session.setStatus(MockInterviewStatus.ANALYZING);
CompletableFuture.runAsync(() -> generateFeedbackAsync(sessionId));
```

### Bug 4: 问题顺序混乱
**问题**: 问题索引不一致

**修复**: 确保设置正确的索引
```java
for (int i = 0; i < questions.size(); i++) {
    questions.get(i).setIndex(i);
}
```

---

## 7. 测试用例

```java
@SpringBootTest
@AutoConfigureMockMvc
class MockInterviewControllerTest {

    @Test
    @WithMockUser(userId = "test-user-id")
    void createSession_Success() throws Exception {
        CreateMockInterviewRequest request = new CreateMockInterviewRequest();
        request.setType("AUDIO");
        
        MockInterviewSettingsDto settings = new MockInterviewSettingsDto();
        settings.setTargetPosition("Software Engineer");
        settings.setInterviewType("TECHNICAL");
        settings.setQuestionCount(5);
        request.setSettings(settings);

        mockMvc.perform(post("/api/mock-interview/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SETUP"))
                .andExpect(jsonPath("$.data.questions").isArray())
                .andExpect(jsonPath("$.data.questions.length()").value(5));
    }

    @Test
    @WithMockUser(userId = "test-user-id")
    void submitAnswer_TextMode() throws Exception {
        UUID sessionId = createTestSession();

        mockMvc.perform(multipart("/api/mock-interview/sessions/{sessionId}/submit", sessionId)
                .param("questionIndex", "0")
                .param("textAnswer", "My answer to the question..."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.response.transcript").exists());
    }
}
```
