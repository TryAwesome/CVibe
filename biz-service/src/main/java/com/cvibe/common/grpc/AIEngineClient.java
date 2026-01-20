package com.cvibe.common.grpc;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * AI Engine gRPC 客户端
 * 
 * 封装对 ai-engine 服务的调用。
 * 当 gRPC 服务不可用时，返回 Mock 数据。
 */
@Slf4j
@Component
public class AIEngineClient {

    private final GrpcConfig grpcConfig;
    private com.cvibe.grpc.ai.AIEngineGrpc.AIEngineBlockingStub blockingStub;
    private com.cvibe.grpc.ai.AIEngineGrpc.AIEngineStub asyncStub;

    public AIEngineClient(GrpcConfig grpcConfig) {
        this.grpcConfig = grpcConfig;
        initStubs();
    }

    private void initStubs() {
        try {
            ManagedChannel channel = grpcConfig.aiEngineChannel();
            if (channel != null) {
                // 不在这里设置 deadline，而是在每次调用时设置
                this.blockingStub = com.cvibe.grpc.ai.AIEngineGrpc.newBlockingStub(channel);
                this.asyncStub = com.cvibe.grpc.ai.AIEngineGrpc.newStub(channel);
                log.info("AI Engine gRPC stubs initialized");
            }
        } catch (Exception e) {
            log.warn("Failed to initialize AI Engine gRPC stubs: {}", e.getMessage());
        }
    }

    // 检查 gRPC 是否可用
    private boolean isAvailable() {
        return blockingStub != null;
    }

    // 获取带有超时设置的 stub (5 minutes for slow models like DeepSeek-R1)
    private com.cvibe.grpc.ai.AIEngineGrpc.AIEngineBlockingStub getStubWithDeadline() {
        return blockingStub.withDeadlineAfter(300, TimeUnit.SECONDS);
    }

    /**
     * 解析简历 - 使用 VLM 提取详细结构化数据
     */
    public ResumeParseResult parseResume(byte[] fileContent, String fileName, String fileType) {
        log.info("AI Engine: ParseResume for file: {}, type: {}, size: {} bytes", 
                fileName, fileType, fileContent.length);
        
        if (!isAvailable()) {
            log.warn("AI Engine not available, returning mock data");
            return mockParseResume(fileName);
        }

        try {
            // 构建请求
            com.cvibe.grpc.ai.ParseResumeRequest request = com.cvibe.grpc.ai.ParseResumeRequest.newBuilder()
                    .setFileContent(ByteString.copyFrom(fileContent))
                    .setFileName(fileName)
                    .setFileType(fileType)
                    .build();

            // 调用 gRPC - 每次调用时设置新的 deadline
            com.cvibe.grpc.ai.ParseResumeResponse response = getStubWithDeadline().parseResume(request);

            if (!response.getSuccess()) {
                log.error("AI Engine ParseResume failed: {}", response.getErrorMessage());
                return ResumeParseResult.builder()
                        .success(false)
                        .errorMessage(response.getErrorMessage())
                        .build();
            }

            // 转换响应
            com.cvibe.grpc.ai.ResumeData data = response.getData();
            
            ResumeParseResult result = ResumeParseResult.builder()
                    .success(true)
                    // 个人信息
                    .name(data.getName())
                    .email(data.getEmail())
                    .phone(data.getPhone())
                    .linkedin(data.getLinkedin())
                    .github(data.getGithub())
                    .website(data.getWebsite())
                    .location(data.getLocation())
                    // 概要
                    .headline(data.getHeadline())
                    .summary(data.getSummary())
                    .rawText(data.getRawText())
                    .build();

            // 转换技能
            List<SkillData> skills = new ArrayList<>();
            for (com.cvibe.grpc.ai.SkillData skill : data.getSkillsList()) {
                skills.add(SkillData.builder()
                        .name(skill.getName())
                        .level(skill.getLevel())
                        .category(skill.getCategory())
                        .build());
            }
            result.setSkills(skills);

            // 转换工作经历
            List<ExperienceData> experiences = new ArrayList<>();
            for (com.cvibe.grpc.ai.ExperienceData exp : data.getExperiencesList()) {
                experiences.add(ExperienceData.builder()
                        .company(exp.getCompany())
                        .title(exp.getTitle())
                        .location(exp.getLocation())
                        .employmentType(exp.getEmploymentType())
                        .startDate(exp.getStartDate())
                        .endDate(exp.getEndDate())
                        .isCurrent(exp.getIsCurrent())
                        .description(exp.getDescription())
                        .achievements(new ArrayList<>(exp.getAchievementsList()))
                        .technologies(new ArrayList<>(exp.getTechnologiesList()))
                        .build());
            }
            result.setExperiences(experiences);

            // 转换教育经历
            List<EducationData> educations = new ArrayList<>();
            for (com.cvibe.grpc.ai.EducationData edu : data.getEducationsList()) {
                educations.add(EducationData.builder()
                        .school(edu.getSchool())
                        .degree(edu.getDegree())
                        .field(edu.getField())
                        .location(edu.getLocation())
                        .startDate(edu.getStartDate())
                        .endDate(edu.getEndDate())
                        .gpa(edu.getGpa())
                        .description(edu.getDescription())
                        .activities(new ArrayList<>(edu.getActivitiesList()))
                        .honors(new ArrayList<>(edu.getHonorsList()))
                        .build());
            }
            result.setEducations(educations);

            // 转换项目经历
            List<ProjectData> projects = new ArrayList<>();
            for (com.cvibe.grpc.ai.ProjectData proj : data.getProjectsList()) {
                projects.add(ProjectData.builder()
                        .name(proj.getName())
                        .description(proj.getDescription())
                        .url(proj.getUrl())
                        .repoUrl(proj.getRepoUrl())
                        .technologies(new ArrayList<>(proj.getTechnologiesList()))
                        .startDate(proj.getStartDate())
                        .endDate(proj.getEndDate())
                        .highlights(new ArrayList<>(proj.getHighlightsList()))
                        .build());
            }
            result.setProjects(projects);

            // 转换证书
            List<CertificationData> certifications = new ArrayList<>();
            for (com.cvibe.grpc.ai.CertificationData cert : data.getCertificationsList()) {
                certifications.add(CertificationData.builder()
                        .name(cert.getName())
                        .issuer(cert.getIssuer())
                        .date(cert.getDate())
                        .url(cert.getUrl())
                        .build());
            }
            result.setCertifications(certifications);

            // 转换语言能力
            List<LanguageData> languages = new ArrayList<>();
            for (com.cvibe.grpc.ai.LanguageData lang : data.getLanguagesList()) {
                languages.add(LanguageData.builder()
                        .language(lang.getLanguage())
                        .proficiency(lang.getProficiency())
                        .build());
            }
            result.setLanguages(languages);

            // 成就
            result.setAchievements(new ArrayList<>(data.getAchievementsList()));

            log.info("AI Engine ParseResume success: name={}, skills={}, exp={}, edu={}, projects={}",
                    result.getName(), 
                    result.getSkills().size(),
                    result.getExperiences().size(),
                    result.getEducations().size(),
                    result.getProjects().size());

            return result;

        } catch (StatusRuntimeException e) {
            log.error("AI Engine gRPC call failed: {}", e.getStatus(), e);
            return mockParseResume(fileName);
        } catch (Exception e) {
            log.error("AI Engine ParseResume error", e);
            return mockParseResume(fileName);
        }
    }

    /**
     * 开始 AI 面试
     */
    public StartInterviewResult startInterview(
            String userId,
            String sessionId,
            String jobTitle,
            String jobDescription,
            String resumeContent,
            String language,
            String difficulty
    ) {
        log.info("AI Engine: StartInterview session={}, job={}", sessionId, jobTitle);
        
        if (!isAvailable()) {
            return mockStartInterview(language);
        }

        try {
            com.cvibe.grpc.ai.StartInterviewRequest request = com.cvibe.grpc.ai.StartInterviewRequest.newBuilder()
                    .setUserId(userId)
                    .setSessionId(sessionId)
                    .setJobTitle(jobTitle)
                    .setJobDescription(jobDescription != null ? jobDescription : "")
                    .setResumeContent(resumeContent != null ? resumeContent : "")
                    .setConfig(com.cvibe.grpc.ai.InterviewConfig.newBuilder()
                            .setLanguage(language != null ? language : "zh")
                            .setDifficulty(difficulty != null ? difficulty : "medium")
                            .build())
                    .build();

            com.cvibe.grpc.ai.StartInterviewResponse response = getStubWithDeadline().startInterview(request);

            return StartInterviewResult.builder()
                    .success(response.getSuccess())
                    .welcomeMessage(response.getWelcomeMessage())
                    .firstQuestion(response.getFirstQuestion())
                    .build();

        } catch (Exception e) {
            log.error("AI Engine StartInterview error", e);
            return mockStartInterview(language);
        }
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
        log.info("AI Engine: SendInterviewMessage session={}", sessionId);
        
        if (!isAvailable()) {
            // Mock 响应
            String[] chunks = {
                "感谢您的回答。",
                "您提到的经验很有价值。",
                "让我问您下一个问题：",
                "您如何处理工作中的挑战？"
            };
            try {
                for (String chunk : chunks) {
                    onChunk.accept(chunk);
                    Thread.sleep(100);
                }
                onComplete.run();
            } catch (Exception e) {
                onError.accept(e);
            }
            return;
        }

        // 实际 gRPC 调用
        try {
            com.cvibe.grpc.ai.SendMessageRequest request = com.cvibe.grpc.ai.SendMessageRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setUserMessage(userMessage)
                    .build();

            Iterator<com.cvibe.grpc.ai.MessageChunk> responseIterator = getStubWithDeadline().sendInterviewMessage(request);
            
            while (responseIterator.hasNext()) {
                com.cvibe.grpc.ai.MessageChunk chunk = responseIterator.next();
                if (!chunk.getContent().isEmpty()) {
                    onChunk.accept(chunk.getContent());
                }
                if (chunk.getIsFinal()) {
                    break;
                }
            }
            onComplete.run();

        } catch (Exception e) {
            log.error("AI Engine SendInterviewMessage error", e);
            onError.accept(e);
        }
    }

    /**
     * 差距分析
     */
    public GapAnalysisResult analyzeGap(
            String userId,
            String goalTitle,
            String targetDate,
            ProfileDataDto profile
    ) {
        log.info("AI Engine: AnalyzeGap user={}, goal={}", userId, goalTitle);
        
        if (!isAvailable()) {
            return mockAnalyzeGap();
        }

        try {
            com.cvibe.grpc.ai.ProfileData.Builder profileBuilder = com.cvibe.grpc.ai.ProfileData.newBuilder()
                    .setName(profile.getName() != null ? profile.getName() : "")
                    .setTitle(profile.getTitle() != null ? profile.getTitle() : "")
                    .setSummary(profile.getSummary() != null ? profile.getSummary() : "");
            
            if (profile.getSkills() != null) {
                profileBuilder.addAllSkills(profile.getSkills());
            }

            com.cvibe.grpc.ai.GapAnalysisRequest request = com.cvibe.grpc.ai.GapAnalysisRequest.newBuilder()
                    .setUserId(userId)
                    .setGoalTitle(goalTitle)
                    .setTargetDate(targetDate != null ? targetDate : "")
                    .setCurrentProfile(profileBuilder.build())
                    .build();

            com.cvibe.grpc.ai.GapAnalysisResponse response = getStubWithDeadline().analyzeGap(request);

            GapAnalysisResult result = new GapAnalysisResult();
            result.setReadinessScore(response.getReadinessScore());
            result.setRecommendations(new ArrayList<>(response.getRecommendationsList()));

            List<GapItem> gaps = new ArrayList<>();
            for (com.cvibe.grpc.ai.GapItem gap : response.getGapsList()) {
                gaps.add(new GapItem(
                        gap.getSkill(),
                        gap.getCurrentLevel(),
                        gap.getRequiredLevel(),
                        gap.getPriority()));
            }
            result.setGaps(gaps);

            return result;

        } catch (Exception e) {
            log.error("AI Engine AnalyzeGap error", e);
            return mockAnalyzeGap();
        }
    }

    /**
     * 生成学习路径（流式）
     */
    public Iterator<LearningPathChunk> generateLearningPath(
            String userId,
            String goalId,
            List<GapItem> gaps,
            String preferredStyle
    ) {
        log.info("AI Engine: GenerateLearningPath user={}, goal={}", userId, goalId);
        
        if (!isAvailable()) {
            List<LearningPathChunk> chunks = new ArrayList<>();
            chunks.add(new LearningPathChunk("Phase 1: Foundation", "Start with fundamentals", false));
            chunks.add(new LearningPathChunk("Phase 2: Practice", "Build projects", false));
            chunks.add(new LearningPathChunk("Phase 3: Advanced", "Master complex topics", true));
            return chunks.iterator();
        }

        try {
            com.cvibe.grpc.ai.LearningPathRequest.Builder requestBuilder = 
                    com.cvibe.grpc.ai.LearningPathRequest.newBuilder()
                            .setUserId(userId)
                            .setGoalId(goalId)
                            .setPreferredStyle(preferredStyle != null ? preferredStyle : "MIXED");

            for (GapItem gap : gaps) {
                requestBuilder.addGaps(com.cvibe.grpc.ai.GapItem.newBuilder()
                        .setSkill(gap.getSkill())
                        .setCurrentLevel(gap.getCurrentLevel())
                        .setRequiredLevel(gap.getRequiredLevel())
                        .setPriority(gap.getPriority())
                        .build());
            }

            Iterator<com.cvibe.grpc.ai.LearningPathChunk> responseIterator =
                    getStubWithDeadline().generateLearningPath(requestBuilder.build());

            List<LearningPathChunk> result = new ArrayList<>();
            while (responseIterator.hasNext()) {
                com.cvibe.grpc.ai.LearningPathChunk chunk = responseIterator.next();
                result.add(new LearningPathChunk(chunk.getPhase(), chunk.getContent(), chunk.getIsFinal()));
            }
            return result.iterator();

        } catch (Exception e) {
            log.error("AI Engine GenerateLearningPath error", e);
            List<LearningPathChunk> fallback = new ArrayList<>();
            fallback.add(new LearningPathChunk("Error", e.getMessage(), true));
            return fallback.iterator();
        }
    }

    /**
     * 开始模拟面试
     */
    public StartMockResult startMockInterview(
            String userId,
            String sessionId,
            String jobTitle,
            String interviewType,
            int questionCount,
            String language
    ) {
        log.info("AI Engine: StartMockInterview session={}", sessionId);
        
        if (!isAvailable()) {
            return StartMockResult.builder()
                    .success(true)
                    .sessionId(sessionId)
                    .totalQuestions(questionCount)
                    .build();
        }

        try {
            com.cvibe.grpc.ai.StartMockRequest request = com.cvibe.grpc.ai.StartMockRequest.newBuilder()
                    .setUserId(userId)
                    .setSessionId(sessionId)
                    .setJobTitle(jobTitle)
                    .setInterviewType(interviewType != null ? interviewType : "MIXED")
                    .setQuestionCount(questionCount)
                    .setLanguage(language != null ? language : "zh")
                    .build();

            com.cvibe.grpc.ai.StartMockResponse response = getStubWithDeadline().startMockInterview(request);

            return StartMockResult.builder()
                    .success(response.getSuccess())
                    .sessionId(response.getSessionId())
                    .totalQuestions(response.getTotalQuestions())
                    .build();

        } catch (Exception e) {
            log.error("AI Engine StartMockInterview error", e);
            return StartMockResult.builder()
                    .success(true)
                    .sessionId(sessionId)
                    .totalQuestions(questionCount)
                    .build();
        }
    }

    /**
     * 评估模拟面试答案
     */
    public EvaluationResult evaluateAnswer(
            String sessionId,
            int questionIndex,
            String question,
            String answerText
    ) {
        log.info("AI Engine: EvaluateAnswer session={}, q={}", sessionId, questionIndex);
        
        if (!isAvailable()) {
            return mockEvaluateAnswer();
        }

        try {
            com.cvibe.grpc.ai.EvaluateAnswerRequest request = com.cvibe.grpc.ai.EvaluateAnswerRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setQuestionIndex(questionIndex)
                    .setQuestion(question)
                    .setAnswerText(answerText)
                    .build();

            com.cvibe.grpc.ai.EvaluationResponse response = getStubWithDeadline().evaluateAnswer(request);

            return EvaluationResult.builder()
                    .score(response.getScore())
                    .feedback(response.getFeedback())
                    .strengths(new ArrayList<>(response.getStrengthsList()))
                    .improvements(new ArrayList<>(response.getImprovementsList()))
                    .build();

        } catch (Exception e) {
            log.error("AI Engine EvaluateAnswer error", e);
            return mockEvaluateAnswer();
        }
    }

    // ==================== Profile Interview (Information Collection) ====================

    /**
     * Start a profile collection interview session
     */
    public StartProfileInterviewResult startProfileInterview(
            String userId,
            String sessionId,
            String language,
            String existingProfile
    ) {
        log.info("AI Engine: StartProfileInterview session={}, user={}", sessionId, userId);

        if (!isAvailable()) {
            return mockStartProfileInterview(language);
        }

        try {
            com.cvibe.grpc.ai.StartProfileInterviewRequest.Builder requestBuilder =
                    com.cvibe.grpc.ai.StartProfileInterviewRequest.newBuilder()
                            .setUserId(userId)
                            .setSessionId(sessionId)
                            .setLanguage(language != null ? language : "zh");

            if (existingProfile != null) {
                requestBuilder.setExistingProfile(existingProfile);
            }

            com.cvibe.grpc.ai.ProfileInterviewResponse response =
                    getStubWithDeadline().startProfileInterview(requestBuilder.build());

            return StartProfileInterviewResult.builder()
                    .success(response.getSuccess())
                    .welcomeMessage(response.getWelcomeMessage())
                    .firstQuestion(response.getFirstQuestion())
                    .currentPhase(response.getCurrentPhase())
                    .build();

        } catch (Exception e) {
            log.error("AI Engine StartProfileInterview error", e);
            return mockStartProfileInterview(language);
        }
    }

    /**
     * Send a message in profile interview session (streaming response)
     * Handles DeepSeek-R1's slow response with thinking status updates
     */
    public void sendProfileInterviewMessage(
            String sessionId,
            String userMessage,
            Consumer<String> onChunk,
            Consumer<String> onPhaseUpdate,
            Runnable onComplete,
            Consumer<Throwable> onError
    ) {
        log.info("AI Engine: SendProfileInterviewMessage session={}", sessionId);

        if (!isAvailable()) {
            // Mock response
            String[] chunks = {
                "谢谢你的分享。",
                "能告诉我更多关于这段经历的细节吗？",
                "比如你在其中负责什么具体工作？"
            };
            try {
                for (String chunk : chunks) {
                    onChunk.accept(chunk);
                    Thread.sleep(100);
                }
                onPhaseUpdate.accept("work");
                onComplete.run();
            } catch (Exception e) {
                onError.accept(e);
            }
            return;
        }

        try {
            com.cvibe.grpc.ai.ProfileInterviewMessageRequest request =
                    com.cvibe.grpc.ai.ProfileInterviewMessageRequest.newBuilder()
                            .setSessionId(sessionId)
                            .setUserMessage(userMessage)
                            .build();

            Iterator<com.cvibe.grpc.ai.ProfileInterviewChunk> responseIterator =
                    getStubWithDeadline().sendProfileInterviewMessage(request);

            String currentPhase = "";
            StringBuilder actualResponse = new StringBuilder();

            while (responseIterator.hasNext()) {
                com.cvibe.grpc.ai.ProfileInterviewChunk chunk = responseIterator.next();
                String content = chunk.getContent();

                if (!content.isEmpty()) {
                    // Filter out [THINKING] status messages - they are heartbeats for slow models
                    if (content.startsWith("[THINKING]")) {
                        log.debug("AI thinking status: {}", content);
                        // Don't add to response, just log
                        continue;
                    }
                    // Accumulate actual response content
                    actualResponse.append(content);
                }
                if (!chunk.getPhase().isEmpty()) {
                    currentPhase = chunk.getPhase();
                }
                if (chunk.getIsFinal()) {
                    break;
                }
            }

            // Send the complete response
            String finalResponse = actualResponse.toString().trim();
            if (!finalResponse.isEmpty()) {
                onChunk.accept(finalResponse);
            }

            if (!currentPhase.isEmpty()) {
                onPhaseUpdate.accept(currentPhase);
            }
            onComplete.run();

        } catch (Exception e) {
            log.error("AI Engine SendProfileInterviewMessage error", e);
            onError.accept(e);
        }
    }

    /**
     * Get current state of a profile interview session
     */
    public ProfileInterviewStateResult getProfileInterviewState(String sessionId) {
        log.info("AI Engine: GetProfileInterviewState session={}", sessionId);

        if (!isAvailable()) {
            return ProfileInterviewStateResult.builder()
                    .success(false)
                    .sessionId(sessionId)
                    .build();
        }

        try {
            com.cvibe.grpc.ai.GetProfileInterviewStateRequest request =
                    com.cvibe.grpc.ai.GetProfileInterviewStateRequest.newBuilder()
                            .setSessionId(sessionId)
                            .build();

            com.cvibe.grpc.ai.ProfileInterviewStateResponse response =
                    getStubWithDeadline().getProfileInterviewState(request);

            return ProfileInterviewStateResult.builder()
                    .success(response.getSuccess())
                    .sessionId(response.getSessionId())
                    .userId(response.getUserId())
                    .currentPhase(response.getCurrentPhase())
                    .phaseName(response.getPhaseName())
                    .turnCount(response.getTurnCount())
                    .status(response.getStatus())
                    .portraitSummary(response.getPortraitSummary())
                    .build();

        } catch (Exception e) {
            log.error("AI Engine GetProfileInterviewState error", e);
            return ProfileInterviewStateResult.builder()
                    .success(false)
                    .sessionId(sessionId)
                    .build();
        }
    }

    /**
     * Finish profile interview and extract structured profile
     */
    public CollectedProfileResult finishProfileInterview(String sessionId) {
        log.info("AI Engine: FinishProfileInterview session={}", sessionId);

        if (!isAvailable()) {
            return CollectedProfileResult.builder()
                    .success(false)
                    .errorMessage("AI Engine not available")
                    .build();
        }

        try {
            com.cvibe.grpc.ai.FinishProfileInterviewRequest request =
                    com.cvibe.grpc.ai.FinishProfileInterviewRequest.newBuilder()
                            .setSessionId(sessionId)
                            .build();

            com.cvibe.grpc.ai.CollectedProfileResponse response =
                    getStubWithDeadline().finishProfileInterview(request);

            return CollectedProfileResult.builder()
                    .success(response.getSuccess())
                    .profileJson(response.getProfileJson())
                    .completenessScore(response.getCompletenessScore())
                    .missingSections(new ArrayList<>(response.getMissingSectionsList()))
                    .errorMessage(response.getErrorMessage())
                    .build();

        } catch (Exception e) {
            log.error("AI Engine FinishProfileInterview error", e);
            return CollectedProfileResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    // ==================== Mock implementations ====================

    private ResumeParseResult mockParseResume(String fileName) {
        log.info("Using mock resume parse for: {}", fileName);
        
        ResumeParseResult result = ResumeParseResult.builder()
                .success(true)
                .name("待解析")
                .email("")
                .phone("")
                .summary("简历文件 " + fileName + " 已上传。AI 解析服务暂时不可用，请稍后重试解析。")
                .rawText("Resume content from " + fileName)
                .build();
        
        result.setSkills(List.of(
                SkillData.builder().name("待解析").level("INTERMEDIATE").category("").build()
        ));
        result.setExperiences(new ArrayList<>());
        result.setEducations(new ArrayList<>());
        result.setProjects(new ArrayList<>());
        result.setCertifications(new ArrayList<>());
        result.setLanguages(new ArrayList<>());
        result.setAchievements(new ArrayList<>());
        
        return result;
    }

    private StartInterviewResult mockStartInterview(String language) {
        if ("zh".equals(language)) {
            return StartInterviewResult.builder()
                    .success(true)
                    .welcomeMessage("您好！欢迎参加本次面试。")
                    .firstQuestion("请先介绍一下您自己，以及您最近的工作经历。")
                    .build();
        } else {
            return StartInterviewResult.builder()
                    .success(true)
                    .welcomeMessage("Hello! Welcome to this interview session.")
                    .firstQuestion("Please tell me about yourself and your recent work experience.")
                    .build();
        }
    }

    private GapAnalysisResult mockAnalyzeGap() {
        GapAnalysisResult result = new GapAnalysisResult();
        result.setReadinessScore(65);
        
        List<GapItem> gaps = new ArrayList<>();
        gaps.add(new GapItem("System Design", "BASIC", "ADVANCED", 5));
        gaps.add(new GapItem("Leadership", "INTERMEDIATE", "ADVANCED", 4));
        gaps.add(new GapItem("Cloud Architecture", "BASIC", "INTERMEDIATE", 3));
        result.setGaps(gaps);
        
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Focus on system design practice");
        recommendations.add("Take on more leadership responsibilities");
        recommendations.add("Get AWS/GCP certification");
        result.setRecommendations(recommendations);
        
        return result;
    }

    private EvaluationResult mockEvaluateAnswer() {
        return EvaluationResult.builder()
                .score(75)
                .feedback("Good answer with clear structure. Consider providing more specific examples.")
                .strengths(List.of("Clear communication", "Structured approach"))
                .improvements(List.of("Add specific metrics", "Provide more examples"))
                .build();
    }

    private StartProfileInterviewResult mockStartProfileInterview(String language) {
        if ("zh".equals(language)) {
            return StartProfileInterviewResult.builder()
                    .success(true)
                    .welcomeMessage("你好！我是你的职业顾问。接下来我会通过对话详细了解你的背景，帮助你构建完整的个人资料库。")
                    .firstQuestion("首先，请简单介绍一下你自己——你目前在做什么工作，或者你的学习背景是什么？")
                    .currentPhase("intro")
                    .build();
        } else {
            return StartProfileInterviewResult.builder()
                    .success(true)
                    .welcomeMessage("Hello! I'm your career advisor. I'll get to know your background through our conversation.")
                    .firstQuestion("First, could you briefly introduce yourself - what do you currently do, or what's your educational background?")
                    .currentPhase("intro")
                    .build();
        }
    }

    // ==================== DTOs ====================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResumeParseResult {
        private boolean success;
        private String errorMessage;
        // 个人信息
        private String name;
        private String email;
        private String phone;
        private String linkedin;
        private String github;
        private String website;
        private String location;
        // 概要
        private String headline;
        private String summary;
        private String rawText;
        // 结构化数据
        private List<SkillData> skills = new ArrayList<>();
        private List<ExperienceData> experiences = new ArrayList<>();
        private List<EducationData> educations = new ArrayList<>();
        private List<ProjectData> projects = new ArrayList<>();
        private List<CertificationData> certifications = new ArrayList<>();
        private List<LanguageData> languages = new ArrayList<>();
        private List<String> achievements = new ArrayList<>();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkillData {
        private String name;
        private String level;       // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
        private String category;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExperienceData {
        private String company;
        private String title;
        private String location;
        private String employmentType;  // FULL_TIME, PART_TIME, etc.
        private String startDate;
        private String endDate;
        private boolean isCurrent;
        private String description;
        private List<String> achievements = new ArrayList<>();
        private List<String> technologies = new ArrayList<>();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EducationData {
        private String school;
        private String degree;
        private String field;
        private String location;
        private String startDate;
        private String endDate;
        private String gpa;
        private String description;
        private List<String> activities = new ArrayList<>();
        private List<String> honors = new ArrayList<>();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjectData {
        private String name;
        private String description;
        private String url;
        private String repoUrl;
        private List<String> technologies = new ArrayList<>();
        private String startDate;
        private String endDate;
        private List<String> highlights = new ArrayList<>();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CertificationData {
        private String name;
        private String issuer;
        private String date;
        private String url;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LanguageData {
        private String language;
        private String proficiency;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StartInterviewResult {
        private boolean success;
        private String welcomeMessage;
        private String firstQuestion;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProfileDataDto {
        private String name;
        private String title;
        private String summary;
        private List<String> skills = new ArrayList<>();
    }

    @Data
    public static class GapAnalysisResult {
        private int readinessScore;
        private List<GapItem> gaps = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GapItem {
        private String skill;
        private String currentLevel;
        private String requiredLevel;
        private int priority;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LearningPathChunk {
        private String phase;
        private String content;
        private boolean isFinal;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StartMockResult {
        private boolean success;
        private String sessionId;
        private int totalQuestions;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EvaluationResult {
        private int score;
        private String feedback;
        private List<String> strengths = new ArrayList<>();
        private List<String> improvements = new ArrayList<>();
    }

    // ==================== Profile Interview DTOs ====================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StartProfileInterviewResult {
        private boolean success;
        private String welcomeMessage;
        private String firstQuestion;
        private String currentPhase;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProfileInterviewStateResult {
        private boolean success;
        private String sessionId;
        private String userId;
        private String currentPhase;
        private String phaseName;
        private int turnCount;
        private String status;
        private String portraitSummary;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CollectedProfileResult {
        private boolean success;
        private String profileJson;
        private int completenessScore;
        private List<String> missingSections = new ArrayList<>();
        private String errorMessage;
    }
}
