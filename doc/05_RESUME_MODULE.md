# Resume 模块详细设计

> 简历上传、解析、管理模块

---

## 1. 模块结构

```
biz/resume/
├── controller/
│   └── ResumeController.java
├── service/
│   ├── ResumeService.java
│   └── ResumeStorageService.java
├── repository/
│   └── ResumeHistoryRepository.java
├── entity/
│   └── ResumeHistory.java
└── dto/
    ├── ResumeDto.java
    └── ResumeUploadResponse.java
```

---

## 2. API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/resume/latest` | 获取最新简历 |
| POST | `/api/resume/upload` | 上传简历（multipart/form-data）|
| GET | `/api/resume/history` | 获取简历历史 |
| DELETE | `/api/resume/{resumeId}` | 删除简历 |
| PUT | `/api/resume/primary/{resumeId}` | 设置为主简历 |

---

## 3. 前端期望的数据结构

### 3.1 Resume 对象

```typescript
interface Resume {
  id: string;
  userId: string;
  filename: string;
  url?: string;
  parsedContent?: {
    personalInfo?: {
      name?: string;
      email?: string;
      phone?: string;
      location?: string;
      linkedin?: string;
      github?: string;
    };
    summary?: string;
    experiences?: Array<{
      company: string;
      title: string;
      startDate: string;
      endDate?: string;
      description: string;
    }>;
    education?: Array<{
      school: string;
      degree: string;
      field: string;
      graduationDate: string;
    }>;
    skills?: string[];
  };
  isPrimary: boolean;
  createdAt: string;
}
```

### 3.2 上传响应

```typescript
interface ResumeUploadResponse {
  success: boolean;
  data: {
    resume: Resume;
    parseStatus: 'SUCCESS' | 'PARTIAL' | 'FAILED';
    message?: string;
  };
}
```

---

## 4. 详细实现

### 4.1 Controller

```java
@RestController
@RequestMapping("/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * 获取最新简历（主简历）
     * GET /api/resume/latest
     */
    @GetMapping("/latest")
    public ApiResponse<ResumeDto> getLatestResume(
            @AuthenticationPrincipal UserPrincipal principal) {
        ResumeDto resume = resumeService.getLatestResume(principal.getId());
        return ApiResponse.success(resume);
    }

    /**
     * 上传简历
     * POST /api/resume/upload
     * Content-Type: multipart/form-data
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResumeUploadResponse> uploadResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        
        // 验证文件
        validateFile(file);
        
        ResumeUploadResponse response = resumeService.uploadAndParseResume(
                principal.getId(), file);
        return ApiResponse.success(response);
    }

    /**
     * 获取简历历史
     * GET /api/resume/history
     */
    @GetMapping("/history")
    public ApiResponse<List<ResumeDto>> getResumeHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ResumeDto> history = resumeService.getResumeHistory(principal.getId());
        return ApiResponse.success(history);
    }

    /**
     * 删除简历
     * DELETE /api/resume/{resumeId}
     */
    @DeleteMapping("/{resumeId}")
    public ApiResponse<Void> deleteResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId) {
        resumeService.deleteResume(principal.getId(), resumeId);
        return ApiResponse.success();
    }

    /**
     * 设置为主简历
     * PUT /api/resume/primary/{resumeId}
     */
    @PutMapping("/primary/{resumeId}")
    public ApiResponse<ResumeDto> setPrimaryResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId) {
        ResumeDto resume = resumeService.setPrimaryResume(principal.getId(), resumeId);
        return ApiResponse.success(resume);
    }

    /**
     * 验证上传文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        // 限制文件大小: 5MB
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        // 限制文件类型
        String contentType = file.getContentType();
        Set<String> allowedTypes = Set.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }
}
```

### 4.2 DTOs

```java
// ResumeDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDto {
    private String id;
    private String userId;
    private String filename;
    private String url;
    private ParsedContent parsedContent;
    private Boolean isPrimary;
    private String createdAt;

    public static ResumeDto fromEntity(ResumeHistory entity) {
        return ResumeDto.builder()
                .id(entity.getId().toString())
                .userId(entity.getUser().getId().toString())
                .filename(entity.getFilename())
                .url(entity.getFileUrl())
                .parsedContent(parseParsedContent(entity.getParsedContent()))
                .isPrimary(entity.getIsPrimary())
                .createdAt(entity.getCreatedAt().toString())
                .build();
    }

    private static ParsedContent parseParsedContent(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, ParsedContent.class);
        } catch (Exception e) {
            return null;
        }
    }
}

// ParsedContent.java - ⚠️ 嵌套结构
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedContent {
    private PersonalInfo personalInfo;
    private String summary;
    private List<WorkExperience> experiences;
    private List<Education> education;
    private List<String> skills;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalInfo {
        private String name;
        private String email;
        private String phone;
        private String location;
        private String linkedin;
        private String github;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkExperience {
        private String company;
        private String title;
        private String startDate;
        private String endDate;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Education {
        private String school;
        private String degree;
        private String field;
        private String graduationDate;
    }
}

// ResumeUploadResponse.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUploadResponse {
    private ResumeDto resume;
    private String parseStatus;  // SUCCESS, PARTIAL, FAILED
    private String message;
}
```

### 4.3 Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeHistoryRepository resumeRepository;
    private final UserRepository userRepository;
    private final ResumeStorageService storageService;
    private final AiEngineClient aiEngineClient;
    private final ObjectMapper objectMapper;

    /**
     * 获取最新（主）简历
     */
    @Transactional(readOnly = true)
    public ResumeDto getLatestResume(UUID userId) {
        return resumeRepository.findLatestByUserId(userId)
                .map(ResumeDto::fromEntity)
                .orElse(null);  // ⚠️ 没有简历时返回 null，不抛异常
    }

    /**
     * 上传并解析简历
     */
    @Transactional
    public ResumeUploadResponse uploadAndParseResume(UUID userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. 存储文件到 MinIO
        String filename = generateFilename(userId, file.getOriginalFilename());
        String fileUrl = storageService.uploadFile(file, filename);

        // 2. 调用 AI 引擎解析
        String parsedJson = null;
        String parseStatus = "FAILED";
        String parseMessage = null;

        try {
            parsedJson = aiEngineClient.parseResume(file.getBytes());
            parseStatus = "SUCCESS";
        } catch (Exception e) {
            log.error("Failed to parse resume", e);
            parseStatus = "FAILED";
            parseMessage = "Resume parsing failed, but file was uploaded successfully";
        }

        // 3. 保存记录
        ResumeHistory resume = ResumeHistory.builder()
                .user(user)
                .filename(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .parsedContent(parsedJson)
                .isPrimary(false)
                .build();

        resume = resumeRepository.save(resume);

        // 4. 如果是第一份简历，设为主简历
        long count = resumeRepository.countByUserId(userId);
        if (count == 1) {
            resume.setIsPrimary(true);
            resume = resumeRepository.save(resume);
        }

        log.info("Uploaded resume for user: {}, filename: {}", userId, filename);

        return ResumeUploadResponse.builder()
                .resume(ResumeDto.fromEntity(resume))
                .parseStatus(parseStatus)
                .message(parseMessage)
                .build();
    }

    /**
     * 获取简历历史
     */
    @Transactional(readOnly = true)
    public List<ResumeDto> getResumeHistory(UUID userId) {
        return resumeRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ResumeDto::fromEntity)
                .toList();
    }

    /**
     * 删除简历
     */
    @Transactional
    public void deleteResume(UUID userId, UUID resumeId) {
        ResumeHistory resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        // 验证归属
        if (!resume.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 如果删除的是主简历，需要选择另一个作为主简历
        if (Boolean.TRUE.equals(resume.getIsPrimary())) {
            resumeRepository.findFirstByUserIdAndIdNotOrderByCreatedAtDesc(userId, resumeId)
                    .ifPresent(r -> {
                        r.setIsPrimary(true);
                        resumeRepository.save(r);
                    });
        }

        // 删除文件
        storageService.deleteFile(resume.getFileUrl());

        resumeRepository.delete(resume);
        log.info("Deleted resume {} for user: {}", resumeId, userId);
    }

    /**
     * 设置为主简历
     */
    @Transactional
    public ResumeDto setPrimaryResume(UUID userId, UUID resumeId) {
        ResumeHistory resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        // 验证归属
        if (!resume.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 清除其他主简历
        resumeRepository.clearPrimaryByUserId(userId);

        // 设置当前为主简历
        resume.setIsPrimary(true);
        resume = resumeRepository.save(resume);

        log.info("Set resume {} as primary for user: {}", resumeId, userId);
        return ResumeDto.fromEntity(resume);
    }

    private String generateFilename(UUID userId, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return String.format("resumes/%s/%s%s", 
                userId, UUID.randomUUID(), extension);
    }
}
```

### 4.4 Storage Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.resumes}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 上传文件到 MinIO
     */
    public String uploadFile(MultipartFile file, String objectName) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            return getFileUrl(objectName);
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String fileUrl) {
        try {
            String objectName = extractObjectName(fileUrl);
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO: {}", fileUrl, e);
        }
    }

    /**
     * 获取预签名 URL（可选）
     */
    public String getPresignedUrl(String objectName, int expireMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(expireMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("Failed to get presigned URL", e);
            throw new BusinessException(ErrorCode.FILE_URL_GENERATION_FAILED);
        }
    }

    private String getFileUrl(String objectName) {
        return String.format("%s/%s/%s", endpoint, bucketName, objectName);
    }

    private String extractObjectName(String fileUrl) {
        // 从 URL 提取 object name
        return fileUrl.replace(endpoint + "/" + bucketName + "/", "");
    }
}
```

### 4.5 Repository

```java
public interface ResumeHistoryRepository extends JpaRepository<ResumeHistory, UUID> {

    List<ResumeHistory> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ResumeHistory> findFirstByUserIdAndIsPrimaryTrueOrderByCreatedAtDesc(UUID userId);

    /**
     * 获取最新简历（优先主简历）
     */
    @Query("SELECT r FROM ResumeHistory r " +
           "WHERE r.user.id = :userId " +
           "ORDER BY r.isPrimary DESC, r.createdAt DESC")
    List<ResumeHistory> findAllByUserIdOrdered(@Param("userId") UUID userId);

    default Optional<ResumeHistory> findLatestByUserId(UUID userId) {
        return findAllByUserIdOrdered(userId).stream().findFirst();
    }

    Optional<ResumeHistory> findFirstByUserIdAndIdNotOrderByCreatedAtDesc(
            UUID userId, UUID excludeId);

    long countByUserId(UUID userId);

    @Modifying
    @Query("UPDATE ResumeHistory r SET r.isPrimary = false WHERE r.user.id = :userId")
    void clearPrimaryByUserId(@Param("userId") UUID userId);
}
```

---

## 5. 与 AI 引擎的集成

### 5.1 gRPC 调用

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AiEngineClient {

    private final ResumeParserGrpc.ResumeParserBlockingStub resumeParserStub;
    private final ObjectMapper objectMapper;

    /**
     * 调用 AI 引擎解析简历
     */
    public String parseResume(byte[] fileContent) {
        try {
            ParseResumeRequest request = ParseResumeRequest.newBuilder()
                    .setFileContent(ByteString.copyFrom(fileContent))
                    .build();

            ParseResumeResponse response = resumeParserStub.parse(request);

            if (response.getSuccess()) {
                return response.getParsedJson();
            } else {
                throw new BusinessException(ErrorCode.RESUME_PARSE_FAILED,
                        response.getErrorMessage());
            }
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }
}
```

### 5.2 AI 引擎返回格式

AI 引擎应返回与 `ParsedContent` 结构一致的 JSON：

```json
{
  "personalInfo": {
    "name": "张三",
    "email": "zhangsan@email.com",
    "phone": "13800138000",
    "location": "北京",
    "linkedin": "linkedin.com/in/zhangsan",
    "github": "github.com/zhangsan"
  },
  "summary": "5年 Java 后端开发经验...",
  "experiences": [
    {
      "company": "阿里巴巴",
      "title": "高级工程师",
      "startDate": "2020-01",
      "endDate": "2023-06",
      "description": "负责电商核心系统..."
    }
  ],
  "education": [
    {
      "school": "北京大学",
      "degree": "本科",
      "field": "计算机科学",
      "graduationDate": "2018-06"
    }
  ],
  "skills": ["Java", "Spring Boot", "MySQL", "Redis"]
}
```

---

## 6. 错误码

| 错误码 | HTTP 状态 | 描述 |
|--------|----------|------|
| 40001 | 404 | Resume not found |
| 40002 | 400 | File is empty |
| 40003 | 400 | File too large (max 5MB) |
| 40004 | 400 | File type not allowed |
| 40005 | 500 | File upload failed |
| 40006 | 500 | Resume parsing failed |
| 40007 | 503 | AI service unavailable |

---

## 7. 常见 Bug 及修复

### Bug 1: 上传时 Content-Type 检查失败
**问题**: 某些浏览器发送的 Content-Type 不标准

**修复**: 检查文件扩展名作为 fallback
```java
private boolean isAllowedType(MultipartFile file) {
    String contentType = file.getContentType();
    String filename = file.getOriginalFilename();
    
    // 先检查 Content-Type
    if (contentType != null && ALLOWED_TYPES.contains(contentType)) {
        return true;
    }
    
    // Fallback: 检查扩展名
    if (filename != null) {
        String ext = filename.toLowerCase();
        return ext.endsWith(".pdf") || ext.endsWith(".doc") || ext.endsWith(".docx");
    }
    
    return false;
}
```

### Bug 2: parsedContent 为空时前端报错
**问题**: 解析失败时 `parsedContent` 为 `null`，前端访问 `parsedContent.skills` 报错

**修复**: 确保返回空对象而非 null
```java
private static ParsedContent parseParsedContent(String json) {
    if (json == null || json.isEmpty()) {
        return new ParsedContent();  // 返回空对象
    }
    // ...
}
```

### Bug 3: 主简历状态不一致
**问题**: 删除主简历后没有设置新的主简历

**修复**: 见 `deleteResume` 方法中的处理逻辑

### Bug 4: 文件 URL 过期
**问题**: MinIO URL 过期后无法访问

**修复**: 使用预签名 URL 或配置 bucket 为公开读取

---

## 8. 测试用例

```java
@SpringBootTest
@AutoConfigureMockMvc
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(userId = "test-user-id")
    void uploadResume_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        mockMvc.perform(multipart("/api/resume/upload")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resume.filename").value("resume.pdf"))
                .andExpect(jsonPath("$.data.parseStatus").exists());
    }

    @Test
    @WithMockUser(userId = "test-user-id")
    void uploadResume_FileTooLarge() throws Exception {
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent);

        mockMvc.perform(multipart("/api/resume/upload")
                .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(40003));
    }
}
```
