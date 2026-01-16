# Resume Builder 模块详细设计

> AI 驱动的简历生成器模块

---

## 1. 模块结构

```
biz/resumebuilder/
├── controller/
│   └── ResumeBuilderController.java
├── service/
│   └── ResumeBuilderService.java
├── repository/
│   └── ResumeTemplateRepository.java
├── entity/
│   └── ResumeTemplate.java
└── dto/
    ├── ResumeTemplateDto.java
    ├── GenerateResumeRequest.java
    └── GeneratedResumeDto.java
```

---

## 2. API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/resume-builder/templates` | 获取模板列表 |
| POST | `/api/resume-builder/generate` | AI 生成简历 |
| GET | `/api/resume-builder/preview/{templateId}` | 预览模板 |

---

## 3. 前端期望的数据结构

### 3.1 模板列表

```typescript
interface ResumeTemplate {
  id: string;
  name: string;
  description: string;
  thumbnail: string;      // 预览图 URL
  category: string;       // "professional" | "creative" | "academic" | "simple"
  isPremium: boolean;
}
```

### 3.2 生成请求

```typescript
interface GenerateResumeRequest {
  templateId: string;
  targetPosition: string;
  targetCompany?: string;
  customizations?: {
    tone?: 'professional' | 'friendly' | 'confident';
    focus?: 'technical' | 'leadership' | 'achievements';
    length?: 'brief' | 'standard' | 'detailed';
  };
}
```

### 3.3 生成响应

```typescript
interface GeneratedResume {
  id: string;
  content: {
    personalInfo: {
      name: string;
      email: string;
      phone: string;
      location: string;
      linkedin?: string;
      github?: string;
    };
    summary: string;       // AI 优化后的摘要
    experiences: Array<{
      company: string;
      title: string;
      startDate: string;
      endDate?: string;
      bullets: string[];   // AI 优化后的经历描述
    }>;
    education: Array<{
      school: string;
      degree: string;
      field: string;
      graduationDate: string;
    }>;
    skills: {
      technical: string[];
      soft: string[];
    };
  };
  htmlPreview: string;     // HTML 预览
  downloadUrl?: string;    // PDF 下载链接
  createdAt: string;
}
```

---

## 4. 详细实现

### 4.1 Controller

```java
@RestController
@RequestMapping("/resume-builder")
@RequiredArgsConstructor
public class ResumeBuilderController {

    private final ResumeBuilderService resumeBuilderService;

    /**
     * 获取可用模板列表
     * GET /api/resume-builder/templates
     */
    @GetMapping("/templates")
    public ApiResponse<List<ResumeTemplateDto>> getTemplates(
            @RequestParam(required = false) String category) {
        List<ResumeTemplateDto> templates = resumeBuilderService.getTemplates(category);
        return ApiResponse.success(templates);
    }

    /**
     * AI 生成简历
     * POST /api/resume-builder/generate
     */
    @PostMapping("/generate")
    public ApiResponse<GeneratedResumeDto> generateResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerateResumeRequest request) {
        GeneratedResumeDto result = resumeBuilderService.generateResume(
                principal.getId(), request);
        return ApiResponse.success(result);
    }

    /**
     * 预览模板
     * GET /api/resume-builder/preview/{templateId}
     */
    @GetMapping("/preview/{templateId}")
    public ApiResponse<String> previewTemplate(
            @PathVariable UUID templateId) {
        String htmlPreview = resumeBuilderService.previewTemplate(templateId);
        return ApiResponse.success(htmlPreview);
    }
}
```

### 4.2 DTOs

```java
// ResumeTemplateDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeTemplateDto {
    private String id;
    private String name;
    private String description;
    private String thumbnail;
    private String category;
    private Boolean isPremium;

    public static ResumeTemplateDto fromEntity(ResumeTemplate entity) {
        return ResumeTemplateDto.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .description(entity.getDescription())
                .thumbnail(entity.getThumbnailUrl())
                .category(entity.getCategory())
                .isPremium(entity.getIsPremium())
                .build();
    }
}

// GenerateResumeRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResumeRequest {
    
    @NotBlank(message = "Template ID is required")
    private String templateId;
    
    @NotBlank(message = "Target position is required")
    @Size(max = 100, message = "Target position too long")
    private String targetPosition;
    
    @Size(max = 100, message = "Target company too long")
    private String targetCompany;
    
    private Customizations customizations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customizations {
        private String tone;     // professional, friendly, confident
        private String focus;    // technical, leadership, achievements
        private String length;   // brief, standard, detailed
    }
}

// GeneratedResumeDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedResumeDto {
    private String id;
    private ResumeContent content;
    private String htmlPreview;
    private String downloadUrl;
    private String createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeContent {
        private PersonalInfo personalInfo;
        private String summary;
        private List<ExperienceItem> experiences;
        private List<EducationItem> education;
        private SkillsGroup skills;
    }

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
    public static class ExperienceItem {
        private String company;
        private String title;
        private String startDate;
        private String endDate;
        private List<String> bullets;  // AI 优化后的 bullet points
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationItem {
        private String school;
        private String degree;
        private String field;
        private String graduationDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillsGroup {
        private List<String> technical;
        private List<String> soft;
    }
}
```

### 4.3 Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeBuilderService {

    private final ResumeTemplateRepository templateRepository;
    private final ProfileService profileService;
    private final ResumeService resumeService;
    private final AiEngineClient aiEngineClient;
    private final PdfGeneratorService pdfGeneratorService;

    /**
     * 获取模板列表
     */
    public List<ResumeTemplateDto> getTemplates(@Nullable String category) {
        List<ResumeTemplate> templates;
        if (category != null && !category.isEmpty()) {
            templates = templateRepository.findByCategoryOrderByNameAsc(category);
        } else {
            templates = templateRepository.findAllByOrderByIsPremiumAscNameAsc();
        }
        return templates.stream()
                .map(ResumeTemplateDto::fromEntity)
                .toList();
    }

    /**
     * AI 生成简历
     */
    @Transactional
    public GeneratedResumeDto generateResume(UUID userId, GenerateResumeRequest request) {
        // 1. 验证模板
        ResumeTemplate template = templateRepository.findById(
                UUID.fromString(request.getTemplateId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));

        // 2. 获取用户资料
        ProfileDto profile = profileService.getOrCreateProfile(userId);
        
        // 3. 获取最新简历内容
        ResumeDto latestResume = resumeService.getLatestResume(userId);

        // 4. 构建 AI 请求
        ResumeGenerationContext context = buildContext(profile, latestResume, request);

        // 5. 调用 AI 引擎
        GeneratedResumeDto.ResumeContent content = aiEngineClient.generateResume(context);

        // 6. 生成 HTML 预览
        String htmlPreview = renderHtmlPreview(template, content);

        // 7. 生成 PDF（异步可选）
        String downloadUrl = pdfGeneratorService.generatePdf(template, content);

        log.info("Generated resume for user: {}, template: {}", userId, template.getName());

        return GeneratedResumeDto.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .htmlPreview(htmlPreview)
                .downloadUrl(downloadUrl)
                .createdAt(Instant.now().toString())
                .build();
    }

    /**
     * 预览模板
     */
    public String previewTemplate(UUID templateId) {
        ResumeTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));

        // 返回模板的示例 HTML
        return template.getSampleHtml();
    }

    /**
     * 构建 AI 上下文
     */
    private ResumeGenerationContext buildContext(
            ProfileDto profile,
            ResumeDto latestResume,
            GenerateResumeRequest request) {

        ResumeGenerationContext.ResumeGenerationContextBuilder builder = 
                ResumeGenerationContext.builder()
                        .targetPosition(request.getTargetPosition())
                        .targetCompany(request.getTargetCompany());

        // 从 Profile 获取信息
        if (profile != null) {
            builder.headline(profile.getHeadline())
                   .summary(profile.getSummary())
                   .experiences(profile.getExperiences())
                   .skills(profile.getSkills());
        }

        // 从上传的简历补充信息
        if (latestResume != null && latestResume.getParsedContent() != null) {
            ParsedContent parsed = latestResume.getParsedContent();
            builder.personalInfo(parsed.getPersonalInfo())
                   .education(parsed.getEducation());
        }

        // 添加定制选项
        if (request.getCustomizations() != null) {
            builder.tone(request.getCustomizations().getTone())
                   .focus(request.getCustomizations().getFocus())
                   .length(request.getCustomizations().getLength());
        }

        return builder.build();
    }

    /**
     * 渲染 HTML 预览
     */
    private String renderHtmlPreview(ResumeTemplate template, 
            GeneratedResumeDto.ResumeContent content) {
        // 使用 Thymeleaf 或其他模板引擎
        Context ctx = new Context();
        ctx.setVariable("content", content);
        return templateEngine.process(template.getTemplateFile(), ctx);
    }
}
```

### 4.4 AI 引擎集成

```java
@Data
@Builder
public class ResumeGenerationContext {
    private String targetPosition;
    private String targetCompany;
    private String headline;
    private String summary;
    private List<ExperienceDto> experiences;
    private List<SkillDto> skills;
    private ParsedContent.PersonalInfo personalInfo;
    private List<ParsedContent.Education> education;
    private String tone;
    private String focus;
    private String length;
}

// AI 引擎客户端
@Slf4j
@Service
@RequiredArgsConstructor
public class AiEngineClient {

    private final ResumeBuilderGrpc.ResumeBuilderBlockingStub resumeBuilderStub;
    private final ObjectMapper objectMapper;

    /**
     * 调用 AI 生成简历
     */
    public GeneratedResumeDto.ResumeContent generateResume(ResumeGenerationContext context) {
        try {
            String contextJson = objectMapper.writeValueAsString(context);
            
            GenerateResumeRequest request = GenerateResumeRequest.newBuilder()
                    .setContextJson(contextJson)
                    .build();

            GenerateResumeResponse response = resumeBuilderStub.generate(request);

            if (response.getSuccess()) {
                return objectMapper.readValue(
                        response.getContentJson(),
                        GeneratedResumeDto.ResumeContent.class);
            } else {
                throw new BusinessException(ErrorCode.RESUME_GENERATION_FAILED,
                        response.getErrorMessage());
            }
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (JsonProcessingException e) {
            log.error("JSON processing failed", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
```

### 4.5 PDF 生成服务

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

    private final MinioClient minioClient;
    
    @Value("${minio.bucket.generated-resumes}")
    private String bucketName;

    /**
     * 生成 PDF 并上传到 MinIO
     */
    public String generatePdf(ResumeTemplate template, 
            GeneratedResumeDto.ResumeContent content) {
        try {
            // 1. 渲染 HTML
            String html = renderToHtml(template, content);

            // 2. HTML 转 PDF（使用 Flying Saucer 或 wkhtmltopdf）
            byte[] pdfBytes = convertHtmlToPdf(html);

            // 3. 上传到 MinIO
            String objectName = String.format("resumes/%s.pdf", UUID.randomUUID());
            
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                    .contentType("application/pdf")
                    .build());

            // 4. 返回预签名 URL（24 小时有效）
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(24, TimeUnit.HOURS)
                    .build());
                    
        } catch (Exception e) {
            log.error("PDF generation failed", e);
            return null;  // PDF 生成失败不阻塞主流程
        }
    }

    private byte[] convertHtmlToPdf(String html) {
        // 使用 Flying Saucer
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        renderer.createPDF(os);
        return os.toByteArray();
    }
}
```

### 4.6 Entity

```java
@Entity
@Table(name = "resume_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeTemplate extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(length = 50)
    private String category;  // professional, creative, academic, simple

    @Column(name = "template_file")
    private String templateFile;  // Thymeleaf 模板文件路径

    @Column(name = "sample_html", columnDefinition = "TEXT")
    private String sampleHtml;

    @Column(name = "is_premium")
    private Boolean isPremium = false;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
```

---

## 5. AI 引擎 Prompt 设计

```python
# ai-engine/src/agents/resumebuilder/workflow.py

RESUME_GENERATION_PROMPT = """
You are an expert resume writer. Generate a professional resume based on the following context.

Target Position: {target_position}
Target Company: {target_company}

Candidate Information:
- Current Headline: {headline}
- Summary: {summary}

Work Experience:
{experiences}

Skills:
{skills}

Education:
{education}

Customization:
- Tone: {tone}
- Focus: {focus}
- Length: {length}

Instructions:
1. Write a compelling professional summary tailored to the target position
2. Rewrite each experience with quantified achievements using STAR method
3. Use strong action verbs and industry keywords
4. Highlight skills relevant to the target position
5. Keep the format clean and ATS-friendly

Output the resume content in the following JSON format:
{{
    "personalInfo": {{ ... }},
    "summary": "...",
    "experiences": [
        {{
            "company": "...",
            "title": "...",
            "startDate": "...",
            "endDate": "...",
            "bullets": ["...", "..."]
        }}
    ],
    "education": [...],
    "skills": {{
        "technical": ["..."],
        "soft": ["..."]
    }}
}}
"""
```

---

## 6. 错误码

| 错误码 | HTTP 状态 | 描述 |
|--------|----------|------|
| 50001 | 404 | Template not found |
| 50002 | 400 | Profile is empty |
| 50003 | 500 | Resume generation failed |
| 50004 | 503 | AI service unavailable |
| 50005 | 500 | PDF generation failed |

---

## 7. 常见 Bug 及修复

### Bug 1: 空 Profile 导致生成失败
**问题**: 用户没有填写 Profile 就生成简历

**修复**: 检查必填字段
```java
if (profile.getExperiences() == null || profile.getExperiences().isEmpty()) {
    throw new BusinessException(ErrorCode.PROFILE_EMPTY, 
            "Please add at least one work experience");
}
```

### Bug 2: bullets 返回为 null
**问题**: AI 有时不返回 bullets 字段

**修复**: 设置默认值
```java
experience.setBullets(experience.getBullets() != null 
        ? experience.getBullets() : List.of());
```

### Bug 3: PDF 中文乱码
**问题**: Flying Saucer 默认不支持中文

**修复**: 配置中文字体
```java
ITextFontResolver fontResolver = renderer.getFontResolver();
fontResolver.addFont("/fonts/NotoSansCJK-Regular.ttc", 
        BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
```

### Bug 4: 模板图片404
**问题**: thumbnail URL 指向不存在的图片

**修复**: 提供默认图片
```java
public String getThumbnail() {
    return thumbnailUrl != null ? thumbnailUrl : "/images/default-template.png";
}
```

---

## 8. 测试用例

```java
@SpringBootTest
@AutoConfigureMockMvc
class ResumeBuilderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getTemplates_Success() throws Exception {
        mockMvc.perform(get("/api/resume-builder/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(userId = "test-user-id")
    void generateResume_Success() throws Exception {
        GenerateResumeRequest request = new GenerateResumeRequest();
        request.setTemplateId("template-uuid");
        request.setTargetPosition("Senior Software Engineer");
        request.setTargetCompany("Google");

        mockMvc.perform(post("/api/resume-builder/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").exists())
                .andExpect(jsonPath("$.data.htmlPreview").exists());
    }
}
```
