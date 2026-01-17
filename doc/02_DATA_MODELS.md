# 数据模型设计

> 本文档定义所有数据库实体和 DTO 的设计规范。

---

## 1. 实体命名规范

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| Entity | 单数名词 | `User`, `Resume`, `Job` |
| DTO | 名词 + Dto/Request/Response | `UserDto`, `LoginRequest`, `AuthResponse` |
| 表名 | 复数蛇形 | `users`, `resume_history`, `job_matches` |
| 字段 | 蛇形 | `created_at`, `is_primary` |

---

## 2. 核心实体

### 2.1 User (用户)

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;  // 可为空（Google 用户）

    @Column(name = "google_sub", unique = true)
    private String googleSub;  // Google OAuth ID

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.ROLE_USER;

    @Column(nullable = false)
    private Boolean enabled = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}

public enum UserRole {
    ROLE_USER,
    ROLE_ADMIN
}
```

**前端期望的 User 字段:**
```typescript
interface User {
  id: string;
  email: string;
  nickname?: string;        // 注意：前端用 nickname，不是 fullName
  role: string;
  hasPassword: boolean;     // 需要计算：passwordHash != null
  createdAt: string;
  googleUser: boolean;      // 需要计算：googleSub != null
}
```

---

### 2.2 UserProfile (用户资料)

```java
@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "headline", length = 200)
    private String headline;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "location", length = 100)
    private String location;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startDate DESC")
    private List<ProfileExperience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProfileSkill> skills = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
```

---

### 2.3 ProfileExperience (工作经历)

```java
@Entity
@Table(name = "profile_experiences")
public class ProfileExperience {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    @Column(name = "company", nullable = false, length = 100)
    private String company;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "employment_type", length = 50)
    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_current")
    private Boolean isCurrent = false;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // 存储为 JSON 字符串: ["Achievement 1", "Achievement 2"]
    @Column(name = "achievements", columnDefinition = "TEXT")
    private String achievements;

    // 存储为 JSON 字符串: ["Java", "Python"]
    @Column(name = "technologies", columnDefinition = "TEXT")
    private String technologies;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

public enum EmploymentType {
    FULL_TIME,
    PART_TIME,
    CONTRACT,
    INTERNSHIP,
    FREELANCE
}
```

**⚠️ 重要：JSON 字段处理**

前端期望 `achievements` 和 `technologies` 是数组，需要在 DTO 转换时处理：

```java
// Entity -> DTO
public List<String> getAchievementsList() {
    if (achievements == null || achievements.isEmpty()) {
        return List.of();
    }
    return objectMapper.readValue(achievements, new TypeReference<List<String>>() {});
}

// DTO -> Entity
public void setAchievementsList(List<String> list) {
    this.achievements = objectMapper.writeValueAsString(list);
}
```

---

### 2.4 ProfileSkill (技能)

```java
@Entity
@Table(name = "profile_skills")
public class ProfileSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "level", length = 20)
    private String level;  // 前端用字符串: "BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

---

### 2.5 ResumeHistory (简历历史)

```java
@Entity
@Table(name = "resume_history")
public class ResumeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "file_path", nullable = false)
    private String filePath;  // MinIO/S3 对象路径

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ResumeStatus status = ResumeStatus.PENDING;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    // 解析出的技能，JSON 数组
    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    // 解析出的结构化数据，JSON 对象
    @Column(name = "parsed_data", columnDefinition = "TEXT")
    private String parsedData;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}

public enum ResumeStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
```

**前端期望的 Resume 字段:**
```typescript
interface Resume {
  id: string;
  fileName: string;
  originalName?: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  isPrimary: boolean;
  skills: string[];        // 需要从 JSON 转换
  parsedData?: object;     // 需要从 JSON 转换
  createdAt: string;
  updatedAt: string;
  downloadUrl?: string;    // 需要生成预签名 URL
}
```

---

### 2.6 AiConfig (AI 配置)

```java
@Entity
@Table(name = "ai_configs")
public class AiConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Language Model
    @Column(name = "provider", length = 50)
    private String provider;  // openai, anthropic, custom

    @Column(name = "api_key")
    private String apiKey;  // 加密存储

    @Column(name = "model_name", length = 50)
    private String modelName;

    @Column(name = "base_url")
    private String baseUrl;

    // Vision Model
    @Column(name = "vision_provider", length = 50)
    private String visionProvider;

    @Column(name = "vision_api_key")
    private String visionApiKey;

    @Column(name = "vision_model_name", length = 50)
    private String visionModelName;

    @Column(name = "vision_base_url")
    private String visionBaseUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
```

**⚠️ API Key 安全:**
- 存储时加密
- 返回给前端时脱敏: `sk-***masked***`

---

### 2.7 Job (职位)

```java
@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "url_hash", nullable = false, unique = true, length = 64)
    private String urlHash;

    @Column(name = "source_url", nullable = false, columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "source", length = 50)
    @Enumerated(EnumType.STRING)
    private JobSource source;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "company", nullable = false)
    private String company;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "salary_range", length = 100)
    private String salary;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // JSON 数组
    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "is_remote")
    private Boolean isRemote = false;

    @Column(name = "posted_at")
    private Instant postedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

public enum JobSource {
    LINKEDIN,
    INDEED,
    GLASSDOOR,
    BOSS,
    LAGOU,
    OTHER
}
```

---

### 2.8 JobMatch (职位匹配)

```java
@Entity
@Table(name = "job_matches")
public class JobMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "match_score")
    private Integer matchScore;

    // JSON 数组: ["Skills match", "Experience level match"]
    @Column(name = "match_reasons", columnDefinition = "TEXT")
    private String matchReasons;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private MatchStatus status = MatchStatus.NEW;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

public enum MatchStatus {
    NEW,
    VIEWED,
    SAVED,
    APPLIED,
    REJECTED
}
```

---

### 2.9 InterviewSession (面试会话)

```java
@Entity
@Table(name = "interview_sessions")
public class InterviewSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_type", length = 50)
    @Enumerated(EnumType.STRING)
    private SessionType sessionType;

    @Column(name = "status", length = 50)
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(name = "current_question_index")
    private Integer currentQuestionIndex = 0;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "focus_area", length = 50)
    private String focusArea;

    @Column(name = "target_role", length = 100)
    private String targetRole;

    @Column(name = "extraction_status", length = 50)
    private String extractionStatus;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<InterviewAnswer> answers = new ArrayList<>();
}

public enum SessionType {
    INITIAL_PROFILE,
    DEEP_DIVE
}

public enum SessionStatus {
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    ABANDONED
}
```

---

### 2.10 GrowthGoal (成长目标)

```java
@Entity
@Table(name = "growth_goals")
public class GrowthGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "target_company", nullable = false, length = 100)
    private String targetCompany;

    @Column(name = "target_position", nullable = false, length = 100)
    private String targetPosition;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private GoalStatus status = GoalStatus.ACTIVE;

    @Column(name = "progress")
    private Integer progress = 0;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL)
    private List<SkillGap> gaps = new ArrayList<>();

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL)
    private List<LearningPath> paths = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

public enum GoalStatus {
    ACTIVE,
    ACHIEVED,
    ABANDONED
}
```

---

### 2.11 Post (社区帖子)

```java
@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "likes_count")
    private Integer likesCount = 0;

    @Column(name = "comments_count")
    private Integer commentsCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
```

**前端期望的 Post 字段:**
```typescript
interface Post {
  id: string;
  authorId: string;
  authorName: string;      // 需要关联查询
  authorRole?: string;     // 需要关联查询
  content: string;
  category?: string;
  likesCount: number;
  commentsCount: number;
  isLiked: boolean;        // 需要查询当前用户是否点赞
  createdAt: string;
  updatedAt: string;
}
```

---

### 2.12 Notification (通知)

```java
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "priority", length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(name = "action_url")
    private String actionUrl;

    @Column(name = "action_text", length = 50)
    private String actionText;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

public enum NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}
```

---

## 3. DTO 设计规范

### 3.1 命名规则

| 用途 | 后缀 | 示例 |
|------|------|------|
| 通用数据传输 | Dto | `UserDto`, `JobDto` |
| 请求参数 | Request | `LoginRequest`, `CreatePostRequest` |
| 响应数据 | Response | `AuthResponse`, `PagedResponse` |

### 3.2 DTO 示例

```java
// 请求 DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}

// 响应 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserDto user;
}

// 通用 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String email;
    private String nickname;
    private String role;
    private boolean hasPassword;
    private String createdAt;
    private boolean googleUser;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .hasPassword(user.getPasswordHash() != null)
                .createdAt(user.getCreatedAt().toString())
                .googleUser(user.getGoogleSub() != null)
                .build();
    }
}
```

### 3.3 分页响应

前端期望的分页格式：

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static <T> PagedResponse<T> from(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
```

---

## 4. 日期时间处理

### 4.1 存储格式
- 数据库: `TIMESTAMP WITH TIME ZONE`
- Java: `Instant` 或 `LocalDate`

### 4.2 API 格式
- 日期时间: ISO 8601 格式 `2026-01-17T10:00:00Z`
- 仅日期: `2026-01-17`

### 4.3 Jackson 配置

```java
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

```yaml
# application.yml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
    date-format: "yyyy-MM-dd'T'HH:mm:ss'Z'"
    time-zone: UTC
```
