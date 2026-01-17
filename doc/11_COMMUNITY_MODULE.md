# Community 模块详细设计

> 社区模块，提供帖子发布、评论、点赞功能

---

## 1. 模块结构

```
biz/community/
├── controller/
│   └── CommunityController.java
├── service/
│   └── CommunityService.java
├── repository/
│   ├── PostRepository.java
│   ├── CommentRepository.java
│   └── PostLikeRepository.java
├── entity/
│   ├── Post.java
│   ├── Comment.java
│   └── PostLike.java
└── dto/
    ├── PostDto.java
    ├── CommentDto.java
    └── CreatePostRequest.java
```

---

## 2. API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/community/posts` | 获取帖子列表 |
| POST | `/api/community/posts` | 创建帖子 |
| GET | `/api/community/posts/{postId}` | 获取帖子详情 |
| DELETE | `/api/community/posts/{postId}` | 删除帖子 |
| POST | `/api/community/posts/{postId}/like` | 点赞 |
| DELETE | `/api/community/posts/{postId}/like` | 取消点赞 |
| GET | `/api/community/posts/{postId}/comments` | 获取评论列表 |
| POST | `/api/community/posts/{postId}/comments` | 添加评论 |
| DELETE | `/api/community/comments/{commentId}` | 删除评论 |

---

## 3. 前端期望的数据结构

### 3.1 Post

```typescript
interface Post {
  id: string;
  author: {
    id: string;
    name: string;
    avatar?: string;
    headline?: string;
  };
  content: string;
  images?: string[];
  tags?: string[];
  category: 'DISCUSSION' | 'QUESTION' | 'SHARE' | 'JOB' | 'EXPERIENCE';
  likesCount: number;
  commentsCount: number;
  isLiked: boolean;
  createdAt: string;
  updatedAt: string;
}
```

### 3.2 Comment

```typescript
interface Comment {
  id: string;
  postId: string;
  author: {
    id: string;
    name: string;
    avatar?: string;
  };
  content: string;
  parentId?: string;          // 回复的评论 ID
  repliesCount: number;
  createdAt: string;
}
```

---

## 4. 详细实现

### 4.1 Controller

```java
@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    /**
     * 获取帖子列表
     * GET /api/community/posts
     */
    @GetMapping("/posts")
    public ApiResponse<PagedResponse<PostDto>> getPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sortBy) {
        
        PagedResponse<PostDto> posts = communityService.getPosts(
                principal != null ? principal.getId() : null,
                category, tag, page, size, sortBy);
        return ApiResponse.success(posts);
    }

    /**
     * 创建帖子
     * POST /api/community/posts
     */
    @PostMapping("/posts")
    public ApiResponse<PostDto> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePostRequest request) {
        PostDto post = communityService.createPost(principal.getId(), request);
        return ApiResponse.success(post);
    }

    /**
     * 获取帖子详情
     * GET /api/community/posts/{postId}
     */
    @GetMapping("/posts/{postId}")
    public ApiResponse<PostDto> getPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId) {
        PostDto post = communityService.getPost(
                principal != null ? principal.getId() : null, postId);
        return ApiResponse.success(post);
    }

    /**
     * 删除帖子
     * DELETE /api/community/posts/{postId}
     */
    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId) {
        communityService.deletePost(principal.getId(), postId);
        return ApiResponse.success();
    }

    /**
     * 点赞
     * POST /api/community/posts/{postId}/like
     */
    @PostMapping("/posts/{postId}/like")
    public ApiResponse<Void> likePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId) {
        communityService.likePost(principal.getId(), postId);
        return ApiResponse.success();
    }

    /**
     * 取消点赞
     * DELETE /api/community/posts/{postId}/like
     */
    @DeleteMapping("/posts/{postId}/like")
    public ApiResponse<Void> unlikePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId) {
        communityService.unlikePost(principal.getId(), postId);
        return ApiResponse.success();
    }

    /**
     * 获取评论列表
     * GET /api/community/posts/{postId}/comments
     */
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<PagedResponse<CommentDto>> getComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CommentDto> comments = communityService.getComments(
                postId, page, size);
        return ApiResponse.success(comments);
    }

    /**
     * 添加评论
     * POST /api/community/posts/{postId}/comments
     */
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommentDto> addComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentDto comment = communityService.addComment(
                principal.getId(), postId, request);
        return ApiResponse.success(comment);
    }

    /**
     * 删除评论
     * DELETE /api/community/comments/{commentId}
     */
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID commentId) {
        communityService.deleteComment(principal.getId(), commentId);
        return ApiResponse.success();
    }
}
```

### 4.2 DTOs

```java
// PostDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    private String id;
    private AuthorDto author;
    private String content;
    private List<String> images;
    private List<String> tags;
    private String category;
    private Integer likesCount;
    private Integer commentsCount;
    private Boolean isLiked;
    private String createdAt;
    private String updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorDto {
        private String id;
        private String name;
        private String avatar;
        private String headline;
    }

    public static PostDto fromEntity(Post entity, boolean isLiked) {
        return PostDto.builder()
                .id(entity.getId().toString())
                .author(AuthorDto.builder()
                        .id(entity.getAuthor().getId().toString())
                        .name(entity.getAuthor().getName())
                        .avatar(entity.getAuthor().getAvatarUrl())
                        .headline(getHeadline(entity.getAuthor()))
                        .build())
                .content(entity.getContent())
                .images(parseJsonArray(entity.getImages()))
                .tags(parseJsonArray(entity.getTags()))
                .category(entity.getCategory() != null 
                        ? entity.getCategory().name() : null)
                .likesCount(entity.getLikesCount())
                .commentsCount(entity.getCommentsCount())
                .isLiked(isLiked)
                .createdAt(entity.getCreatedAt().toString())
                .updatedAt(entity.getUpdatedAt().toString())
                .build();
    }

    private static String getHeadline(User user) {
        // 从 profile 获取 headline
        if (user.getProfile() != null) {
            return user.getProfile().getHeadline();
        }
        return null;
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

// CommentDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private String id;
    private String postId;
    private PostDto.AuthorDto author;
    private String content;
    private String parentId;
    private Integer repliesCount;
    private String createdAt;

    public static CommentDto fromEntity(Comment entity) {
        return CommentDto.builder()
                .id(entity.getId().toString())
                .postId(entity.getPost().getId().toString())
                .author(PostDto.AuthorDto.builder()
                        .id(entity.getAuthor().getId().toString())
                        .name(entity.getAuthor().getName())
                        .avatar(entity.getAuthor().getAvatarUrl())
                        .build())
                .content(entity.getContent())
                .parentId(entity.getParent() != null 
                        ? entity.getParent().getId().toString() : null)
                .repliesCount(entity.getRepliesCount())
                .createdAt(entity.getCreatedAt().toString())
                .build();
    }
}

// CreatePostRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {
    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content too long")
    private String content;

    private List<String> images;

    private List<String> tags;

    @NotBlank(message = "Category is required")
    private String category;  // DISCUSSION, QUESTION, SHARE, JOB, EXPERIENCE
}

// CreateCommentRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {
    @NotBlank(message = "Content is required")
    @Size(max = 1000, message = "Comment too long")
    private String content;

    private String parentId;  // 回复的评论 ID（可选）
}
```

### 4.3 Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 获取帖子列表
     */
    @Transactional(readOnly = true)
    public PagedResponse<PostDto> getPosts(UUID userId, String category, 
            String tag, int page, int size, String sortBy) {
        
        // 构建排序
        Sort sort = switch (sortBy) {
            case "hot" -> Sort.by("likesCount").descending();
            case "comments" -> Sort.by("commentsCount").descending();
            default -> Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Post> postPage;
        if (category != null && !category.isEmpty()) {
            postPage = postRepository.findByCategory(
                    PostCategory.valueOf(category), pageable);
        } else {
            postPage = postRepository.findAll(pageable);
        }

        // 获取用户点赞的帖子 ID
        Set<UUID> likedPostIds = userId != null
                ? likeRepository.findLikedPostIdsByUserId(userId)
                : Set.of();

        List<PostDto> posts = postPage.getContent().stream()
                .map(post -> PostDto.fromEntity(post, 
                        likedPostIds.contains(post.getId())))
                .toList();

        return PagedResponse.<PostDto>builder()
                .content(posts)
                .page(page)
                .size(size)
                .totalElements(postPage.getTotalElements())
                .totalPages(postPage.getTotalPages())
                .hasNext(postPage.hasNext())
                .hasPrevious(postPage.hasPrevious())
                .build();
    }

    /**
     * 创建帖子
     */
    @Transactional
    public PostDto createPost(UUID userId, CreatePostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.builder()
                .author(user)
                .content(request.getContent())
                .images(toJson(request.getImages()))
                .tags(toJson(request.getTags()))
                .category(PostCategory.valueOf(request.getCategory()))
                .likesCount(0)
                .commentsCount(0)
                .build();

        post = postRepository.save(post);
        log.info("Created post {} by user: {}", post.getId(), userId);

        return PostDto.fromEntity(post, false);
    }

    /**
     * 获取帖子详情
     */
    @Transactional(readOnly = true)
    public PostDto getPost(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        boolean isLiked = userId != null && 
                likeRepository.existsByUserIdAndPostId(userId, postId);

        return PostDto.fromEntity(post, isLiked);
    }

    /**
     * 删除帖子
     */
    @Transactional
    public void deletePost(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 验证归属
        if (!post.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 删除关联的评论和点赞（使用级联删除或手动删除）
        commentRepository.deleteByPostId(postId);
        likeRepository.deleteByPostId(postId);
        
        postRepository.delete(post);
        log.info("Deleted post {} by user: {}", postId, userId);
    }

    /**
     * 点赞
     */
    @Transactional
    public void likePost(UUID userId, UUID postId) {
        // 检查是否已点赞
        if (likeRepository.existsByUserIdAndPostId(userId, postId)) {
            return;  // 幂等操作
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.getReferenceById(userId);

        PostLike like = PostLike.builder()
                .user(user)
                .post(post)
                .build();

        likeRepository.save(like);

        // 更新点赞数
        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);

        log.info("User {} liked post {}", userId, postId);
    }

    /**
     * 取消点赞
     */
    @Transactional
    public void unlikePost(UUID userId, UUID postId) {
        Optional<PostLike> likeOpt = likeRepository.findByUserIdAndPostId(userId, postId);
        
        if (likeOpt.isEmpty()) {
            return;  // 幂等操作
        }

        likeRepository.delete(likeOpt.get());

        // 更新点赞数
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
        postRepository.save(post);

        log.info("User {} unliked post {}", userId, postId);
    }

    /**
     * 获取评论列表
     */
    @Transactional(readOnly = true)
    public PagedResponse<CommentDto> getComments(UUID postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by("createdAt").descending());

        // 只获取顶级评论（parentId 为 null）
        Page<Comment> commentPage = commentRepository.findByPostIdAndParentIsNull(
                postId, pageable);

        List<CommentDto> comments = commentPage.getContent().stream()
                .map(CommentDto::fromEntity)
                .toList();

        return PagedResponse.<CommentDto>builder()
                .content(comments)
                .page(page)
                .size(size)
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .hasNext(commentPage.hasNext())
                .hasPrevious(commentPage.hasPrevious())
                .build();
    }

    /**
     * 添加评论
     */
    @Transactional
    public CommentDto addComment(UUID userId, UUID postId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Comment.CommentBuilder builder = Comment.builder()
                .post(post)
                .author(user)
                .content(request.getContent())
                .repliesCount(0);

        // 处理回复
        if (request.getParentId() != null && !request.getParentId().isEmpty()) {
            Comment parent = commentRepository.findById(UUID.fromString(request.getParentId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
            builder.parent(parent);

            // 更新父评论的回复数
            parent.setRepliesCount(parent.getRepliesCount() + 1);
            commentRepository.save(parent);
        }

        Comment comment = commentRepository.save(builder.build());

        // 更新帖子评论数
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        log.info("User {} commented on post {}", userId, postId);
        return CommentDto.fromEntity(comment);
    }

    /**
     * 删除评论
     */
    @Transactional
    public void deleteComment(UUID userId, UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        // 验证归属
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Post post = comment.getPost();

        // 如果是父评论，也删除所有回复
        int deletedCount = 1;
        if (comment.getParent() == null) {
            deletedCount += commentRepository.deleteByParentId(commentId);
        } else {
            // 更新父评论的回复数
            Comment parent = comment.getParent();
            parent.setRepliesCount(Math.max(0, parent.getRepliesCount() - 1));
            commentRepository.save(parent);
        }

        commentRepository.delete(comment);

        // 更新帖子评论数
        post.setCommentsCount(Math.max(0, post.getCommentsCount() - deletedCount));
        postRepository.save(post);

        log.info("Deleted comment {} by user: {}", commentId, userId);
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
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
// Post.java
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

    @Column(columnDefinition = "TEXT")
    private String images;  // JSON array

    @Column(columnDefinition = "TEXT")
    private String tags;  // JSON array

    @Enumerated(EnumType.STRING)
    private PostCategory category;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    @Column(name = "comments_count", nullable = false)
    private Integer commentsCount = 0;
}

// Comment.java
@Entity
@Table(name = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(name = "replies_count", nullable = false)
    private Integer repliesCount = 0;
}

// PostLike.java
@Entity
@Table(name = "post_likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "post_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
}

public enum PostCategory {
    DISCUSSION,
    QUESTION,
    SHARE,
    JOB,
    EXPERIENCE
}
```

### 4.5 Repository

```java
public interface PostRepository extends JpaRepository<Post, UUID> {

    Page<Post> findByCategory(PostCategory category, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.author.id = :authorId")
    Page<Post> findByAuthorId(@Param("authorId") UUID authorId, Pageable pageable);
}

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByPostIdAndParentIsNull(UUID postId, Pageable pageable);

    List<Comment> findByParentId(UUID parentId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id = :postId")
    void deleteByPostId(@Param("postId") UUID postId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.parent.id = :parentId")
    int deleteByParentId(@Param("parentId") UUID parentId);
}

public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    boolean existsByUserIdAndPostId(UUID userId, UUID postId);

    Optional<PostLike> findByUserIdAndPostId(UUID userId, UUID postId);

    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId")
    Set<UUID> findLikedPostIdsByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.post.id = :postId")
    void deleteByPostId(@Param("postId") UUID postId);
}
```

---

## 5. 错误码

| 错误码 | HTTP 状态 | 描述 |
|--------|----------|------|
| 100001 | 404 | Post not found |
| 100002 | 404 | Comment not found |
| 100003 | 400 | Invalid category |
| 100004 | 403 | Forbidden |
| 100005 | 400 | Content too long |

---

## 6. 常见 Bug 及修复

### Bug 1: 点赞数不一致
**问题**: 并发点赞时计数不准确

**修复**: 使用乐观锁或 SQL 原子更新
```java
@Modifying
@Query("UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.id = :postId")
void incrementLikesCount(@Param("postId") UUID postId);
```

### Bug 2: isLiked 状态查询性能问题
**问题**: 每个帖子单独查询是否点赞

**修复**: 批量查询
```java
Set<UUID> likedPostIds = likeRepository.findLikedPostIdsByUserId(userId);
```

### Bug 3: 删除帖子后评论未清理
**问题**: 帖子删除后评论仍存在

**修复**: 使用级联删除或手动删除

---

## 7. 测试用例

```java
@SpringBootTest
@AutoConfigureMockMvc
class CommunityControllerTest {

    @Test
    @WithMockUser(userId = "test-user-id")
    void createPost_Success() throws Exception {
        CreatePostRequest request = new CreatePostRequest();
        request.setContent("This is a test post");
        request.setCategory("DISCUSSION");
        request.setTags(List.of("java", "career"));

        mockMvc.perform(post("/api/community/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("This is a test post"));
    }

    @Test
    @WithMockUser(userId = "test-user-id")
    void likePost_Success() throws Exception {
        UUID postId = createTestPost();

        mockMvc.perform(post("/api/community/posts/{postId}/like", postId))
                .andExpect(status().isOk());

        // 验证点赞状态
        mockMvc.perform(get("/api/community/posts/{postId}", postId))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likesCount").value(1));
    }
}
```
