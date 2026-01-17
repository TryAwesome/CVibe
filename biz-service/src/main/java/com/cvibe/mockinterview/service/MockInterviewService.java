package com.cvibe.mockinterview.service;

import com.cvibe.auth.entity.User;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import com.cvibe.mockinterview.dto.*;
import com.cvibe.mockinterview.entity.*;
import com.cvibe.mockinterview.repository.MockInterviewSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing mock interview sessions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockInterviewService {

    private final MockInterviewSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // Mock questions by category
    private static final Map<String, List<MockQuestion>> MOCK_QUESTIONS_BY_CATEGORY = createMockQuestions();

    /**
     * Create and start a new mock interview session
     */
    @Transactional
    public MockInterviewStateResponse createSession(UUID userId, CreateMockInterviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Parse interview type
        MockInterviewType type;
        try {
            type = MockInterviewType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INTERVIEW_TYPE);
        }

        MockInterviewSettingsDto settings = request.getSettings();
        if (settings == null) {
            settings = MockInterviewSettingsDto.builder()
                    .difficulty("MEDIUM")
                    .questionCount(5)
                    .timePerQuestion(120)
                    .build();
        }

        // Generate questions
        List<MockQuestionData> questions = generateQuestionsForSession(settings);

        Instant now = Instant.now();
        MockInterviewSession session = MockInterviewSession.builder()
                .user(user)
                .type(type)
                .status(MockInterviewStatus.IN_PROGRESS)
                .currentQuestionIndex(0)
                .totalQuestions(questions.size())
                .settingsJson(toJson(settings))
                .questionsJson(toJson(questions))
                .startedAt(now)
                .build();

        session = sessionRepository.save(session);
        log.info("Created mock interview session {} for user {}", session.getId(), userId);

        return buildStateResponse(session, questions, settings);
    }

    /**
     * Get session by ID
     */
    @Transactional(readOnly = true)
    public MockInterviewStateResponse getSession(UUID userId, UUID sessionId) {
        MockInterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOCK_SESSION_NOT_FOUND));

        List<MockQuestionData> questions = parseQuestions(session.getQuestionsJson());
        MockInterviewSettingsDto settings = parseSettings(session.getSettingsJson());
        return buildStateResponse(session, questions, settings);
    }

    /**
     * Get paginated history for a user
     */
    @Transactional(readOnly = true)
    public Page<MockInterviewSessionDto> getHistory(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(session -> {
                    MockInterviewSettingsDto settings = parseSettings(session.getSettingsJson());
                    return MockInterviewSessionDto.fromEntity(session, settings);
                });
    }

    /**
     * Get next question for the session
     */
    @Transactional(readOnly = true)
    public MockInterviewQuestionDto getNextQuestion(UUID userId, UUID sessionId) {
        MockInterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOCK_SESSION_NOT_FOUND));

        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.SESSION_NOT_ACTIVE);
        }

        List<MockQuestionData> questions = parseQuestions(session.getQuestionsJson());
        int currentIndex = session.getCurrentQuestionIndex();

        if (currentIndex >= questions.size()) {
            throw new BusinessException(ErrorCode.QUESTION_INDEX_OUT_OF_RANGE);
        }

        MockQuestionData q = questions.get(currentIndex);
        MockInterviewSettingsDto settings = parseSettings(session.getSettingsJson());

        return MockInterviewQuestionDto.builder()
                .index(currentIndex)
                .questionId(q.getId())
                .question(q.getQuestion())
                .category(q.getCategory())
                .timeLimit(settings.getTimePerQuestion())
                .build();
    }

    /**
     * Submit an answer to a question
     */
    @Transactional
    public MockAnswerSubmitResponse submitAnswer(UUID userId, UUID sessionId, SubmitMockAnswerRequest request) {
        MockInterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOCK_SESSION_NOT_FOUND));

        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.SESSION_NOT_ACTIVE);
        }

        List<MockQuestionData> questions = parseQuestions(session.getQuestionsJson());
        int currentIndex = session.getCurrentQuestionIndex();

        if (currentIndex >= questions.size()) {
            throw new BusinessException(ErrorCode.QUESTION_INDEX_OUT_OF_RANGE);
        }

        MockQuestionData currentQuestion = questions.get(currentIndex);

        // Generate mock feedback
        int score = generateMockScore();
        MockInterviewQuestionDto.FeedbackDto feedback = generateMockFeedback(score, currentQuestion.getCategory());

        // Update the question with response and feedback
        currentQuestion.setResponse(MockQuestionData.Response.builder()
                .type(request.getResponseType())
                .content(request.getContent())
                .mediaUrl(request.getMediaUrl())
                .durationSeconds(request.getDurationSeconds())
                .submittedAt(Instant.now().toString())
                .build());

        currentQuestion.setFeedback(MockQuestionData.Feedback.builder()
                .score(score)
                .overallFeedback(feedback.getOverallFeedback())
                .strengths(feedback.getStrengths())
                .improvements(feedback.getImprovements())
                .suggestedResponse(feedback.getSuggestedResponse())
                .build());

        // Update session
        session.setCurrentQuestionIndex(currentIndex + 1);
        session.setQuestionsJson(toJson(questions));

        boolean hasMore = session.getCurrentQuestionIndex() < questions.size();
        MockInterviewQuestionDto nextQuestion = null;

        if (hasMore) {
            MockQuestionData next = questions.get(session.getCurrentQuestionIndex());
            MockInterviewSettingsDto settings = parseSettings(session.getSettingsJson());
            nextQuestion = MockInterviewQuestionDto.builder()
                    .index(session.getCurrentQuestionIndex())
                    .questionId(next.getId())
                    .question(next.getQuestion())
                    .category(next.getCategory())
                    .timeLimit(settings.getTimePerQuestion())
                    .build();
        } else {
            // Session completed - trigger analysis
            session.setStatus(MockInterviewStatus.ANALYZING);
        }

        sessionRepository.save(session);
        log.info("Answer submitted for session {} question {}", sessionId, currentIndex);

        return MockAnswerSubmitResponse.builder()
                .accepted(true)
                .score(score)
                .feedback(feedback)
                .nextQuestion(nextQuestion)
                .hasMoreQuestions(hasMore)
                .sessionCompleted(!hasMore)
                .build();
    }

    /**
     * Complete the session and generate overall feedback
     */
    @Transactional
    public MockInterviewStateResponse completeSession(UUID userId, UUID sessionId) {
        MockInterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOCK_SESSION_NOT_FOUND));

        if (session.getStatus() == MockInterviewStatus.COMPLETED) {
            // Already completed, return current state
            List<MockQuestionData> questions = parseQuestions(session.getQuestionsJson());
            MockInterviewSettingsDto settings = parseSettings(session.getSettingsJson());
            return buildStateResponse(session, questions, settings);
        }

        List<MockQuestionData> questions = parseQuestions(session.getQuestionsJson());

        // Calculate overall score from individual question scores
        int totalScore = 0;
        int scoredQuestions = 0;
        for (MockQuestionData q : questions) {
            if (q.getFeedback() != null && q.getFeedback().getScore() != null) {
                totalScore += q.getFeedback().getScore();
                scoredQuestions++;
            }
        }
        int overallScore = scoredQuestions > 0 ? totalScore / scoredQuestions : 0;

        // Generate overall feedback
        OverallFeedback overallFeedback = generateOverallFeedback(overallScore, questions);

        session.setStatus(MockInterviewStatus.COMPLETED);
        session.setOverallScore(overallScore);
        session.setFeedbackJson(toJson(overallFeedback));
        session.setCompletedAt(Instant.now());
        sessionRepository.save(session);

        log.info("Session {} completed with score {}", sessionId, overallScore);

        MockInterviewSettingsDto settings = parseSettings(session.getSettingsJson());
        return buildStateResponse(session, questions, settings);
    }

    /**
     * Pause a session
     */
    @Transactional
    public MockInterviewSessionDto pauseSession(UUID userId, UUID sessionId) {
        MockInterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOCK_SESSION_NOT_FOUND));

        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.SESSION_NOT_ACTIVE);
        }

        session.setStatus(MockInterviewStatus.PAUSED);
        session = sessionRepository.save(session);

        log.info("Session {} paused", sessionId);
        MockInterviewSettingsDto settings = parseSettings(session.getSettingsJson());
        return MockInterviewSessionDto.fromEntity(session, settings);
    }

    /**
     * Resume a paused session
     */
    @Transactional
    public MockInterviewStateResponse resumeSession(UUID userId, UUID sessionId) {
        MockInterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOCK_SESSION_NOT_FOUND));

        if (session.getStatus() != MockInterviewStatus.PAUSED) {
            throw new BusinessException(ErrorCode.SESSION_NOT_ACTIVE);
        }

        session.setStatus(MockInterviewStatus.IN_PROGRESS);
        session = sessionRepository.save(session);

        log.info("Session {} resumed", sessionId);

        List<MockQuestionData> questions = parseQuestions(session.getQuestionsJson());
        MockInterviewSettingsDto settings = parseSettings(session.getSettingsJson());
        return buildStateResponse(session, questions, settings);
    }

    /**
     * Get summary statistics for a user
     */
    @Transactional(readOnly = true)
    public MockInterviewSummaryDto getSummary(UUID userId) {
        long total = sessionRepository.countByUserId(userId);
        long completed = sessionRepository.countCompletedByUserId(userId);
        long inProgress = sessionRepository.countByUserIdAndStatus(userId, MockInterviewStatus.IN_PROGRESS);
        Double avgScore = sessionRepository.getAverageScoreByUserId(userId);

        // Get type stats
        long videoCount = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(s -> s.getType() == MockInterviewType.VIDEO)
                .count();
        long audioCount = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(s -> s.getType() == MockInterviewType.AUDIO)
                .count();
        long textCount = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(s -> s.getType() == MockInterviewType.TEXT)
                .count();

        // Get best score and last interview date
        List<MockInterviewSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Integer bestScore = sessions.stream()
                .filter(s -> s.getOverallScore() != null)
                .map(MockInterviewSession::getOverallScore)
                .max(Integer::compareTo)
                .orElse(null);

        String lastInterviewDate = sessions.isEmpty() ? null : 
                sessions.get(0).getCreatedAt().toString();

        return MockInterviewSummaryDto.builder()
                .totalInterviews(total)
                .completedInterviews(completed)
                .inProgressInterviews(inProgress)
                .averageScore(avgScore)
                .bestScore(bestScore)
                .lastInterviewDate(lastInterviewDate)
                .typeStats(MockInterviewSummaryDto.TypeStats.builder()
                        .videoCount(videoCount)
                        .audioCount(audioCount)
                        .textCount(textCount)
                        .build())
                .build();
    }

    // ==================== Helper Methods ====================

    private MockInterviewStateResponse buildStateResponse(
            MockInterviewSession session,
            List<MockQuestionData> questions,
            MockInterviewSettingsDto settings) {

        MockInterviewQuestionDto currentQuestion = null;
        if (session.getCurrentQuestionIndex() < questions.size() && session.isActive()) {
            MockQuestionData q = questions.get(session.getCurrentQuestionIndex());
            currentQuestion = MockInterviewQuestionDto.builder()
                    .index(session.getCurrentQuestionIndex())
                    .questionId(q.getId())
                    .question(q.getQuestion())
                    .category(q.getCategory())
                    .timeLimit(settings != null ? settings.getTimePerQuestion() : 120)
                    .build();
        }

        List<MockInterviewQuestionDto> questionDtos = questions.stream()
                .map(this::toQuestionDto)
                .collect(Collectors.toList());

        boolean hasMore = session.getCurrentQuestionIndex() < questions.size();
        String nextAction = session.isCompleted() ? "VIEW_RESULTS" :
                (session.getStatus() == MockInterviewStatus.SETUP ? "START" : "ANSWER_QUESTION");

        return MockInterviewStateResponse.builder()
                .session(MockInterviewSessionDto.fromEntity(session, settings))
                .currentQuestion(currentQuestion)
                .questions(questionDtos)
                .hasMoreQuestions(hasMore)
                .sessionCompleted(session.isCompleted())
                .nextAction(nextAction)
                .build();
    }

    private MockInterviewQuestionDto toQuestionDto(MockQuestionData data) {
        MockInterviewQuestionDto.ResponseDto responseDto = null;
        if (data.getResponse() != null) {
            responseDto = MockInterviewQuestionDto.ResponseDto.builder()
                    .type(data.getResponse().getType())
                    .content(data.getResponse().getContent())
                    .mediaUrl(data.getResponse().getMediaUrl())
                    .durationSeconds(data.getResponse().getDurationSeconds())
                    .submittedAt(data.getResponse().getSubmittedAt())
                    .build();
        }

        MockInterviewQuestionDto.FeedbackDto feedbackDto = null;
        if (data.getFeedback() != null) {
            feedbackDto = MockInterviewQuestionDto.FeedbackDto.builder()
                    .score(data.getFeedback().getScore())
                    .overallFeedback(data.getFeedback().getOverallFeedback())
                    .strengths(data.getFeedback().getStrengths())
                    .improvements(data.getFeedback().getImprovements())
                    .suggestedResponse(data.getFeedback().getSuggestedResponse())
                    .build();
        }

        return MockInterviewQuestionDto.builder()
                .index(data.getIndex())
                .questionId(data.getId())
                .question(data.getQuestion())
                .category(data.getCategory())
                .timeLimit(data.getTimeLimit())
                .response(responseDto)
                .feedback(feedbackDto)
                .build();
    }

    private List<MockQuestionData> generateQuestionsForSession(MockInterviewSettingsDto settings) {
        List<MockQuestionData> result = new ArrayList<>();
        int questionCount = settings.getQuestionCount() != null ? settings.getQuestionCount() : 5;

        // Mix of categories based on difficulty
        List<String> categories = List.of("BEHAVIORAL", "TECHNICAL", "SITUATIONAL");
        Random random = new Random();

        for (int i = 0; i < questionCount; i++) {
            String category = categories.get(i % categories.size());
            List<MockQuestion> categoryQuestions = MOCK_QUESTIONS_BY_CATEGORY.getOrDefault(
                    category, MOCK_QUESTIONS_BY_CATEGORY.get("BEHAVIORAL"));

            MockQuestion selected = categoryQuestions.get(random.nextInt(categoryQuestions.size()));

            result.add(MockQuestionData.builder()
                    .id(UUID.randomUUID().toString())
                    .index(i)
                    .question(selected.getQuestion())
                    .category(category)
                    .timeLimit(settings.getTimePerQuestion())
                    .build());
        }

        return result;
    }

    private int generateMockScore() {
        return 60 + new Random().nextInt(40); // 60-99
    }

    private MockInterviewQuestionDto.FeedbackDto generateMockFeedback(int score, String category) {
        String overallFeedback;
        String[] strengths;
        String[] improvements;
        String suggestedResponse;

        if (score >= 90) {
            overallFeedback = "Excellent response! You demonstrated strong communication skills and provided specific, relevant examples.";
            strengths = new String[]{"Clear and structured answer", "Good use of specific examples", "Confident delivery"};
            improvements = new String[]{"Consider adding quantifiable results"};
            suggestedResponse = "Your answer was comprehensive. To make it even stronger, include metrics or specific outcomes.";
        } else if (score >= 80) {
            overallFeedback = "Good response! You covered the key points well but could add more specific details.";
            strengths = new String[]{"Addressed the question directly", "Good overall structure"};
            improvements = new String[]{"Add more specific examples", "Include measurable outcomes"};
            suggestedResponse = "Try using the STAR method (Situation, Task, Action, Result) to structure your response more effectively.";
        } else if (score >= 70) {
            overallFeedback = "Decent response. Focus on being more specific and providing concrete examples.";
            strengths = new String[]{"Understood the question"};
            improvements = new String[]{"Be more specific", "Use concrete examples", "Improve structure"};
            suggestedResponse = "Start with a brief overview, then provide a specific example, and conclude with the impact or result.";
        } else {
            overallFeedback = "Your response needs improvement. Focus on answering the question directly with specific examples.";
            strengths = new String[]{"Attempted to address the question"};
            improvements = new String[]{"Answer the question more directly", "Provide specific examples", "Practice structured responses"};
            suggestedResponse = "Practice the STAR method and prepare 3-5 strong examples from your experience that you can adapt to different questions.";
        }

        return MockInterviewQuestionDto.FeedbackDto.builder()
                .score(score)
                .overallFeedback(overallFeedback)
                .strengths(strengths)
                .improvements(improvements)
                .suggestedResponse(suggestedResponse)
                .build();
    }

    private OverallFeedback generateOverallFeedback(int overallScore, List<MockQuestionData> questions) {
        String summary;
        String[] keyStrengths;
        String[] areasToImprove;
        String[] recommendations;

        if (overallScore >= 85) {
            summary = "Outstanding performance! You demonstrated excellent interview skills across all questions.";
            keyStrengths = new String[]{"Strong communication skills", "Excellent use of examples", "Confident and professional demeanor"};
            areasToImprove = new String[]{"Continue to quantify achievements", "Practice for even more challenging scenarios"};
            recommendations = new String[]{"You're well-prepared for interviews", "Focus on company-specific preparation"};
        } else if (overallScore >= 70) {
            summary = "Good performance! You showed solid interview skills with room for improvement.";
            keyStrengths = new String[]{"Good understanding of questions", "Adequate response structure"};
            areasToImprove = new String[]{"Add more specific examples", "Improve response timing", "Work on confidence"};
            recommendations = new String[]{"Practice with more mock interviews", "Prepare 5-7 strong STAR stories"};
        } else {
            summary = "This interview highlighted areas for improvement. Keep practicing!";
            keyStrengths = new String[]{"Willingness to practice"};
            areasToImprove = new String[]{"Response structure", "Specific examples", "Time management"};
            recommendations = new String[]{"Review common interview questions", "Practice the STAR method", "Record yourself and review"};
        }

        return OverallFeedback.builder()
                .overallScore(overallScore)
                .summary(summary)
                .keyStrengths(keyStrengths)
                .areasToImprove(areasToImprove)
                .recommendations(recommendations)
                .questionCount(questions.size())
                .completedAt(Instant.now().toString())
                .build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON", e);
            return "{}";
        }
    }

    private List<MockQuestionData> parseQuestions(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<MockQuestionData>>() {});
        } catch (Exception e) {
            log.error("Failed to parse questions JSON", e);
            return new ArrayList<>();
        }
    }

    private MockInterviewSettingsDto parseSettings(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MockInterviewSettingsDto.class);
        } catch (Exception e) {
            log.error("Failed to parse settings JSON", e);
            return null;
        }
    }

    private static Map<String, List<MockQuestion>> createMockQuestions() {
        Map<String, List<MockQuestion>> map = new HashMap<>();

        // Behavioral Questions
        map.put("BEHAVIORAL", List.of(
                new MockQuestion("Tell me about a time you had to deal with a difficult team member."),
                new MockQuestion("Describe a situation where you had to meet a tight deadline."),
                new MockQuestion("Give an example of when you showed leadership."),
                new MockQuestion("Tell me about a time you failed and what you learned."),
                new MockQuestion("Describe a conflict you had at work and how you resolved it."),
                new MockQuestion("Tell me about your greatest professional achievement."),
                new MockQuestion("Describe a time when you had to adapt to a significant change."),
                new MockQuestion("Give an example of when you went above and beyond your job duties.")
        ));

        // Technical Questions
        map.put("TECHNICAL", List.of(
                new MockQuestion("Walk me through your approach to solving complex problems."),
                new MockQuestion("Describe a challenging technical project you worked on."),
                new MockQuestion("How do you stay updated with the latest technologies?"),
                new MockQuestion("Explain a technical concept to me as if I were non-technical."),
                new MockQuestion("Describe your debugging process when something goes wrong."),
                new MockQuestion("How do you prioritize technical debt versus new features?"),
                new MockQuestion("Tell me about a time you optimized a system or process."),
                new MockQuestion("How do you ensure code quality in your projects?")
        ));

        // Situational Questions
        map.put("SITUATIONAL", List.of(
                new MockQuestion("How would you handle a disagreement with your manager?"),
                new MockQuestion("What would you do if you discovered a colleague was underperforming?"),
                new MockQuestion("How would you prioritize multiple urgent tasks?"),
                new MockQuestion("What would you do if you realized you made a significant mistake?"),
                new MockQuestion("How would you handle receiving critical feedback?"),
                new MockQuestion("What would you do if you were asked to do something unethical?"),
                new MockQuestion("How would you approach a project with unclear requirements?"),
                new MockQuestion("What would you do if your team disagreed on an important decision?")
        ));

        return map;
    }

    // ==================== Inner Classes ====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class MockQuestion {
        private String question;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class MockQuestionData {
        private String id;
        private Integer index;
        private String question;
        private String category;
        private Integer timeLimit;
        private Response response;
        private Feedback feedback;

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class Response {
            private String type;
            private String content;
            private String mediaUrl;
            private Integer durationSeconds;
            private String submittedAt;
        }

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class Feedback {
            private Integer score;
            private String overallFeedback;
            private String[] strengths;
            private String[] improvements;
            private String suggestedResponse;
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class OverallFeedback {
        private Integer overallScore;
        private String summary;
        private String[] keyStrengths;
        private String[] areasToImprove;
        private String[] recommendations;
        private Integer questionCount;
        private String completedAt;
    }
}
