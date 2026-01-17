# Settings 模块详细设计

> 用户设置模块，包括密码修改和 AI 配置

---

## 1. 模块结构

```
biz/settings/
├── controller/
│   └── SettingsController.java
├── service/
│   └── SettingsService.java
├── repository/
│   └── UserAiConfigRepository.java
├── entity/
│   └── UserAiConfig.java
└── dto/
    ├── ChangePasswordRequest.java
    └── AiConfigDto.java
```

---

## 2. API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| PUT | `/api/settings/password` | 修改密码 |
| GET | `/api/settings/ai-config` | 获取 AI 配置 |
| PUT | `/api/settings/ai-config` | 更新 AI 配置 |

---

## 3. 前端期望的数据结构

### 3.1 修改密码请求

```typescript
interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
```

### 3.2 AI 配置

```typescript
interface AiConfig {
  language: 'zh' | 'en';
  responseStyle: 'concise' | 'detailed' | 'balanced';
  interviewDifficulty: 'easy' | 'medium' | 'hard';
  focusAreas: string[];        // ["algorithms", "system-design", "behavioral"]
  customInstructions?: string; // 自定义 AI 指令
}
```

---

## 4. 详细实现

### 4.1 Controller

```java
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * 修改密码
     * PUT /api/settings/password
     */
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        settingsService.changePassword(principal.getId(), request);
        return ApiResponse.success();
    }

    /**
     * 获取 AI 配置
     * GET /api/settings/ai-config
     */
    @GetMapping("/ai-config")
    public ApiResponse<AiConfigDto> getAiConfig(
            @AuthenticationPrincipal UserPrincipal principal) {
        AiConfigDto config = settingsService.getAiConfig(principal.getId());
        return ApiResponse.success(config);
    }

    /**
     * 更新 AI 配置
     * PUT /api/settings/ai-config
     */
    @PutMapping("/ai-config")
    public ApiResponse<AiConfigDto> updateAiConfig(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AiConfigDto request) {
        AiConfigDto config = settingsService.updateAiConfig(principal.getId(), request);
        return ApiResponse.success(config);
    }
}
```

### 4.2 DTOs

```java
// ChangePasswordRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    
    @NotBlank(message = "Current password is required")
    private String currentPassword;
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Password must contain at least one uppercase, one lowercase, and one digit")
    private String newPassword;
}

// AiConfigDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfigDto {
    
    @NotBlank(message = "Language is required")
    private String language;  // zh, en
    
    @NotBlank(message = "Response style is required")
    private String responseStyle;  // concise, detailed, balanced
    
    private String interviewDifficulty;  // easy, medium, hard
    
    private List<String> focusAreas;
    
    @Size(max = 1000, message = "Custom instructions too long")
    private String customInstructions;

    public static AiConfigDto fromEntity(UserAiConfig entity) {
        return AiConfigDto.builder()
                .language(entity.getLanguage())
                .responseStyle(entity.getResponseStyle())
                .interviewDifficulty(entity.getInterviewDifficulty())
                .focusAreas(parseJsonArray(entity.getFocusAreas()))
                .customInstructions(entity.getCustomInstructions())
                .build();
    }

    /**
     * 默认配置
     */
    public static AiConfigDto defaultConfig() {
        return AiConfigDto.builder()
                .language("zh")
                .responseStyle("balanced")
                .interviewDifficulty("medium")
                .focusAreas(List.of())
                .build();
    }

    private static List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) return List.of();
        try {
            return new ObjectMapper().readValue(json, 
                    new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
```

### 4.3 Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserRepository userRepository;
    private final UserAiConfigRepository aiConfigRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 验证当前密码
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 不允许新密码与旧密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    /**
     * 获取 AI 配置
     */
    @Transactional(readOnly = true)
    public AiConfigDto getAiConfig(UUID userId) {
        return aiConfigRepository.findByUserId(userId)
                .map(AiConfigDto::fromEntity)
                .orElse(AiConfigDto.defaultConfig());
    }

    /**
     * 更新 AI 配置
     */
    @Transactional
    public AiConfigDto updateAiConfig(UUID userId, AiConfigDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserAiConfig config = aiConfigRepository.findByUserId(userId)
                .orElseGet(() -> UserAiConfig.builder().user(user).build());

        // 验证并设置语言
        if (!Set.of("zh", "en").contains(request.getLanguage())) {
            throw new BusinessException(ErrorCode.INVALID_LANGUAGE);
        }
        config.setLanguage(request.getLanguage());

        // 验证并设置响应风格
        if (!Set.of("concise", "detailed", "balanced").contains(request.getResponseStyle())) {
            throw new BusinessException(ErrorCode.INVALID_RESPONSE_STYLE);
        }
        config.setResponseStyle(request.getResponseStyle());

        // 设置面试难度
        if (request.getInterviewDifficulty() != null) {
            if (!Set.of("easy", "medium", "hard").contains(request.getInterviewDifficulty())) {
                throw new BusinessException(ErrorCode.INVALID_DIFFICULTY);
            }
            config.setInterviewDifficulty(request.getInterviewDifficulty());
        }

        // 设置关注领域
        if (request.getFocusAreas() != null) {
            config.setFocusAreas(toJson(request.getFocusAreas()));
        }

        // 设置自定义指令
        config.setCustomInstructions(request.getCustomInstructions());

        config = aiConfigRepository.save(config);
        log.info("Updated AI config for user: {}", userId);

        return AiConfigDto.fromEntity(config);
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
@Table(name = "user_ai_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAiConfig extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 10)
    private String language = "zh";

    @Column(name = "response_style", nullable = false, length = 20)
    private String responseStyle = "balanced";

    @Column(name = "interview_difficulty", length = 10)
    private String interviewDifficulty = "medium";

    @Column(name = "focus_areas", columnDefinition = "TEXT")
    private String focusAreas;  // JSON array

    @Column(name = "custom_instructions", length = 1000)
    private String customInstructions;
}
```

### 4.5 Repository

```java
public interface UserAiConfigRepository extends JpaRepository<UserAiConfig, UUID> {

    Optional<UserAiConfig> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
```

---

## 5. AI 配置使用

AI 引擎在调用时需要读取用户的 AI 配置：

```java
@Service
@RequiredArgsConstructor
public class AiContextBuilder {

    private final SettingsService settingsService;

    /**
     * 构建 AI 调用上下文
     */
    public AiContext buildContext(UUID userId) {
        AiConfigDto config = settingsService.getAiConfig(userId);

        return AiContext.builder()
                .language(config.getLanguage())
                .responseStyle(config.getResponseStyle())
                .difficulty(config.getInterviewDifficulty())
                .focusAreas(config.getFocusAreas())
                .customInstructions(config.getCustomInstructions())
                .build();
    }
}
```

AI 引擎端处理：

```python
# ai-engine/src/utils/context.py

def build_system_prompt(context: dict) -> str:
    language = context.get("language", "zh")
    style = context.get("responseStyle", "balanced")
    custom = context.get("customInstructions", "")
    
    base_prompt = ""
    if language == "zh":
        base_prompt = "请用中文回复。"
    else:
        base_prompt = "Please respond in English."
    
    if style == "concise":
        base_prompt += "回复要简洁明了，突出重点。"
    elif style == "detailed":
        base_prompt += "回复要详细全面，包含具体例子。"
    
    if custom:
        base_prompt += f"\n额外指令：{custom}"
    
    return base_prompt
```

---

## 6. 错误码

| 错误码 | HTTP 状态 | 描述 |
|--------|----------|------|
| 120001 | 400 | Invalid current password |
| 120002 | 400 | New password same as old |
| 120003 | 400 | Invalid language |
| 120004 | 400 | Invalid response style |
| 120005 | 400 | Invalid difficulty |

---

## 7. 常见 Bug 及修复

### Bug 1: 配置不存在时报错
**问题**: 新用户没有 AI 配置记录

**修复**: 返回默认配置
```java
return aiConfigRepository.findByUserId(userId)
        .map(AiConfigDto::fromEntity)
        .orElse(AiConfigDto.defaultConfig());
```

### Bug 2: 密码修改后 Token 仍有效
**问题**: 安全问题，旧 Token 应该失效

**修复**: 在 User 中记录密码修改时间，Token 验证时检查
```java
@Column(name = "password_changed_at")
private Instant passwordChangedAt;

// 在 JWT 验证时
if (user.getPasswordChangedAt() != null && 
    tokenIssuedAt.isBefore(user.getPasswordChangedAt())) {
    throw new BusinessException(ErrorCode.TOKEN_INVALID);
}
```

### Bug 3: focusAreas 为 null
**问题**: 未设置时返回 null 而非空数组

**修复**: 使用空数组
```java
private static List<String> parseJsonArray(String json) {
    if (json == null || json.isEmpty()) return List.of();
    // ...
}
```

---

## 8. 测试用例

```java
@SpringBootTest
@AutoConfigureMockMvc
class SettingsControllerTest {

    @Test
    @WithMockUser(userId = "test-user-id")
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPassword123");
        request.setNewPassword("NewPassword456");

        mockMvc.perform(put("/api/settings/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(userId = "test-user-id")
    void changePassword_InvalidCurrent() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("WrongPassword");
        request.setNewPassword("NewPassword456");

        mockMvc.perform(put("/api/settings/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(120001));
    }

    @Test
    @WithMockUser(userId = "test-user-id")
    void updateAiConfig_Success() throws Exception {
        AiConfigDto request = new AiConfigDto();
        request.setLanguage("en");
        request.setResponseStyle("detailed");
        request.setInterviewDifficulty("hard");
        request.setFocusAreas(List.of("algorithms", "system-design"));

        mockMvc.perform(put("/api/settings/ai-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.language").value("en"));
    }

    @Test
    @WithMockUser(userId = "test-user-id")
    void getAiConfig_DefaultForNewUser() throws Exception {
        mockMvc.perform(get("/api/settings/ai-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.language").value("zh"))
                .andExpect(jsonPath("$.data.responseStyle").value("balanced"));
    }
}
```
