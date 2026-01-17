# 编码规范与最佳实践

> 后端代码规范、命名约定、架构模式

---

## 1. 项目结构

### 1.1 标准包结构

```
com.cvibe.biz/
├── common/                    # 公共组件
│   ├── config/               # 配置类
│   ├── exception/            # 异常类
│   ├── response/             # 响应类
│   ├── security/             # 安全组件
│   └── util/                 # 工具类
│
├── auth/                      # 认证模块
│   ├── controller/
│   ├── service/
│   ├── dto/
│   └── repository/
│
├── profile/                   # Profile 模块
│   ├── controller/
│   ├── service/
│   ├── dto/
│   ├── entity/
│   └── repository/
│
├── resume/                    # Resume 模块
│   └── ...
│
└── ...                        # 其他模块
```

### 1.2 模块内结构

```
module/
├── controller/
│   └── XxxController.java     # REST 控制器
├── service/
│   ├── XxxService.java        # 业务逻辑
│   └── impl/                  # （可选）实现类
├── dto/
│   ├── XxxDto.java            # 数据传输对象
│   ├── XxxRequest.java        # 请求对象
│   └── XxxResponse.java       # 响应对象
├── entity/
│   └── Xxx.java               # JPA 实体
├── repository/
│   └── XxxRepository.java     # 数据访问层
└── mapper/                    # （可选）对象映射
    └── XxxMapper.java
```

---

## 2. 命名约定

### 2.1 类命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 控制器 | `XxxController` | `ProfileController` |
| 服务类 | `XxxService` | `ProfileService` |
| 仓库接口 | `XxxRepository` | `UserRepository` |
| 实体类 | `Xxx` (单数) | `User`, `Post` |
| DTO | `XxxDto` | `ProfileDto` |
| 请求类 | `XxxRequest` | `CreateGoalRequest` |
| 响应类 | `XxxResponse` | `LoginResponse` |
| 配置类 | `XxxConfig` | `SecurityConfig` |
| 异常类 | `XxxException` | `BusinessException` |
| 枚举 | `XxxType/Status/Level` | `GoalStatus` |

### 2.2 方法命名

| 操作 | Controller | Service |
|------|------------|---------|
| 获取单个 | `getXxx()` | `getXxx()` / `findXxx()` |
| 获取列表 | `getXxxList()` / `getXxxs()` | `getXxxs()` / `listXxx()` |
| 创建 | `createXxx()` | `createXxx()` / `addXxx()` |
| 更新 | `updateXxx()` | `updateXxx()` |
| 删除 | `deleteXxx()` | `deleteXxx()` / `removeXxx()` |
| 搜索 | `searchXxx()` | `searchXxx()` |

### 2.3 变量命名

```java
// ✅ Good
UUID userId;
String emailAddress;
List<Post> posts;
boolean isActive;
int totalCount;

// ❌ Bad
UUID id;           // 不够具体
String mail;       // 缩写不清晰
List<Post> list;   // 名称无意义
boolean active;    // 布尔变量应以 is/has/can 开头
int cnt;           // 不要用缩写
```

### 2.4 常量命名

```java
// 类常量
public static final int MAX_FILE_SIZE = 5 * 1024 * 1024;
public static final String DEFAULT_LANGUAGE = "zh";

// 配置键
public static final String CONFIG_KEY_JWT_SECRET = "jwt.secret";
```

---

## 3. Controller 规范

### 3.1 基本结构

```java
@Slf4j
@RestController
@RequestMapping("/xxx")
@RequiredArgsConstructor
public class XxxController {

    private final XxxService xxxService;

    /**
     * 获取资源
     * GET /api/xxx/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<XxxDto> getXxx(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        XxxDto result = xxxService.getXxx(principal.getId(), id);
        return ApiResponse.success(result);
    }

    /**
     * 创建资源
     * POST /api/xxx
     */
    @PostMapping
    public ApiResponse<XxxDto> createXxx(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateXxxRequest request) {
        XxxDto result = xxxService.createXxx(principal.getId(), request);
        return ApiResponse.success(result);
    }
}
```

### 3.2 规范要点

1. **使用 `@RequiredArgsConstructor`** 进行依赖注入
2. **Controller 只做**：参数接收、调用 Service、返回响应
3. **不要在 Controller 中写业务逻辑**
4. **使用 `@Valid` 校验请求参数**
5. **使用 `@AuthenticationPrincipal` 获取当前用户**
6. **统一返回 `ApiResponse<T>`**

### 3.3 分页参数

```java
@GetMapping
public ApiResponse<PagedResponse<XxxDto>> getList(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir) {
    // ...
}
```

---

## 4. Service 规范

### 4.1 基本结构

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class XxxService {

    private final XxxRepository xxxRepository;
    private final YyyRepository yyyRepository;
    private final ObjectMapper objectMapper;

    /**
     * 获取资源
     */
    @Transactional(readOnly = true)
    public XxxDto getXxx(UUID userId, UUID xxxId) {
        Xxx xxx = xxxRepository.findById(xxxId)
                .orElseThrow(() -> new BusinessException(ErrorCode.XXX_NOT_FOUND));

        // 验证权限
        if (!xxx.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return XxxDto.fromEntity(xxx);
    }

    /**
     * 创建资源
     */
    @Transactional
    public XxxDto createXxx(UUID userId, CreateXxxRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Xxx xxx = Xxx.builder()
                .user(user)
                .field1(request.getField1())
                .field2(request.getField2())
                .build();

        xxx = xxxRepository.save(xxx);
        log.info("Created xxx {} for user: {}", xxx.getId(), userId);

        return XxxDto.fromEntity(xxx);
    }
}
```

### 4.2 规范要点

1. **使用 `@Transactional`**：
   - 只读操作：`@Transactional(readOnly = true)`
   - 写操作：`@Transactional`
2. **先验证权限，再执行操作**
3. **使用 Builder 模式创建对象**
4. **记录关键操作日志**
5. **抛出业务异常而非返回 null**

### 4.3 事务边界

```java
// ✅ Good - 事务在 Service 层
@Transactional
public void transferPoints(UUID fromUser, UUID toUser, int points) {
    deductPoints(fromUser, points);
    addPoints(toUser, points);
}

// ❌ Bad - 事务在 Controller 层
@PostMapping("/transfer")
@Transactional  // 不要这样做
public ApiResponse<Void> transfer(...) {
    // ...
}
```

---

## 5. Entity 规范

### 5.1 基类

```java
@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
```

### 5.2 实体定义

```java
@Entity
@Table(name = "posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostCategory category;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    // 使用 JSON 存储数组
    @Column(columnDefinition = "TEXT")
    private String tags;  // JSON array: ["java", "career"]
}
```

### 5.3 规范要点

1. **继承 `BaseEntity`** 获得 id、createdAt、updatedAt
2. **使用 `FetchType.LAZY`** 延迟加载关联
3. **使用 `@Enumerated(EnumType.STRING)`** 存储枚举
4. **使用 `@Column(columnDefinition = "TEXT")` 存储长文本和 JSON**
5. **设置合理的默认值**
6. **使用 Lombok 的 `@Data`、`@Builder`**

---

## 6. DTO 规范

### 6.1 DTO 定义

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    private String id;           // UUID 转 String
    private AuthorDto author;
    private String content;
    private List<String> tags;   // JSON 解析为 List
    private String category;     // Enum 转 String
    private Integer likesCount;
    private Boolean isLiked;     // 动态计算字段
    private String createdAt;    // Instant 转 String

    /**
     * Entity -> DTO 转换
     */
    public static PostDto fromEntity(Post entity, boolean isLiked) {
        return PostDto.builder()
                .id(entity.getId().toString())
                .author(AuthorDto.fromEntity(entity.getAuthor()))
                .content(entity.getContent())
                .tags(parseJsonArray(entity.getTags()))
                .category(entity.getCategory().name())
                .likesCount(entity.getLikesCount())
                .isLiked(isLiked)
                .createdAt(entity.getCreatedAt().toString())
                .build();
    }
}
```

### 6.2 规范要点

1. **所有 ID 返回 String**（避免 JavaScript 数字精度问题）
2. **所有时间返回 ISO 8601 格式字符串**
3. **所有枚举返回字符串**
4. **JSON 字段解析为对应类型**
5. **提供 `fromEntity()` 静态方法**

---

## 7. Repository 规范

```java
public interface PostRepository extends JpaRepository<Post, UUID> {

    // 简单查询 - 使用方法名推导
    List<Post> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    Page<Post> findByCategory(PostCategory category, Pageable pageable);

    boolean existsByIdAndAuthorId(UUID id, UUID authorId);

    // 复杂查询 - 使用 @Query
    @Query("SELECT p FROM Post p WHERE p.author.id = :authorId AND p.category = :category")
    List<Post> findByAuthorAndCategory(
            @Param("authorId") UUID authorId,
            @Param("category") PostCategory category);

    // 更新操作 - 使用 @Modifying
    @Modifying
    @Query("UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.id = :postId")
    void incrementLikesCount(@Param("postId") UUID postId);

    // 删除操作
    @Modifying
    @Query("DELETE FROM Post p WHERE p.createdAt < :cutoff")
    int deleteOldPosts(@Param("cutoff") Instant cutoff);
}
```

---

## 8. 安全规范

### 8.1 权限检查

```java
// 每个操作都要检查归属权限
public void updatePost(UUID userId, UUID postId, UpdatePostRequest request) {
    Post post = postRepository.findById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

    // ⚠️ 必须检查！
    if (!post.getAuthor().getId().equals(userId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    // 继续更新...
}
```

### 8.2 输入验证

```java
@Data
public class CreatePostRequest {
    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content too long")
    private String content;

    @NotBlank(message = "Category is required")
    @Pattern(regexp = "^(DISCUSSION|QUESTION|SHARE|JOB|EXPERIENCE)$",
            message = "Invalid category")
    private String category;
}
```

### 8.3 敏感数据处理

```java
// 密码加密
user.setPassword(passwordEncoder.encode(request.getPassword()));

// 不要返回敏感信息
@JsonIgnore
private String password;

// 日志中不要打印敏感信息
log.info("User logged in: {}", user.getEmail());  // ✅
log.info("User logged in with password: {}", password);  // ❌
```

---

## 9. 性能最佳实践

### 9.1 N+1 问题

```java
// ❌ Bad - N+1 查询
List<Post> posts = postRepository.findAll();
posts.forEach(post -> {
    post.getAuthor().getName();  // 每个 post 都会触发一次查询
});

// ✅ Good - 使用 JOIN FETCH
@Query("SELECT p FROM Post p JOIN FETCH p.author")
List<Post> findAllWithAuthor();
```

### 9.2 批量操作

```java
// ❌ Bad - 单条更新
for (UUID id : ids) {
    Post post = postRepository.findById(id).orElseThrow();
    post.setStatus(Status.DELETED);
    postRepository.save(post);
}

// ✅ Good - 批量更新
@Modifying
@Query("UPDATE Post p SET p.status = :status WHERE p.id IN :ids")
void updateStatusByIds(@Param("ids") List<UUID> ids, @Param("status") Status status);
```

### 9.3 分页查询

```java
// 使用 Pageable 进行分页
Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
Page<Post> posts = postRepository.findByCategory(category, pageable);
```

---

## 10. 代码审查清单

### 10.1 必须检查

- [ ] 所有端点都有权限检查
- [ ] 敏感操作有日志记录
- [ ] 输入参数有验证
- [ ] 异常被正确处理
- [ ] 事务边界正确设置
- [ ] 没有 N+1 查询问题
- [ ] 没有敏感信息泄露

### 10.2 代码质量

- [ ] 命名清晰有意义
- [ ] 方法长度 < 50 行
- [ ] 类长度 < 500 行
- [ ] 没有重复代码
- [ ] 有必要的注释
- [ ] 单元测试覆盖核心逻辑

---

## 11. Git 提交规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 类型 (type)

| 类型 | 描述 |
|------|------|
| feat | 新功能 |
| fix | Bug 修复 |
| docs | 文档更新 |
| style | 代码格式调整 |
| refactor | 代码重构 |
| test | 测试相关 |
| chore | 构建/工具变动 |

### 示例

```
feat(profile): add experience CRUD endpoints

- Add POST /api/profile/experiences
- Add PUT /api/profile/experiences/{id}
- Add DELETE /api/profile/experiences/{id}

Closes #123
```
