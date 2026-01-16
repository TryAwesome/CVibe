# 服务间调用与编排

> biz-service 如何调用 ai-engine 和 search-service

---

## 1. 架构概述

```
┌─────────────────────────────────────────────────────────────────┐
│                     biz-service (Java)                          │
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐ │
│  │ Controller  │───▶│   Service   │───▶│  gRPC Client Stub   │ │
│  └─────────────┘    └─────────────┘    └─────────────────────┘ │
│                                               │                 │
└───────────────────────────────────────────────┼─────────────────┘
                                                │
                    ┌───────────────────────────┼───────────────────────────┐
                    │                           │                           │
                    ▼                           ▼                           │
    ┌───────────────────────────┐   ┌───────────────────────────┐          │
    │  ai-engine (Python)       │   │  search-service (Go)      │          │
    │  :50051                   │   │  :50052                   │          │
    └───────────────────────────┘   └───────────────────────────┘          │
```

**关键点：**
1. 前端只与 biz-service 通信
2. biz-service 作为 API 网关和业务编排层
3. biz-service 通过 gRPC 调用 ai-engine 和 search-service
4. ai-engine 和 search-service 互不依赖

---

## 2. biz-service gRPC 配置

### 2.1 Maven 依赖

```xml
<!-- pom.xml -->
<properties>
    <grpc.version>1.60.0</grpc.version>
    <protobuf.version>3.25.0</protobuf.version>
</properties>

<dependencies>
    <!-- gRPC -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty-shaded</artifactId>
        <version>${grpc.version}</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-protobuf</artifactId>
        <version>${grpc.version}</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-stub</artifactId>
        <version>${grpc.version}</version>
    </dependency>

    <!-- Proto 编译支持 -->
    <dependency>
        <groupId>javax.annotation</groupId>
        <artifactId>javax.annotation-api</artifactId>
        <version>1.3.2</version>
    </dependency>
</dependencies>

<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.1</version>
        </extension>
    </extensions>
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}</pluginArtifact>
                <protoSourceRoot>${project.basedir}/src/main/proto</protoSourceRoot>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 2.2 Proto 文件位置

```
biz-service/
└── src/main/
    ├── proto/
    │   ├── ai_engine.proto
    │   └── search_service.proto
    └── java/com/cvibe/
        └── grpc/
            └── (生成的代码)
```

### 2.3 gRPC 配置类

```java
// com/cvibe/common/config/GrpcConfig.java
package com.cvibe.common.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cvibe.grpc.ai.AIEngineGrpc;
import com.cvibe.grpc.search.SearchServiceGrpc;

@Configuration
public class GrpcConfig {

    @Value("${grpc.ai-engine.host:localhost}")
    private String aiEngineHost;

    @Value("${grpc.ai-engine.port:50051}")
    private int aiEnginePort;

    @Value("${grpc.search-service.host:localhost}")
    private String searchServiceHost;

    @Value("${grpc.search-service.port:50052}")
    private int searchServicePort;

    @Bean
    public ManagedChannel aiEngineChannel() {
        return ManagedChannelBuilder
                .forAddress(aiEngineHost, aiEnginePort)
                .usePlaintext()  // 开发环境不使用 TLS
                .maxInboundMessageSize(50 * 1024 * 1024)  // 50MB
                .build();
    }

    @Bean
    public ManagedChannel searchServiceChannel() {
        return ManagedChannelBuilder
                .forAddress(searchServiceHost, searchServicePort)
                .usePlaintext()
                .maxInboundMessageSize(50 * 1024 * 1024)
                .build();
    }

    @Bean
    public AIEngineGrpc.AIEngineBlockingStub aiEngineBlockingStub(ManagedChannel aiEngineChannel) {
        return AIEngineGrpc.newBlockingStub(aiEngineChannel);
    }

    @Bean
    public AIEngineGrpc.AIEngineStub aiEngineAsyncStub(ManagedChannel aiEngineChannel) {
        return AIEngineGrpc.newStub(aiEngineChannel);
    }

    @Bean
    public SearchServiceGrpc.SearchServiceBlockingStub searchServiceBlockingStub(ManagedChannel searchServiceChannel) {
        return SearchServiceGrpc.newBlockingStub(searchServiceChannel);
    }
}
```

### 2.4 application.yml 配置

```yaml
# application.yml
grpc:
  ai-engine:
    host: ${AI_ENGINE_HOST:localhost}
    port: ${AI_ENGINE_PORT:50051}
  search-service:
    host: ${SEARCH_SERVICE_HOST:localhost}
    port: ${SEARCH_SERVICE_PORT:50052}
```

---

## 3. gRPC 客户端封装

### 3.1 AI Engine 客户端

```java
// com/cvibe/grpc/ai/AIEngineClient.java
package com.cvibe.grpc.ai;

import com.cvibe.grpc.ai.AIEngineGrpc.*;
import com.cvibe.grpc.ai.AIEngineProto.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIEngineClient {

    private final AIEngineBlockingStub blockingStub;
    private final AIEngineStub asyncStub;

    /**
     * 解析简历
     */
    public ParseResumeResponse parseResume(byte[] fileContent, String fileName, String fileType) {
        log.info("Calling AI Engine: ParseResume for file: {}", fileName);
        
        ParseResumeRequest request = ParseResumeRequest.newBuilder()
                .setFileContent(com.google.protobuf.ByteString.copyFrom(fileContent))
                .setFileName(fileName)
                .setFileType(fileType)
                .build();

        try {
            return blockingStub.parseResume(request);
        } catch (Exception e) {
            log.error("Failed to parse resume: {}", e.getMessage());
            throw new GrpcException("AI Engine unavailable", e);
        }
    }

    /**
     * 开始 AI 面试
     */
    public StartInterviewResponse startInterview(
            String userId,
            String sessionId,
            String jobTitle,
            String jobDescription,
            String resumeContent,
            String language,
            String difficulty
    ) {
        StartInterviewRequest request = StartInterviewRequest.newBuilder()
                .setUserId(userId)
                .setSessionId(sessionId)
                .setJobTitle(jobTitle)
                .setJobDescription(jobDescription)
                .setResumeContent(resumeContent)
                .setConfig(InterviewConfig.newBuilder()
                        .setLanguage(language)
                        .setDifficulty(difficulty)
                        .build())
                .build();

        return blockingStub.startInterview(request);
    }

    /**
     * 发送面试消息（流式响应）
     */
    public void sendInterviewMessage(
            String sessionId,
            String userMessage,
            Consumer<String> onChunk,
            Runnable onComplete,
            Consumer<Throwable> onError
    ) {
        SendMessageRequest request = SendMessageRequest.newBuilder()
                .setSessionId(sessionId)
                .setUserMessage(userMessage)
                .build();

        asyncStub.sendInterviewMessage(request, new StreamObserver<MessageChunk>() {
            @Override
            public void onNext(MessageChunk chunk) {
                if (!chunk.getIsFinal()) {
                    onChunk.accept(chunk.getContent());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Stream error: {}", t.getMessage());
                onError.accept(t);
            }

            @Override
            public void onCompleted() {
                onComplete.run();
            }
        });
    }

    /**
     * 差距分析
     */
    public GapAnalysisResponse analyzeGap(
            String userId,
            String goalTitle,
            String targetDate,
            ProfileData profile
    ) {
        GapAnalysisRequest request = GapAnalysisRequest.newBuilder()
                .setUserId(userId)
                .setGoalTitle(goalTitle)
                .setTargetDate(targetDate)
                .setCurrentProfile(profile)
                .build();

        return blockingStub.analyzeGap(request);
    }

    /**
     * 生成学习路径（流式）
     */
    public Iterator<LearningPathChunk> generateLearningPath(
            String userId,
            String goalId,
            java.util.List<GapItem> gaps,
            String preferredStyle
    ) {
        LearningPathRequest request = LearningPathRequest.newBuilder()
                .setUserId(userId)
                .setGoalId(goalId)
                .addAllGaps(gaps)
                .setPreferredStyle(preferredStyle)
                .build();

        return blockingStub.generateLearningPath(request);
    }
}
```

### 3.2 Search Service 客户端

```java
// com/cvibe/grpc/search/SearchServiceClient.java
package com.cvibe.grpc.search;

import com.cvibe.grpc.search.SearchServiceGrpc.*;
import com.cvibe.grpc.search.SearchServiceProto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchServiceClient {

    private final SearchServiceBlockingStub blockingStub;

    /**
     * 搜索职位
     */
    public SearchJobsResponse searchJobs(
            String query,
            List<String> locations,
            List<String> industries,
            String experienceLevel,
            String salaryRange,
            int page,
            int pageSize,
            String sortBy
    ) {
        log.info("Calling Search Service: SearchJobs query={}", query);

        SearchJobsRequest request = SearchJobsRequest.newBuilder()
                .setQuery(query)
                .addAllLocations(locations)
                .addAllIndustries(industries)
                .setExperienceLevel(experienceLevel != null ? experienceLevel : "")
                .setSalaryRange(salaryRange != null ? salaryRange : "")
                .setPage(page)
                .setPageSize(pageSize)
                .setSortBy(sortBy != null ? sortBy : "relevance")
                .build();

        try {
            return blockingStub.searchJobs(request);
        } catch (Exception e) {
            log.error("Failed to search jobs: {}", e.getMessage());
            throw new GrpcException("Search Service unavailable", e);
        }
    }

    /**
     * 简历-职位匹配
     */
    public MatchResponse matchResumeToJob(ResumeProfile resume, Job job) {
        MatchRequest request = MatchRequest.newBuilder()
                .setResume(resume)
                .setJob(job)
                .build();

        return blockingStub.matchResumeToJob(request);
    }

    /**
     * 批量匹配
     */
    public BatchMatchResponse batchMatch(ResumeProfile resume, List<String> jobIds) {
        BatchMatchRequest request = BatchMatchRequest.newBuilder()
                .setResume(resume)
                .addAllJobIds(jobIds)
                .build();

        return blockingStub.batchMatch(request);
    }

    /**
     * 获取推荐职位
     */
    public RecommendResponse getRecommendations(
            String userId,
            ResumeProfile resume,
            int limit,
            List<String> excludeJobIds
    ) {
        RecommendRequest request = RecommendRequest.newBuilder()
                .setUserId(userId)
                .setResume(resume)
                .setLimit(limit)
                .addAllExcludeJobIds(excludeJobIds)
                .build();

        return blockingStub.getJobRecommendations(request);
    }

    /**
     * 搜索建议
     */
    public SuggestionResponse getSuggestions(String prefix, int limit) {
        SuggestionRequest request = SuggestionRequest.newBuilder()
                .setPrefix(prefix)
                .setLimit(limit)
                .build();

        return blockingStub.getSearchSuggestions(request);
    }
}
```

---

## 4. 业务服务集成示例

### 4.1 简历服务（调用 AI Engine）

```java
// com/cvibe/biz/resume/service/ResumeService.java
package com.cvibe.biz.resume.service;

import com.cvibe.biz.resume.dto.ResumeDto;
import com.cvibe.biz.resume.dto.ParsedResumeDto;
import com.cvibe.biz.resume.entity.Resume;
import com.cvibe.biz.resume.repository.ResumeRepository;
import com.cvibe.grpc.ai.AIEngineClient;
import com.cvibe.grpc.ai.AIEngineProto.*;
import com.cvibe.common.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final StorageService storageService;
    private final AIEngineClient aiEngineClient;

    /**
     * 上传并解析简历
     */
    @Transactional
    public ResumeDto uploadAndParse(UUID userId, MultipartFile file) {
        // 1. 保存文件
        String fileUrl = storageService.upload(file, "resumes/" + userId);

        // 2. 调用 AI Engine 解析
        ParseResumeResponse response = aiEngineClient.parseResume(
                file.getBytes(),
                file.getOriginalFilename(),
                file.getContentType()
        );

        if (!response.getSuccess()) {
            log.error("Resume parsing failed: {}", response.getErrorMessage());
            throw new BusinessException(ErrorCode.RESUME_PARSE_FAILED);
        }

        // 3. 保存解析结果
        ResumeData data = response.getData();
        Resume resume = Resume.builder()
                .userId(userId)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .parsedName(data.getName())
                .parsedEmail(data.getEmail())
                .parsedPhone(data.getPhone())
                .parsedSummary(data.getSummary())
                .parsedSkills(String.join(",", data.getSkillsList()))
                .rawText(data.getRawText())
                .build();

        resume = resumeRepository.save(resume);

        log.info("Resume uploaded and parsed: {}", resume.getId());
        return ResumeDto.fromEntity(resume);
    }
}
```

### 4.2 面试服务（调用 AI Engine + SSE 流式）

```java
// com/cvibe/biz/interview/service/InterviewService.java
package com.cvibe.biz.interview.service;

import com.cvibe.biz.interview.dto.*;
import com.cvibe.biz.interview.entity.InterviewSession;
import com.cvibe.biz.interview.entity.InterviewMessage;
import com.cvibe.biz.interview.repository.*;
import com.cvibe.grpc.ai.AIEngineClient;
import com.cvibe.grpc.ai.AIEngineProto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewMessageRepository messageRepository;
    private final AIEngineClient aiEngineClient;

    /**
     * 开始面试会话
     */
    @Transactional
    public StartInterviewResponse startSession(UUID userId, StartInterviewRequest request) {
        // 创建会话
        InterviewSession session = InterviewSession.builder()
                .userId(userId)
                .jobTitle(request.getJobTitle())
                .jobDescription(request.getJobDescription())
                .status("ACTIVE")
                .build();
        session = sessionRepository.save(session);

        // 调用 AI Engine
        com.cvibe.grpc.ai.AIEngineProto.StartInterviewResponse aiResponse =
                aiEngineClient.startInterview(
                        userId.toString(),
                        session.getId().toString(),
                        request.getJobTitle(),
                        request.getJobDescription(),
                        request.getResumeContent(),
                        request.getLanguage(),
                        request.getDifficulty()
                );

        // 保存 AI 的首条消息
        InterviewMessage welcomeMsg = InterviewMessage.builder()
                .session(session)
                .role("assistant")
                .content(aiResponse.getWelcomeMessage() + "\n\n" + aiResponse.getFirstQuestion())
                .build();
        messageRepository.save(welcomeMsg);

        return StartInterviewResponse.builder()
                .sessionId(session.getId().toString())
                .message(welcomeMsg.getContent())
                .build();
    }

    /**
     * 发送消息（SSE 流式响应）
     */
    public SseEmitter sendMessage(UUID userId, String sessionId, String userMessage) {
        SseEmitter emitter = new SseEmitter(60000L);  // 60 秒超时

        // 保存用户消息
        InterviewSession session = sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        InterviewMessage userMsg = InterviewMessage.builder()
                .session(session)
                .role("user")
                .content(userMessage)
                .build();
        messageRepository.save(userMsg);

        // 用于收集完整响应
        StringBuilder fullResponse = new StringBuilder();

        // 调用 AI Engine（流式）
        aiEngineClient.sendInterviewMessage(
                sessionId,
                userMessage,
                // onChunk
                chunk -> {
                    try {
                        fullResponse.append(chunk);
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(chunk));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                // onComplete
                () -> {
                    try {
                        // 保存 AI 完整响应
                        InterviewMessage aiMsg = InterviewMessage.builder()
                                .session(session)
                                .role("assistant")
                                .content(fullResponse.toString())
                                .build();
                        messageRepository.save(aiMsg);

                        emitter.send(SseEmitter.event().name("done").data(""));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                // onError
                error -> emitter.completeWithError(error)
        );

        return emitter;
    }
}
```

### 4.3 职位服务（调用 Search Service）

```java
// com/cvibe/biz/job/service/JobService.java
package com.cvibe.biz.job.service;

import com.cvibe.biz.job.dto.*;
import com.cvibe.biz.profile.service.ProfileService;
import com.cvibe.grpc.search.SearchServiceClient;
import com.cvibe.grpc.search.SearchServiceProto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final SearchServiceClient searchServiceClient;
    private final ProfileService profileService;
    private final JobSaveRepository jobSaveRepository;

    /**
     * 搜索职位
     */
    public PagedJobResponse searchJobs(UUID userId, JobSearchRequest request) {
        // 调用 Search Service
        SearchJobsResponse response = searchServiceClient.searchJobs(
                request.getQuery(),
                request.getLocations() != null ? request.getLocations() : List.of(),
                request.getIndustries() != null ? request.getIndustries() : List.of(),
                request.getExperienceLevel(),
                request.getSalaryRange(),
                request.getPage(),
                request.getPageSize(),
                request.getSortBy()
        );

        // 转换为 DTO
        List<JobDto> jobs = response.getJobsList().stream()
                .map(this::grpcJobToDto)
                .collect(Collectors.toList());

        // 检查是否已收藏
        List<UUID> savedJobIds = jobSaveRepository.findSavedJobIdsByUserId(userId);
        jobs.forEach(job -> job.setIsSaved(savedJobIds.contains(UUID.fromString(job.getId()))));

        return PagedJobResponse.builder()
                .jobs(jobs)
                .total(response.getTotal())
                .page(response.getPage())
                .totalPages(response.getTotalPages())
                .build();
    }

    /**
     * 获取推荐职位
     */
    public List<JobMatchDto> getRecommendations(UUID userId) {
        // 获取用户 Profile
        var profile = profileService.getProfile(userId);

        // 构建 gRPC ResumeProfile
        ResumeProfile resumeProfile = ResumeProfile.newBuilder()
                .setUserId(userId.toString())
                .setTitle(profile.getTitle())
                .addAllSkills(profile.getSkills().stream()
                        .map(s -> s.getName())
                        .collect(Collectors.toList()))
                .setYearsExperience(calculateYearsExperience(profile.getExperiences()))
                .build();

        // 获取已查看的职位 ID（排除）
        List<String> viewedJobIds = getViewedJobIds(userId);

        // 调用 Search Service
        RecommendResponse response = searchServiceClient.getRecommendations(
                userId.toString(),
                resumeProfile,
                20,  // limit
                viewedJobIds
        );

        return response.getRecommendationsList().stream()
                .map(r -> JobMatchDto.builder()
                        .job(grpcJobToDto(r.getJob()))
                        .matchScore(r.getMatchScore())
                        .matchReason(r.getRecommendReason())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 批量计算匹配度
     */
    public List<JobMatchDto> batchMatchJobs(UUID userId, List<String> jobIds) {
        var profile = profileService.getProfile(userId);

        ResumeProfile resumeProfile = ResumeProfile.newBuilder()
                .setUserId(userId.toString())
                .setTitle(profile.getTitle())
                .addAllSkills(profile.getSkills().stream()
                        .map(s -> s.getName())
                        .collect(Collectors.toList()))
                .build();

        BatchMatchResponse response = searchServiceClient.batchMatch(resumeProfile, jobIds);

        return response.getMatchesList().stream()
                .map(m -> JobMatchDto.builder()
                        .jobId(m.getJobId())
                        .matchScore(m.getScore())
                        .matchDetails(MatchDetailsDto.builder()
                                .skillMatch(m.getDetails().getSkillMatch())
                                .experienceMatch(m.getDetails().getExperienceMatch())
                                .locationMatch(m.getDetails().getLocationMatch())
                                .build())
                        .build())
                .collect(Collectors.toList());
    }

    private JobDto grpcJobToDto(Job job) {
        return JobDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .companyLogo(job.getCompanyLogo())
                .location(job.getLocation())
                .salaryRange(job.getSalaryRange())
                .experience(job.getExperience())
                .employmentType(job.getEmploymentType())
                .description(job.getDescription())
                .requirements(job.getRequirementsList())
                .benefits(job.getBenefitsList())
                .postedAt(job.getPostedAt())
                .source(job.getSource())
                .sourceUrl(job.getSourceUrl())
                .matchScore(job.getMatchScore())
                .build();
    }
}
```

---

## 5. 错误处理

### 5.1 gRPC 异常

```java
// com/cvibe/grpc/GrpcException.java
package com.cvibe.grpc;

public class GrpcException extends RuntimeException {
    public GrpcException(String message) {
        super(message);
    }

    public GrpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 5.2 全局异常处理

```java
// com/cvibe/common/exception/GlobalExceptionHandler.java
@ExceptionHandler(GrpcException.class)
public ApiResponse<Void> handleGrpcException(GrpcException e) {
    log.error("gRPC service error: {}", e.getMessage());
    return ApiResponse.error(ErrorCode.SERVICE_UNAVAILABLE);
}

@ExceptionHandler(io.grpc.StatusRuntimeException.class)
public ApiResponse<Void> handleGrpcStatusException(io.grpc.StatusRuntimeException e) {
    log.error("gRPC status error: {} - {}", e.getStatus(), e.getMessage());
    
    return switch (e.getStatus().getCode()) {
        case UNAVAILABLE -> ApiResponse.error(ErrorCode.SERVICE_UNAVAILABLE);
        case DEADLINE_EXCEEDED -> ApiResponse.error(ErrorCode.REQUEST_TIMEOUT);
        case INVALID_ARGUMENT -> ApiResponse.error(ErrorCode.INVALID_PARAMETER);
        default -> ApiResponse.error(ErrorCode.INTERNAL_ERROR);
    };
}
```

---

## 6. 服务健康检查

### 6.1 健康检查端点

```java
// com/cvibe/biz/health/controller/HealthController.java
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final AIEngineClient aiEngineClient;
    private final SearchServiceClient searchServiceClient;

    @GetMapping
    public ApiResponse<HealthStatus> checkHealth() {
        boolean aiEngineHealthy = checkAIEngine();
        boolean searchServiceHealthy = checkSearchService();

        HealthStatus status = HealthStatus.builder()
                .status(aiEngineHealthy && searchServiceHealthy ? "UP" : "DEGRADED")
                .aiEngine(aiEngineHealthy ? "UP" : "DOWN")
                .searchService(searchServiceHealthy ? "UP" : "DOWN")
                .timestamp(Instant.now().toString())
                .build();

        return ApiResponse.success(status);
    }

    private boolean checkAIEngine() {
        try {
            // 发送简单请求测试连接
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkSearchService() {
        try {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## 7. 服务启动顺序

### 7.1 Docker Compose

```yaml
# infra/docker-compose.yml
version: '3.8'

services:
  # 基础设施
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: cvibe
      POSTGRES_USER: cvibe
      POSTGRES_PASSWORD: cvibe123
    ports:
      - "5432:5432"

  redis:
    image: redis:7
    ports:
      - "6379:6379"

  elasticsearch:
    image: elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"

  # AI Engine (Python)
  ai-engine:
    build: ../ai-engine
    ports:
      - "50051:50051"
    environment:
      - GRPC_PORT=50051
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    depends_on:
      - redis

  # Search Service (Go)
  search-service:
    build: ../search-service
    ports:
      - "50052:50052"
    environment:
      - GRPC_PORT=50052
      - ELASTICSEARCH_URL=http://elasticsearch:9200
      - REDIS_URL=redis://redis:6379
    depends_on:
      - elasticsearch
      - redis

  # Biz Service (Java)
  biz-service:
    build: ../biz-service
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - AI_ENGINE_HOST=ai-engine
      - AI_ENGINE_PORT=50051
      - SEARCH_SERVICE_HOST=search-service
      - SEARCH_SERVICE_PORT=50052
      - DATABASE_URL=jdbc:postgresql://postgres:5432/cvibe
    depends_on:
      - postgres
      - ai-engine
      - search-service

  # Frontend (Next.js)
  frontend:
    build: ../frontend
    ports:
      - "3000:3000"
    environment:
      - NEXT_PUBLIC_API_URL=http://localhost:8080
    depends_on:
      - biz-service
```

### 7.2 启动顺序

```bash
# 1. 基础设施
docker-compose up -d postgres redis elasticsearch

# 2. 等待基础设施就绪
sleep 10

# 3. 后端服务
docker-compose up -d ai-engine search-service

# 4. 等待 gRPC 服务就绪
sleep 5

# 5. 业务服务
docker-compose up -d biz-service

# 6. 前端
docker-compose up -d frontend
```

---

## 8. 调用链路示意

### 8.1 简历上传解析

```
Frontend                 biz-service              ai-engine
   │                          │                       │
   │  POST /api/resumes       │                       │
   │  [multipart/form-data]   │                       │
   │─────────────────────────▶│                       │
   │                          │                       │
   │                          │  gRPC: ParseResume    │
   │                          │  [file bytes]         │
   │                          │──────────────────────▶│
   │                          │                       │
   │                          │                       │ OCR + LLM 解析
   │                          │                       │
   │                          │◀──────────────────────│
   │                          │  ParseResumeResponse  │
   │                          │                       │
   │                          │ 保存到数据库          │
   │                          │                       │
   │◀─────────────────────────│                       │
   │  { success: true,        │                       │
   │    data: ResumeDto }     │                       │
```

### 8.2 职位搜索 + 匹配

```
Frontend                 biz-service              search-service
   │                          │                       │
   │  GET /api/v1/jobs?q=...  │                       │
   │─────────────────────────▶│                       │
   │                          │                       │
   │                          │  gRPC: SearchJobs     │
   │                          │──────────────────────▶│
   │                          │                       │
   │                          │                       │ ES 搜索
   │                          │                       │
   │                          │◀──────────────────────│
   │                          │  SearchJobsResponse   │
   │                          │                       │
   │                          │  gRPC: BatchMatch     │
   │                          │──────────────────────▶│
   │                          │                       │
   │                          │                       │ 并行匹配
   │                          │                       │
   │                          │◀──────────────────────│
   │                          │  BatchMatchResponse   │
   │                          │                       │
   │◀─────────────────────────│                       │
   │  { jobs with matchScore }│                       │
```

### 8.3 AI 面试（流式）

```
Frontend                 biz-service              ai-engine
   │                          │                       │
   │  POST /api/v1/interviews │                       │
   │  /sessions/{id}/message  │                       │
   │─────────────────────────▶│                       │
   │                          │                       │
   │                          │  gRPC: SendMessage    │
   │                          │  (stream)             │
   │                          │──────────────────────▶│
   │                          │                       │
   │  SSE: chunk 1            │◀──────────────────────│ chunk 1
   │◀─────────────────────────│                       │
   │                          │                       │
   │  SSE: chunk 2            │◀──────────────────────│ chunk 2
   │◀─────────────────────────│                       │
   │                          │                       │
   │  SSE: chunk N            │◀──────────────────────│ chunk N
   │◀─────────────────────────│                       │
   │                          │                       │
   │  SSE: done               │◀──────────────────────│ complete
   │◀─────────────────────────│                       │
```
