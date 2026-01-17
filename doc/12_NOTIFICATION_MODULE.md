# Notification 模块详细设计

> 通知管理模块

---

## 1. 模块结构

```
biz/notification/
├── controller/
│   └── NotificationController.java
├── service/
│   └── NotificationService.java
├── repository/
│   └── NotificationRepository.java
├── entity/
│   └── Notification.java
└── dto/
    └── NotificationDto.java
```

---

## 2. API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/notifications` | 获取通知列表 |
| GET | `/api/notifications/unread-count` | 获取未读数量 |
| PUT | `/api/notifications/{notificationId}/read` | 标记为已读 |
| PUT | `/api/notifications/read-all` | 全部标记已读 |
| DELETE | `/api/notifications/{notificationId}` | 删除通知 |

---

## 3. 前端期望的数据结构

### 3.1 Notification

```typescript
interface Notification {
  id: string;
  userId: string;
  type: 'SYSTEM' | 'JOB_MATCH' | 'INTERVIEW_REMINDER' | 'GROWTH_TIP' | 'COMMUNITY';
  title: string;
  content: string;
  link?: string;           // 点击跳转链接
  data?: Record<string, any>;  // 额外数据
  isRead: boolean;
  createdAt: string;
}
```

---

## 4. 详细实现

### 4.1 Controller

```java
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取通知列表
     * GET /api/notifications
     */
    @GetMapping
    public ApiResponse<PagedResponse<NotificationDto>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PagedResponse<NotificationDto> notifications = notificationService.getNotifications(
                principal.getId(), type, isRead, page, size);
        return ApiResponse.success(notifications);
    }

    /**
     * 获取未读数量
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ApiResponse<Integer> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        int count = notificationService.getUnreadCount(principal.getId());
        return ApiResponse.success(count);
    }

    /**
     * 标记为已读
     * PUT /api/notifications/{notificationId}/read
     */
    @PutMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID notificationId) {
        notificationService.markAsRead(principal.getId(), notificationId);
        return ApiResponse.success();
    }

    /**
     * 全部标记已读
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal.getId());
        return ApiResponse.success();
    }

    /**
     * 删除通知
     * DELETE /api/notifications/{notificationId}
     */
    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteNotification(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID notificationId) {
        notificationService.deleteNotification(principal.getId(), notificationId);
        return ApiResponse.success();
    }
}
```

### 4.2 DTOs

```java
// NotificationDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private String id;
    private String userId;
    private String type;
    private String title;
    private String content;
    private String link;
    private Map<String, Object> data;
    private Boolean isRead;
    private String createdAt;

    public static NotificationDto fromEntity(Notification entity) {
        return NotificationDto.builder()
                .id(entity.getId().toString())
                .userId(entity.getUser().getId().toString())
                .type(entity.getType().name())
                .title(entity.getTitle())
                .content(entity.getContent())
                .link(entity.getLink())
                .data(parseData(entity.getDataJson()))
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt().toString())
                .build();
    }

    private static Map<String, Object> parseData(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return new ObjectMapper().readValue(json, 
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
```

### 4.3 Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 获取通知列表
     */
    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> getNotifications(UUID userId, 
            String type, Boolean isRead, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by("createdAt").descending());

        Page<Notification> notificationPage;

        if (type != null && isRead != null) {
            notificationPage = notificationRepository.findByUserIdAndTypeAndIsRead(
                    userId, NotificationType.valueOf(type), isRead, pageable);
        } else if (type != null) {
            notificationPage = notificationRepository.findByUserIdAndType(
                    userId, NotificationType.valueOf(type), pageable);
        } else if (isRead != null) {
            notificationPage = notificationRepository.findByUserIdAndIsRead(
                    userId, isRead, pageable);
        } else {
            notificationPage = notificationRepository.findByUserId(userId, pageable);
        }

        List<NotificationDto> notifications = notificationPage.getContent().stream()
                .map(NotificationDto::fromEntity)
                .toList();

        return PagedResponse.<NotificationDto>builder()
                .content(notifications)
                .page(page)
                .size(size)
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .hasNext(notificationPage.hasNext())
                .hasPrevious(notificationPage.hasPrevious())
                .build();
    }

    /**
     * 获取未读数量
     */
    @Transactional(readOnly = true)
    public int getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    /**
     * 标记为已读
     */
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 验证归属
        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * 全部标记已读
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked all notifications as read for user: {}", userId);
    }

    /**
     * 删除通知
     */
    @Transactional
    public void deleteNotification(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 验证归属
        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        notificationRepository.delete(notification);
        log.info("Deleted notification {} for user: {}", notificationId, userId);
    }

    // ==================== 发送通知 ====================

    /**
     * 发送系统通知
     */
    @Transactional
    public void sendSystemNotification(UUID userId, String title, String content) {
        createNotification(userId, NotificationType.SYSTEM, title, content, null, null);
    }

    /**
     * 发送职位匹配通知
     */
    @Transactional
    public void sendJobMatchNotification(UUID userId, String jobTitle, 
            String company, UUID jobId, int matchScore) {
        String title = "新的职位匹配";
        String content = String.format("我们为您找到一个匹配度 %d%% 的职位：%s - %s", 
                matchScore, jobTitle, company);
        String link = "/jobs/" + jobId;
        Map<String, Object> data = Map.of("jobId", jobId.toString(), "matchScore", matchScore);
        
        createNotification(userId, NotificationType.JOB_MATCH, title, content, link, data);
    }

    /**
     * 发送面试提醒通知
     */
    @Transactional
    public void sendInterviewReminder(UUID userId, String sessionId, String position) {
        String title = "面试练习提醒";
        String content = String.format("您有一个关于 %s 的面试练习等待继续", position);
        String link = "/interview/sessions/" + sessionId;
        
        createNotification(userId, NotificationType.INTERVIEW_REMINDER, title, content, link, null);
    }

    /**
     * 发送成长提示通知
     */
    @Transactional
    public void sendGrowthTip(UUID userId, String tip) {
        String title = "成长小贴士";
        createNotification(userId, NotificationType.GROWTH_TIP, title, tip, "/growth", null);
    }

    /**
     * 发送社区互动通知
     */
    @Transactional
    public void sendCommunityNotification(UUID userId, String title, 
            String content, String link) {
        createNotification(userId, NotificationType.COMMUNITY, title, content, link, null);
    }

    /**
     * 创建通知
     */
    private void createNotification(UUID userId, NotificationType type, 
            String title, String content, String link, Map<String, Object> data) {
        User user = userRepository.getReferenceById(userId);

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .link(link)
                .dataJson(data != null ? toJson(data) : null)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Created {} notification for user: {}", type, userId);
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
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String link;

    @Column(name = "data_json", columnDefinition = "TEXT")
    private String dataJson;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
}

public enum NotificationType {
    SYSTEM,
    JOB_MATCH,
    INTERVIEW_REMINDER,
    GROWTH_TIP,
    COMMUNITY
}
```

### 4.5 Repository

```java
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndType(UUID userId, NotificationType type, Pageable pageable);

    Page<Notification> findByUserIdAndIsRead(UUID userId, Boolean isRead, Pageable pageable);

    Page<Notification> findByUserIdAndTypeAndIsRead(UUID userId, NotificationType type, 
            Boolean isRead, Pageable pageable);

    int countByUserIdAndIsRead(UUID userId, Boolean isRead);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    int deleteOldNotifications(@Param("cutoff") Instant cutoff);
}
```

---

## 5. 定时任务

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationRepository notificationRepository;

    /**
     * 每天凌晨清理 30 天前的通知
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldNotifications() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        int deleted = notificationRepository.deleteOldNotifications(cutoff);
        log.info("Deleted {} old notifications", deleted);
    }
}
```

---

## 6. 错误码

| 错误码 | HTTP 状态 | 描述 |
|--------|----------|------|
| 110001 | 404 | Notification not found |
| 110002 | 400 | Invalid notification type |
| 110003 | 403 | Forbidden |

---

## 7. 常见 Bug 及修复

### Bug 1: 未读数量缓存不一致
**问题**: 频繁查询数据库影响性能

**修复**: 使用 Redis 缓存未读数量
```java
@Cacheable(value = "unreadCount", key = "#userId")
public int getUnreadCount(UUID userId) {
    return notificationRepository.countByUserIdAndIsRead(userId, false);
}

@CacheEvict(value = "unreadCount", key = "#userId")
public void markAsRead(UUID userId, UUID notificationId) {
    // ...
}
```

### Bug 2: data 字段返回 null
**问题**: 某些通知没有 data，前端访问报错

**修复**: 前端处理 null，或返回空对象

### Bug 3: 通知过多导致查询慢
**问题**: 用户通知积累过多

**修复**: 定时清理 + 分页限制

---

## 8. 测试用例

```java
@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Test
    @WithMockUser(userId = "test-user-id")
    void getUnreadCount_Success() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    @WithMockUser(userId = "test-user-id")
    void markAllAsRead_Success() throws Exception {
        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证未读数量为 0
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(jsonPath("$.data").value(0));
    }
}
```
