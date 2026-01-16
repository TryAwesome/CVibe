# Job 模块详细设计

> 职位搜索、推荐和匹配模块

---

## 1. 模块结构

```
biz/job/
├── controller/
│   └── JobController.java
├── service/
│   └── JobService.java
├── repository/
│   ├── JobRepository.java
│   └── JobMatchRepository.java
├── entity/
│   ├── Job.java
│   └── JobMatch.java
└── dto/
    ├── JobDto.java
    ├── JobMatchDto.java
    └── JobSearchRequest.java
```

---

## 2. API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/jobs` | 搜索职位 |
| GET | `/api/v1/jobs/{jobId}` | 获取职位详情 |
| GET | `/api/v1/jobs/recommended` | 获取推荐职位 |
| GET | `/api/v1/jobs/matches` | 获取匹配记录 |
| POST | `/api/v1/jobs/{jobId}/save` | 收藏职位 |
| DELETE | `/api/v1/jobs/{jobId}/save` | 取消收藏 |
| GET | `/api/v1/jobs/saved` | 获取收藏列表 |

---

## 3. 前端期望的数据结构

### 3.1 Job

```typescript
interface Job {
  id: string;
  title: string;
  company: string;
  companyLogo?: string;
  location: string;
  type: 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'INTERNSHIP' | 'REMOTE';
  salary?: {
    min: number;
    max: number;
    currency: string;
    period: 'HOURLY' | 'MONTHLY' | 'YEARLY';
  };
  description: string;
  requirements: string[];
  responsibilities: string[];
  benefits?: string[];
  skills: string[];
  experienceLevel: 'ENTRY' | 'MID' | 'SENIOR' | 'LEAD' | 'EXECUTIVE';
  postedAt: string;
  deadline?: string;
  source: string;           // "BOSS", "LAGOU", "LINKEDIN"
  sourceUrl?: string;
  isSaved?: boolean;
  matchScore?: number;      // 0-100, AI 计算的匹配度
}
```

### 3.2 JobMatch

```typescript
interface JobMatch {
  id: string;
  userId: string;
  job: Job;
  matchScore: number;
  matchDetails: {
    skillMatch: number;
    experienceMatch: number;
    locationMatch: number;
    salaryMatch: number;
  };
  matchedSkills: string[];
  missingSkills: string[];
  recommendations: string[];
  createdAt: string;
}
```

### 3.3 搜索请求

```typescript
interface JobSearchRequest {
  keyword?: string;
  location?: string;
  type?: string;            // FULL_TIME, PART_TIME, etc.
  experienceLevel?: string; // ENTRY, MID, SENIOR
  salaryMin?: number;
  salaryMax?: number;
  skills?: string[];
  company?: string;
  source?: string;          // BOSS, LAGOU
  page?: number;
  size?: number;
  sortBy?: 'relevance' | 'date' | 'salary';
}
```

---

## 4. 详细实现

### 4.1 Controller

```java
@RestController
@RequestMapping("/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /**
     * 搜索职位
     * GET /api/v1/jobs
     */
    @GetMapping
    public ApiResponse<PagedResponse<JobDto>> searchJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) Integer salaryMin,
            @RequestParam(required = false) Integer salaryMax,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "relevance") String sortBy) {

        JobSearchRequest request = JobSearchRequest.builder()
                .keyword(keyword)
                .location(location)
                .type(type)
                .experienceLevel(experienceLevel)
                .salaryMin(salaryMin)
                .salaryMax(salaryMax)
                .skills(skills)
                .company(company)
                .source(source)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .build();

        PagedResponse<JobDto> jobs = jobService.searchJobs(
                principal != null ? principal.getId() : null, request);
        return ApiResponse.success(jobs);
    }

    /**
     * 获取职位详情
     * GET /api/v1/jobs/{jobId}
     */
    @GetMapping("/{jobId}")
    public ApiResponse<JobDto> getJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {
        JobDto job = jobService.getJob(
                principal != null ? principal.getId() : null, jobId);
        return ApiResponse.success(job);
    }

    /**
     * 获取推荐职位
     * GET /api/v1/jobs/recommended
     */
    @GetMapping("/recommended")
    public ApiResponse<List<JobMatchDto>> getRecommendedJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "10") int limit) {
        List<JobMatchDto> jobs = jobService.getRecommendedJobs(principal.getId(), limit);
        return ApiResponse.success(jobs);
    }

    /**
     * 获取匹配记录
     * GET /api/v1/jobs/matches
     */
    @GetMapping("/matches")
    public ApiResponse<PagedResponse<JobMatchDto>> getMatches(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<JobMatchDto> matches = jobService.getMatches(
                principal.getId(), page, size);
        return ApiResponse.success(matches);
    }

    /**
     * 收藏职位
     * POST /api/v1/jobs/{jobId}/save
     */
    @PostMapping("/{jobId}/save")
    public ApiResponse<Void> saveJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {
        jobService.saveJob(principal.getId(), jobId);
        return ApiResponse.success();
    }

    /**
     * 取消收藏
     * DELETE /api/v1/jobs/{jobId}/save
     */
    @DeleteMapping("/{jobId}/save")
    public ApiResponse<Void> unsaveJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {
        jobService.unsaveJob(principal.getId(), jobId);
        return ApiResponse.success();
    }

    /**
     * 获取收藏列表
     * GET /api/v1/jobs/saved
     */
    @GetMapping("/saved")
    public ApiResponse<PagedResponse<JobDto>> getSavedJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<JobDto> jobs = jobService.getSavedJobs(
                principal.getId(), page, size);
        return ApiResponse.success(jobs);
    }
}
```

### 4.2 DTOs

```java
// JobDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {
    private String id;
    private String title;
    private String company;
    private String companyLogo;
    private String location;
    private String type;
    private SalaryDto salary;
    private String description;
    private List<String> requirements;
    private List<String> responsibilities;
    private List<String> benefits;
    private List<String> skills;
    private String experienceLevel;
    private String postedAt;
    private String deadline;
    private String source;
    private String sourceUrl;
    private Boolean isSaved;
    private Integer matchScore;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaryDto {
        private Integer min;
        private Integer max;
        private String currency;
        private String period;
    }

    public static JobDto fromEntity(Job entity, boolean isSaved, Integer matchScore) {
        JobDtoBuilder builder = JobDto.builder()
                .id(entity.getId().toString())
                .title(entity.getTitle())
                .company(entity.getCompany())
                .companyLogo(entity.getCompanyLogo())
                .location(entity.getLocation())
                .type(entity.getType() != null ? entity.getType().name() : null)
                .description(entity.getDescription())
                .requirements(parseJsonArray(entity.getRequirements()))
                .responsibilities(parseJsonArray(entity.getResponsibilities()))
                .benefits(parseJsonArray(entity.getBenefits()))
                .skills(parseJsonArray(entity.getSkills()))
                .experienceLevel(entity.getExperienceLevel() != null 
                        ? entity.getExperienceLevel().name() : null)
                .postedAt(entity.getPostedAt().toString())
                .source(entity.getSource())
                .sourceUrl(entity.getSourceUrl())
                .isSaved(isSaved)
                .matchScore(matchScore);

        // 薪资信息
        if (entity.getSalaryMin() != null || entity.getSalaryMax() != null) {
            builder.salary(SalaryDto.builder()
                    .min(entity.getSalaryMin())
                    .max(entity.getSalaryMax())
                    .currency(entity.getSalaryCurrency())
                    .period(entity.getSalaryPeriod())
                    .build());
        }

        if (entity.getDeadline() != null) {
            builder.deadline(entity.getDeadline().toString());
        }

        return builder.build();
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

// JobMatchDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchDto {
    private String id;
    private String userId;
    private JobDto job;
    private Integer matchScore;
    private MatchDetails matchDetails;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> recommendations;
    private String createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchDetails {
        private Integer skillMatch;
        private Integer experienceMatch;
        private Integer locationMatch;
        private Integer salaryMatch;
    }

    public static JobMatchDto fromEntity(JobMatch entity) {
        return JobMatchDto.builder()
                .id(entity.getId().toString())
                .userId(entity.getUser().getId().toString())
                .job(JobDto.fromEntity(entity.getJob(), false, entity.getMatchScore()))
                .matchScore(entity.getMatchScore())
                .matchDetails(parseMatchDetails(entity.getMatchDetailsJson()))
                .matchedSkills(parseJsonArray(entity.getMatchedSkillsJson()))
                .missingSkills(parseJsonArray(entity.getMissingSkillsJson()))
                .recommendations(parseJsonArray(entity.getRecommendationsJson()))
                .createdAt(entity.getCreatedAt().toString())
                .build();
    }

    private static MatchDetails parseMatchDetails(String json) {
        if (json == null) return null;
        try {
            return new ObjectMapper().readValue(json, MatchDetails.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> parseJsonArray(String json) {
        if (json == null) return List.of();
        try {
            return new ObjectMapper().readValue(json, 
                    new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}

// JobSearchRequest.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchRequest {
    private String keyword;
    private String location;
    private String type;
    private String experienceLevel;
    private Integer salaryMin;
    private Integer salaryMax;
    private List<String> skills;
    private String company;
    private String source;
    private int page = 0;
    private int size = 20;
    private String sortBy = "relevance";
}

// PagedResponse.java
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
    private boolean hasNext;
    private boolean hasPrevious;
}
```

### 4.3 Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobMatchRepository matchRepository;
    private final JobSaveRepository saveRepository;
    private final ProfileService profileService;
    private final SearchServiceClient searchServiceClient;
    private final AiEngineClient aiEngineClient;
    private final ObjectMapper objectMapper;

    /**
     * 搜索职位
     */
    @Transactional(readOnly = true)
    public PagedResponse<JobDto> searchJobs(UUID userId, JobSearchRequest request) {
        // 调用 search-service 进行搜索
        SearchResult result = searchServiceClient.searchJobs(request);

        // 获取用户收藏的职位 ID
        Set<UUID> savedJobIds = userId != null 
                ? saveRepository.findSavedJobIdsByUserId(userId) 
                : Set.of();

        // 转换结果
        List<JobDto> jobs = result.getJobs().stream()
                .map(job -> JobDto.fromEntity(job, 
                        savedJobIds.contains(job.getId()), null))
                .toList();

        return PagedResponse.<JobDto>builder()
                .content(jobs)
                .page(request.getPage())
                .size(request.getSize())
                .totalElements(result.getTotalCount())
                .totalPages((int) Math.ceil((double) result.getTotalCount() / request.getSize()))
                .hasNext(request.getPage() < (int) Math.ceil((double) result.getTotalCount() / request.getSize()) - 1)
                .hasPrevious(request.getPage() > 0)
                .build();
    }

    /**
     * 获取职位详情
     */
    @Transactional(readOnly = true)
    public JobDto getJob(UUID userId, UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));

        boolean isSaved = userId != null && saveRepository.existsByUserIdAndJobId(userId, jobId);

        // 计算匹配分数
        Integer matchScore = null;
        if (userId != null) {
            matchScore = calculateMatchScore(userId, job);
        }

        return JobDto.fromEntity(job, isSaved, matchScore);
    }

    /**
     * 获取推荐职位
     */
    @Transactional
    public List<JobMatchDto> getRecommendedJobs(UUID userId, int limit) {
        // 获取用户资料
        ProfileDto profile = profileService.getOrCreateProfile(userId);

        // 提取用户技能
        List<String> userSkills = profile.getSkills() != null
                ? profile.getSkills().stream().map(SkillDto::getName).toList()
                : List.of();

        // 调用 search-service 获取推荐职位
        List<Job> recommendedJobs = searchServiceClient.getRecommendedJobs(
                userSkills, 
                profile.getHeadline(),
                limit
        );

        // 计算匹配详情
        List<JobMatchDto> matches = new ArrayList<>();
        for (Job job : recommendedJobs) {
            JobMatchDto match = calculateAndSaveMatch(userId, job, profile);
            matches.add(match);
        }

        return matches;
    }

    /**
     * 获取匹配记录
     */
    @Transactional(readOnly = true)
    public PagedResponse<JobMatchDto> getMatches(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by("matchScore").descending());
        
        Page<JobMatch> matchPage = matchRepository.findByUserId(userId, pageable);

        List<JobMatchDto> matches = matchPage.getContent().stream()
                .map(JobMatchDto::fromEntity)
                .toList();

        return PagedResponse.<JobMatchDto>builder()
                .content(matches)
                .page(page)
                .size(size)
                .totalElements(matchPage.getTotalElements())
                .totalPages(matchPage.getTotalPages())
                .hasNext(matchPage.hasNext())
                .hasPrevious(matchPage.hasPrevious())
                .build();
    }

    /**
     * 收藏职位
     */
    @Transactional
    public void saveJob(UUID userId, UUID jobId) {
        if (saveRepository.existsByUserIdAndJobId(userId, jobId)) {
            return;  // 已收藏，幂等操作
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        JobSave save = JobSave.builder()
                .user(user)
                .job(job)
                .build();

        saveRepository.save(save);
        log.info("User {} saved job {}", userId, jobId);
    }

    /**
     * 取消收藏
     */
    @Transactional
    public void unsaveJob(UUID userId, UUID jobId) {
        saveRepository.deleteByUserIdAndJobId(userId, jobId);
        log.info("User {} unsaved job {}", userId, jobId);
    }

    /**
     * 获取收藏列表
     */
    @Transactional(readOnly = true)
    public PagedResponse<JobDto> getSavedJobs(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by("createdAt").descending());

        Page<JobSave> savePage = saveRepository.findByUserId(userId, pageable);

        List<JobDto> jobs = savePage.getContent().stream()
                .map(save -> JobDto.fromEntity(save.getJob(), true, null))
                .toList();

        return PagedResponse.<JobDto>builder()
                .content(jobs)
                .page(page)
                .size(size)
                .totalElements(savePage.getTotalElements())
                .totalPages(savePage.getTotalPages())
                .hasNext(savePage.hasNext())
                .hasPrevious(savePage.hasPrevious())
                .build();
    }

    // ==================== Helper ====================

    private Integer calculateMatchScore(UUID userId, Job job) {
        ProfileDto profile = profileService.getOrCreateProfile(userId);
        return aiEngineClient.calculateJobMatchScore(profile, job);
    }

    private JobMatchDto calculateAndSaveMatch(UUID userId, Job job, ProfileDto profile) {
        // 调用 AI 引擎计算详细匹配
        JobMatchResult result = aiEngineClient.analyzeJobMatch(profile, job);

        // 查找或创建匹配记录
        JobMatch match = matchRepository.findByUserIdAndJobId(userId, job.getId())
                .orElse(JobMatch.builder()
                        .user(userRepository.getReferenceById(userId))
                        .job(job)
                        .build());

        match.setMatchScore(result.getOverallScore());
        match.setMatchDetailsJson(toJson(result.getMatchDetails()));
        match.setMatchedSkillsJson(toJson(result.getMatchedSkills()));
        match.setMissingSkillsJson(toJson(result.getMissingSkills()));
        match.setRecommendationsJson(toJson(result.getRecommendations()));

        match = matchRepository.save(match);
        return JobMatchDto.fromEntity(match);
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

### 4.4 与 Search Service 的集成

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceClient {

    private final JobSearchGrpc.JobSearchBlockingStub searchStub;

    /**
     * 搜索职位
     */
    public SearchResult searchJobs(JobSearchRequest request) {
        try {
            SearchJobsRequest.Builder builder = SearchJobsRequest.newBuilder()
                    .setPage(request.getPage())
                    .setSize(request.getSize())
                    .setSortBy(request.getSortBy());

            if (request.getKeyword() != null) {
                builder.setKeyword(request.getKeyword());
            }
            if (request.getLocation() != null) {
                builder.setLocation(request.getLocation());
            }
            if (request.getType() != null) {
                builder.setType(request.getType());
            }
            if (request.getSkills() != null) {
                builder.addAllSkills(request.getSkills());
            }

            SearchJobsResponse response = searchStub.search(builder.build());

            // 转换结果
            List<Job> jobs = response.getJobsList().stream()
                    .map(this::convertToJob)
                    .toList();

            return SearchResult.builder()
                    .jobs(jobs)
                    .totalCount(response.getTotalCount())
                    .build();

        } catch (StatusRuntimeException e) {
            log.error("Search service call failed", e);
            throw new BusinessException(ErrorCode.SEARCH_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 获取推荐职位
     */
    public List<Job> getRecommendedJobs(List<String> skills, String headline, int limit) {
        try {
            GetRecommendedRequest request = GetRecommendedRequest.newBuilder()
                    .addAllSkills(skills)
                    .setHeadline(headline != null ? headline : "")
                    .setLimit(limit)
                    .build();

            GetRecommendedResponse response = searchStub.getRecommended(request);

            return response.getJobsList().stream()
                    .map(this::convertToJob)
                    .toList();

        } catch (StatusRuntimeException e) {
            log.error("Get recommended jobs failed", e);
            return List.of();
        }
    }

    private Job convertToJob(JobProto proto) {
        // 从 proto 转换为 Job entity
        return Job.builder()
                .id(UUID.fromString(proto.getId()))
                .title(proto.getTitle())
                .company(proto.getCompany())
                .location(proto.getLocation())
                // ... 其他字段
                .build();
    }
}
```

### 4.5 Entity

```java
// Job.java
@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(name = "company_logo")
    private String companyLogo;

    private String location;

    @Enumerated(EnumType.STRING)
    private JobType type;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "salary_currency")
    private String salaryCurrency;

    @Column(name = "salary_period")
    private String salaryPeriod;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;  // JSON array

    @Column(columnDefinition = "TEXT")
    private String responsibilities;  // JSON array

    @Column(columnDefinition = "TEXT")
    private String benefits;  // JSON array

    @Column(columnDefinition = "TEXT")
    private String skills;  // JSON array

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level")
    private ExperienceLevel experienceLevel;

    @Column(name = "posted_at")
    private Instant postedAt;

    private LocalDate deadline;

    @Column(nullable = false)
    private String source;  // BOSS, LAGOU, etc.

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "source_id")
    private String sourceId;  // 原平台的 ID，用于去重
}

// JobMatch.java
@Entity
@Table(name = "job_matches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "match_details_json", columnDefinition = "TEXT")
    private String matchDetailsJson;

    @Column(name = "matched_skills_json", columnDefinition = "TEXT")
    private String matchedSkillsJson;

    @Column(name = "missing_skills_json", columnDefinition = "TEXT")
    private String missingSkillsJson;

    @Column(name = "recommendations_json", columnDefinition = "TEXT")
    private String recommendationsJson;
}

// JobSave.java
@Entity
@Table(name = "job_saves", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "job_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSave extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
}

public enum JobType {
    FULL_TIME,
    PART_TIME,
    CONTRACT,
    INTERNSHIP,
    REMOTE
}

public enum ExperienceLevel {
    ENTRY,
    MID,
    SENIOR,
    LEAD,
    EXECUTIVE
}
```

---

## 5. 错误码

| 错误码 | HTTP 状态 | 描述 |
|--------|----------|------|
| 90001 | 404 | Job not found |
| 90002 | 400 | Invalid search parameters |
| 90003 | 503 | Search service unavailable |
| 90004 | 503 | AI service unavailable |

---

## 6. 常见 Bug 及修复

### Bug 1: 分页参数错误
**问题**: page 从 1 开始还是从 0 开始不一致

**修复**: 统一使用 0-based
```java
@RequestParam(defaultValue = "0") int page
```

### Bug 2: 收藏状态不一致
**问题**: 搜索结果中 isSaved 状态不正确

**修复**: 批量查询收藏状态
```java
Set<UUID> savedJobIds = saveRepository.findSavedJobIdsByUserId(userId);
```

### Bug 3: 空列表返回 null
**问题**: skills 等数组字段为 null 而非空数组

**修复**: 使用空数组
```java
.skills(parseJsonArray(entity.getSkills()))  // 返回 List.of() 而非 null
```

---

## 7. 测试用例

```java
@SpringBootTest
@AutoConfigureMockMvc
class JobControllerTest {

    @Test
    void searchJobs_Success() throws Exception {
        mockMvc.perform(get("/api/v1/jobs")
                .param("keyword", "Java")
                .param("location", "北京")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser(userId = "test-user-id")
    void saveJob_Success() throws Exception {
        UUID jobId = createTestJob();

        mockMvc.perform(post("/api/v1/jobs/{jobId}/save", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证收藏状态
        mockMvc.perform(get("/api/v1/jobs/{jobId}", jobId))
                .andExpect(jsonPath("$.data.isSaved").value(true));
    }
}
```
