package com.cvibe.common.grpc;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;

/**
 * AI Engine gRPC 客户端
 * 
 * 封装对 ai-engine 服务的调用。
 * 当 gRPC 服务不可用时，返回 Mock 数据。
 * 
 * 注意：proto 生成的类需要先运行 mvn compile 才会生成
 */
@Slf4j
@Component
public class AIEngineClient {

    private final GrpcConfig grpcConfig;

    public AIEngineClient(GrpcConfig grpcConfig) {
        this.grpcConfig = grpcConfig;
    }

    // 检查 gRPC 是否可用
    private boolean isAvailable() {
        try {
            var channel = grpcConfig.aiEngineChannel();
            return channel != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析简历
     */
    public ResumeParseResult parseResume(byte[] fileContent, String fileName, String fileType) {
        log.info("AI Engine: ParseResume for file: {}", fileName);
        
        if (!isAvailable()) {
            log.warn("AI Engine not available, returning mock data");
            return mockParseResume(fileName);
        }

        // TODO: 实际调用 gRPC 服务
        return mockParseResume(fileName);
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

        return mockStartInterview(language);
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
        
        try {
            String[] chunks = {
                "感谢您的回答。",
                "您提到的经验很有价值。",
                "让我问您下一个问题：",
                "您如何处理工作中的挑战？"
            };
            
            for (String chunk : chunks) {
                onChunk.accept(chunk);
                Thread.sleep(100);
            }
            onComplete.run();
        } catch (Exception e) {
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
            ProfileData profile
    ) {
        log.info("AI Engine: AnalyzeGap user={}, goal={}", userId, goalTitle);
        
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
        
        List<LearningPathChunk> chunks = new ArrayList<>();
        chunks.add(new LearningPathChunk("Phase 1: Foundation", "Start with fundamentals", false));
        chunks.add(new LearningPathChunk("Phase 2: Practice", "Build projects", false));
        chunks.add(new LearningPathChunk("Phase 3: Advanced", "Master complex topics", true));
        return chunks.iterator();
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
        
        StartMockResult result = new StartMockResult();
        result.setSuccess(true);
        result.setSessionId(sessionId);
        result.setTotalQuestions(questionCount);
        return result;
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
        
        EvaluationResult result = new EvaluationResult();
        result.setScore(75);
        result.setFeedback("Good answer with clear structure. Consider providing more specific examples.");
        
        List<String> strengths = new ArrayList<>();
        strengths.add("Clear communication");
        strengths.add("Structured approach");
        result.setStrengths(strengths);
        
        List<String> improvements = new ArrayList<>();
        improvements.add("Add specific metrics");
        improvements.add("Provide more examples");
        result.setImprovements(improvements);
        
        return result;
    }

    // ==================== Mock implementations ====================

    private ResumeParseResult mockParseResume(String fileName) {
        ResumeParseResult result = new ResumeParseResult();
        result.setSuccess(true);
        result.setName("John Doe");
        result.setEmail("john.doe@example.com");
        result.setPhone("+1-234-567-8900");
        result.setSummary("Experienced software engineer with 5+ years in backend development.");
        
        List<String> skills = new ArrayList<>();
        skills.add("Java");
        skills.add("Python");
        skills.add("AWS");
        skills.add("Kubernetes");
        skills.add("PostgreSQL");
        result.setSkills(skills);
        result.setRawText("Resume content from " + fileName);
        return result;
    }

    private StartInterviewResult mockStartInterview(String language) {
        StartInterviewResult result = new StartInterviewResult();
        result.setSuccess(true);
        if ("zh".equals(language)) {
            result.setWelcomeMessage("您好！欢迎参加本次面试。");
            result.setFirstQuestion("请先介绍一下您自己，以及您最近的工作经历。");
        } else {
            result.setWelcomeMessage("Hello! Welcome to this interview session.");
            result.setFirstQuestion("Please tell me about yourself and your recent work experience.");
        }
        return result;
    }

    // ==================== DTOs ====================

    @Data
    public static class ResumeParseResult {
        private boolean success;
        private String errorMessage;
        private String name;
        private String email;
        private String phone;
        private String summary;
        private List<String> skills = new ArrayList<>();
        private String rawText;
        private List<ExperienceData> experiences = new ArrayList<>();
        private List<EducationData> educations = new ArrayList<>();
    }

    @Data
    public static class ExperienceData {
        private String company;
        private String title;
        private String startDate;
        private String endDate;
        private String description;
        private boolean isCurrent;
    }

    @Data
    public static class EducationData {
        private String school;
        private String degree;
        private String field;
        private String startDate;
        private String endDate;
    }

    @Data
    public static class StartInterviewResult {
        private boolean success;
        private String welcomeMessage;
        private String firstQuestion;
    }

    @Data
    public static class ProfileData {
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
    public static class StartMockResult {
        private boolean success;
        private String sessionId;
        private int totalQuestions;
    }

    @Data
    public static class EvaluationResult {
        private int score;
        private String feedback;
        private List<String> strengths = new ArrayList<>();
        private List<String> improvements = new ArrayList<>();
    }
}
