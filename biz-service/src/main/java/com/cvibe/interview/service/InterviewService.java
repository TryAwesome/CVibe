package com.cvibe.interview.service;

import com.cvibe.auth.entity.User;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import com.cvibe.common.grpc.AIEngineClient;
import com.cvibe.interview.dto.*;
import com.cvibe.interview.entity.*;
import com.cvibe.interview.repository.InterviewSessionAnswerRepository;
import com.cvibe.interview.repository.InterviewSessionRepository;
import com.cvibe.profile.service.ProfileService;
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
 * Service for managing interview sessions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewSessionAnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AIEngineClient aiEngineClient;
    private final ProfileService profileService;

    // Mock questions for different focus areas
    private static final Map<FocusArea, List<MockQuestion>> MOCK_QUESTIONS = createMockQuestions();

    /**
     * Create a new interview session
     */
    @Transactional
    public SessionStateResponse createSession(UUID userId, CreateSessionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Parse session type
        SessionType sessionType;
        try {
            sessionType = SessionType.valueOf(request.getSessionType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INTERVIEW_TYPE);
        }

        // Parse focus area if provided
        FocusArea focusArea = null;
        if (request.getFocusArea() != null && !request.getFocusArea().isEmpty()) {
            try {
                focusArea = FocusArea.valueOf(request.getFocusArea());
            } catch (IllegalArgumentException e) {
                // Use default
                focusArea = FocusArea.WORK_EXPERIENCE;
            }
        }

        // Generate questions for this session
        List<MockQuestion> questions = generateQuestionsForSession(sessionType, focusArea);

        Instant now = Instant.now();
        InterviewSession session = InterviewSession.builder()
                .user(user)
                .sessionType(sessionType)
                .status(SessionStatus.IN_PROGRESS)
                .focusArea(focusArea != null ? focusArea : FocusArea.WORK_EXPERIENCE)
                .targetRole(request.getTargetRole())
                .language(request.getLanguage() != null ? request.getLanguage() : "en")
                .currentQuestionIndex(0)
                .totalQuestions(questions.size())
                .extractionStatus(ExtractionStatus.PENDING)
                .questionsJson(toJson(questions))
                .startedAt(now)
                .lastActivityAt(now)
                .build();

        session = sessionRepository.save(session);
        log.info("Created interview session {} for user {}", session.getId(), userId);

        return buildSessionStateResponse(session, questions);
    }

    /**
     * Get session by ID
     */
    @Transactional(readOnly = true)
    public SessionStateResponse getSession(UUID userId, UUID sessionId) {
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        List<MockQuestion> questions = parseQuestions(session.getQuestionsJson());
        return buildSessionStateResponse(session, questions);
    }

    /**
     * Get all sessions for a user
     */
    @Transactional(readOnly = true)
    public List<InterviewSessionDto> getSessions(UUID userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(InterviewSessionDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get paginated sessions for a user
     */
    @Transactional(readOnly = true)
    public Page<InterviewSessionDto> getSessions(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(InterviewSessionDto::fromEntity);
    }

    /**
     * Submit an answer to a question
     */
    @Transactional
    public AnswerSubmitResponse submitAnswer(UUID userId, UUID sessionId, SubmitAnswerRequest request) {
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.SESSION_NOT_ACTIVE);
        }

        List<MockQuestion> questions = parseQuestions(session.getQuestionsJson());
        
        // Find the question being answered
        MockQuestion currentQuestion = questions.stream()
                .filter(q -> q.getId().equals(request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));

        // Generate mock feedback
        int score = generateMockScore();
        String feedback = generateMockFeedback(score);

        // Save the answer
        InterviewSessionAnswer answer = InterviewSessionAnswer.builder()
                .session(session)
                .questionId(request.getQuestionId())
                .question(currentQuestion.getQuestion())
                .answer(request.getAnswer())
                .category(currentQuestion.getCategory())
                .score(score)
                .feedback(feedback)
                .questionOrder(session.getCurrentQuestionIndex())
                .build();

        answerRepository.save(answer);

        // Update session
        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);
        session.setLastActivityAt(Instant.now());

        boolean hasMoreQuestions = session.getCurrentQuestionIndex() < questions.size();
        InterviewQuestionDto nextQuestion = null;

        if (hasMoreQuestions) {
            MockQuestion next = questions.get(session.getCurrentQuestionIndex());
            nextQuestion = InterviewQuestionDto.builder()
                    .id(UUID.randomUUID().toString())
                    .questionId(next.getId().toString())
                    .question(next.getQuestion())
                    .category(next.getCategory())
                    .build();
        } else {
            // Session is completed
            session.setStatus(SessionStatus.COMPLETED);
            session.setCompletedAt(Instant.now());
            session.setExtractionStatus(ExtractionStatus.COMPLETED);
        }

        sessionRepository.save(session);

        log.info("Answer submitted for session {} question {}", sessionId, request.getQuestionId());

        return AnswerSubmitResponse.builder()
                .accepted(true)
                .score(score)
                .feedback(feedback)
                .nextQuestion(nextQuestion)
                .hasMoreQuestions(hasMoreQuestions)
                .sessionCompleted(!hasMoreQuestions)
                .build();
    }

    /**
     * Get all answers for a session
     */
    @Transactional(readOnly = true)
    public List<InterviewQuestionDto> getSessionAnswers(UUID userId, UUID sessionId) {
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        return answerRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId)
                .stream()
                .map(InterviewQuestionDto::fromAnswer)
                .collect(Collectors.toList());
    }

    /**
     * Pause a session
     */
    @Transactional
    public InterviewSessionDto pauseSession(UUID userId, UUID sessionId) {
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.SESSION_NOT_ACTIVE);
        }

        session.setStatus(SessionStatus.PAUSED);
        session.setLastActivityAt(Instant.now());
        session = sessionRepository.save(session);

        log.info("Session {} paused", sessionId);
        return InterviewSessionDto.fromEntity(session);
    }

    /**
     * Resume a paused session
     */
    @Transactional
    public SessionStateResponse resumeSession(UUID userId, UUID sessionId) {
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getStatus() != SessionStatus.PAUSED) {
            throw new BusinessException(ErrorCode.SESSION_NOT_ACTIVE);
        }

        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setLastActivityAt(Instant.now());
        session = sessionRepository.save(session);

        List<MockQuestion> questions = parseQuestions(session.getQuestionsJson());
        log.info("Session {} resumed", sessionId);
        return buildSessionStateResponse(session, questions);
    }

    /**
     * Delete a session
     */
    @Transactional
    public void deleteSession(UUID userId, UUID sessionId) {
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        answerRepository.deleteBySessionId(sessionId);
        sessionRepository.delete(session);

        log.info("Session {} deleted", sessionId);
    }

    // ==================== Profile Interview (AI-powered) ====================

    /**
     * Start a profile collection interview session via AI Engine
     */
    @Transactional
    public ProfileInterviewStartResponse startProfileInterview(UUID userId, StartProfileInterviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String sessionId = UUID.randomUUID().toString();
        String language = request.getLanguage() != null ? request.getLanguage() : "zh";

        // Get existing profile JSON if any
        String existingProfile = null;
        try {
            var profile = profileService.getProfile(userId);
            if (profile != null) {
                existingProfile = objectMapper.writeValueAsString(profile);
            }
        } catch (Exception e) {
            log.debug("No existing profile for user {}", userId);
        }

        // Call AI Engine
        AIEngineClient.StartProfileInterviewResult result = aiEngineClient.startProfileInterview(
                userId.toString(),
                sessionId,
                language,
                existingProfile
        );

        if (!result.isSuccess()) {
            throw new BusinessException(ErrorCode.AI_ENGINE_ERROR);
        }

        // Create session record in database
        Instant now = Instant.now();
        InterviewSession session = InterviewSession.builder()
                .user(user)
                .sessionType(SessionType.PROFILE_INTERVIEW)
                .status(SessionStatus.IN_PROGRESS)
                .focusArea(FocusArea.WORK_EXPERIENCE)
                .language(language)
                .currentQuestionIndex(0)
                .totalQuestions(0)
                .extractionStatus(ExtractionStatus.PENDING)
                .startedAt(now)
                .lastActivityAt(now)
                .build();

        // Store AI session ID in questions JSON field
        session.setQuestionsJson(objectMapper.createObjectNode()
                .put("aiSessionId", sessionId)
                .put("currentPhase", result.getCurrentPhase())
                .toString());

        session = sessionRepository.save(session);
        log.info("Created profile interview session {} for user {}", session.getId(), userId);

        return ProfileInterviewStartResponse.builder()
                .sessionId(session.getId().toString())
                .aiSessionId(sessionId)
                .welcomeMessage(result.getWelcomeMessage())
                .firstQuestion(result.getFirstQuestion())
                .currentPhase(result.getCurrentPhase())
                .build();
    }

    /**
     * Send a message in profile interview session
     */
    @Transactional
    public ProfileInterviewMessageResponse sendProfileInterviewMessage(
            UUID userId,
            UUID sessionId,
            ProfileInterviewMessageRequest request
    ) {
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.SESSION_NOT_ACTIVE);
        }

        // Get AI session ID from stored data
        String aiSessionId;
        try {
            var node = objectMapper.readTree(session.getQuestionsJson());
            aiSessionId = node.has("aiSessionId") ? node.get("aiSessionId").asText() : sessionId.toString();
        } catch (Exception e) {
            aiSessionId = sessionId.toString();
        }

        // Collect response from AI Engine
        StringBuilder responseBuilder = new StringBuilder();
        final String[] currentPhase = {""};
        final Throwable[] error = {null};

        aiEngineClient.sendProfileInterviewMessage(
                aiSessionId,
                request.getMessage(),
                chunk -> responseBuilder.append(chunk),
                phase -> currentPhase[0] = phase,
                () -> {},
                err -> error[0] = err
        );

        // Check for errors
        if (error[0] != null) {
            log.error("AI Engine error: {}", error[0].getMessage());
            throw new BusinessException(ErrorCode.AI_ENGINE_ERROR);
        }

        String response = responseBuilder.toString();
        String phase = currentPhase[0];

        // If response is empty, return a fallback message
        if (response.isEmpty()) {
            log.warn("Empty response from AI Engine for session {}", sessionId);
            response = "抱歉，我需要一点时间来处理。请稍后再试，或者您可以继续描述您的背景。";
        }

        // Update session
        session.setLastActivityAt(Instant.now());
        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);

        // Update phase in stored data
        try {
            var node = objectMapper.readTree(session.getQuestionsJson());
            var updatedNode = objectMapper.createObjectNode();
            node.fieldNames().forEachRemaining(f -> updatedNode.set(f, node.get(f)));
            if (!phase.isEmpty()) {
                updatedNode.put("currentPhase", phase);
            }
            session.setQuestionsJson(updatedNode.toString());
        } catch (Exception e) {
            log.warn("Failed to update phase", e);
        }

        // Save answer record
        InterviewSessionAnswer answer = InterviewSessionAnswer.builder()
                .session(session)
                .questionId(UUID.randomUUID())
                .question(request.getMessage())
                .answer(response)
                .category(phase.isEmpty() ? "GENERAL" : phase.toUpperCase())
                .questionOrder(session.getCurrentQuestionIndex())
                .build();
        answerRepository.save(answer);

        sessionRepository.save(session);

        // Determine phase name based on phase code
        String phaseName = getPhaseName(phase);

        return ProfileInterviewMessageResponse.builder()
                .response(response)
                .currentPhase(phase)
                .phaseName(phaseName)
                .turnCount(session.getCurrentQuestionIndex())
                .isComplete(false)
                .build();
    }

    /**
     * Convert phase code to human-readable phase name
     */
    private String getPhaseName(String phase) {
        if (phase == null || phase.isEmpty()) {
            return "对话中";
        }
        switch (phase.toLowerCase()) {
            case "intro":
                return "自我介绍";
            case "work":
            case "work_experience":
                return "工作经历";
            case "education":
                return "教育背景";
            case "skills":
                return "技能特长";
            case "projects":
                return "项目经验";
            case "goals":
                return "职业目标";
            case "summary":
                return "总结";
            default:
                return "对话中";
        }
    }

    /**
     * Get profile interview state
     */
    @Transactional(readOnly = true)
    public ProfileInterviewStateResponse getProfileInterviewState(UUID userId, UUID sessionId) {
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // Get AI session ID
        String aiSessionId;
        String currentPhase = "";
        try {
            var node = objectMapper.readTree(session.getQuestionsJson());
            aiSessionId = node.has("aiSessionId") ? node.get("aiSessionId").asText() : sessionId.toString();
            currentPhase = node.has("currentPhase") ? node.get("currentPhase").asText() : "";
        } catch (Exception e) {
            aiSessionId = sessionId.toString();
        }

        // Get state from AI Engine
        var result = aiEngineClient.getProfileInterviewState(aiSessionId);

        return ProfileInterviewStateResponse.builder()
                .sessionId(sessionId.toString())
                .currentPhase(result.isSuccess() ? result.getCurrentPhase() : currentPhase)
                .phaseName(result.isSuccess() ? result.getPhaseName() : "")
                .turnCount(session.getCurrentQuestionIndex())
                .status(session.getStatus().name())
                .portraitSummary(result.isSuccess() ? result.getPortraitSummary() : "")
                .build();
    }

    /**
     * Finish profile interview and sync to profile
     */
    @Transactional
    public ProfileInterviewFinishResponse finishProfileInterview(UUID userId, UUID sessionId) {
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // Get AI session ID
        String aiSessionId;
        try {
            var node = objectMapper.readTree(session.getQuestionsJson());
            aiSessionId = node.has("aiSessionId") ? node.get("aiSessionId").asText() : sessionId.toString();
        } catch (Exception e) {
            aiSessionId = sessionId.toString();
        }

        // Finish interview and get extracted profile
        var result = aiEngineClient.finishProfileInterview(aiSessionId);

        if (!result.isSuccess()) {
            log.error("Failed to finish profile interview: {}", result.getErrorMessage());
            throw new BusinessException(ErrorCode.AI_ENGINE_ERROR);
        }

        // Parse and sync profile
        try {
            var extractedProfile = objectMapper.readValue(result.getProfileJson(), Map.class);
            profileService.syncFromInterview(userId, extractedProfile);
            log.info("Profile synced from interview for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to sync profile from interview", e);
            // Continue even if sync fails - we still want to complete the session
        }

        // Update session status
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        session.setExtractionStatus(ExtractionStatus.COMPLETED);
        session.setExtractedData(result.getProfileJson());
        sessionRepository.save(session);

        log.info("Profile interview {} completed with score {}", sessionId, result.getCompletenessScore());

        return ProfileInterviewFinishResponse.builder()
                .success(true)
                .completenessScore(result.getCompletenessScore())
                .missingSections(result.getMissingSections())
                .message("Interview completed successfully. Profile has been updated.")
                .build();
    }

    // ==================== Helper Methods ====================

    private SessionStateResponse buildSessionStateResponse(InterviewSession session, List<MockQuestion> questions) {
        InterviewQuestionDto currentQuestion = null;
        
        if (session.getCurrentQuestionIndex() < questions.size()) {
            MockQuestion q = questions.get(session.getCurrentQuestionIndex());
            currentQuestion = InterviewQuestionDto.builder()
                    .id(UUID.randomUUID().toString())
                    .questionId(q.getId().toString())
                    .question(q.getQuestion())
                    .category(q.getCategory())
                    .build();
        }

        List<InterviewQuestionDto> answeredQuestions = answerRepository
                .findBySessionIdOrderByQuestionOrderAsc(session.getId())
                .stream()
                .map(InterviewQuestionDto::fromAnswer)
                .collect(Collectors.toList());

        boolean hasMore = session.getCurrentQuestionIndex() < questions.size();
        String nextAction = hasMore ? "ANSWER_QUESTION" : "VIEW_RESULTS";

        return SessionStateResponse.builder()
                .session(InterviewSessionDto.fromEntity(session))
                .currentQuestion(currentQuestion)
                .answeredQuestions(answeredQuestions)
                .hasMoreQuestions(hasMore)
                .sessionCompleted(session.getStatus() == SessionStatus.COMPLETED)
                .nextAction(nextAction)
                .build();
    }

    private List<MockQuestion> generateQuestionsForSession(SessionType type, FocusArea focusArea) {
        List<MockQuestion> questions = new ArrayList<>();
        
        // Get questions for the focus area
        FocusArea area = focusArea != null ? focusArea : FocusArea.WORK_EXPERIENCE;
        List<MockQuestion> areaQuestions = MOCK_QUESTIONS.getOrDefault(area, 
                MOCK_QUESTIONS.get(FocusArea.WORK_EXPERIENCE));
        
        // Select up to 10 questions
        int count = Math.min(10, areaQuestions.size());
        for (int i = 0; i < count; i++) {
            MockQuestion q = areaQuestions.get(i);
            questions.add(new MockQuestion(UUID.randomUUID(), q.getQuestion(), q.getCategory()));
        }

        return questions;
    }

    private int generateMockScore() {
        return 60 + new Random().nextInt(40); // 60-99
    }

    private String generateMockFeedback(int score) {
        if (score >= 90) {
            return "Excellent response! You provided clear, detailed information with specific examples.";
        } else if (score >= 80) {
            return "Good response! Consider adding more specific examples to strengthen your answer.";
        } else if (score >= 70) {
            return "Decent response. Try to be more specific and quantify your achievements where possible.";
        } else {
            return "Your answer could be improved. Focus on specific examples and outcomes.";
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON", e);
            return "[]";
        }
    }

    private List<MockQuestion> parseQuestions(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<MockQuestion>>() {});
        } catch (Exception e) {
            log.error("Failed to parse questions JSON", e);
            return new ArrayList<>();
        }
    }

    private static Map<FocusArea, List<MockQuestion>> createMockQuestions() {
        Map<FocusArea, List<MockQuestion>> map = new EnumMap<>(FocusArea.class);

        // Work Experience Questions
        map.put(FocusArea.WORK_EXPERIENCE, List.of(
            new MockQuestion(UUID.randomUUID(), "Tell me about yourself and your professional background.", "INTRO"),
            new MockQuestion(UUID.randomUUID(), "Describe your most recent role and key responsibilities.", "EXPERIENCE"),
            new MockQuestion(UUID.randomUUID(), "What was your biggest achievement in your current/last position?", "ACHIEVEMENT"),
            new MockQuestion(UUID.randomUUID(), "Describe a challenging project you worked on and how you handled it.", "PROBLEM_SOLVING"),
            new MockQuestion(UUID.randomUUID(), "How do you prioritize your work when you have multiple deadlines?", "ORGANIZATION"),
            new MockQuestion(UUID.randomUUID(), "Tell me about a time you had to learn a new skill quickly.", "ADAPTABILITY"),
            new MockQuestion(UUID.randomUUID(), "Describe your experience working in a team environment.", "TEAMWORK"),
            new MockQuestion(UUID.randomUUID(), "What technologies or tools are you most proficient with?", "SKILLS"),
            new MockQuestion(UUID.randomUUID(), "How do you stay updated with industry trends?", "GROWTH"),
            new MockQuestion(UUID.randomUUID(), "What are your career goals for the next 3-5 years?", "GOALS")
        ));

        // Skills Questions
        map.put(FocusArea.SKILLS, List.of(
            new MockQuestion(UUID.randomUUID(), "What are your core technical skills?", "TECHNICAL"),
            new MockQuestion(UUID.randomUUID(), "Describe a project where you applied your strongest skill.", "APPLICATION"),
            new MockQuestion(UUID.randomUUID(), "What skills are you currently working to improve?", "GROWTH"),
            new MockQuestion(UUID.randomUUID(), "How do you approach learning new technologies?", "LEARNING"),
            new MockQuestion(UUID.randomUUID(), "Describe your experience with [relevant technology].", "SPECIFIC"),
            new MockQuestion(UUID.randomUUID(), "What soft skills do you consider your strengths?", "SOFT_SKILLS"),
            new MockQuestion(UUID.randomUUID(), "How do you handle situations where you lack expertise?", "ADAPTABILITY"),
            new MockQuestion(UUID.randomUUID(), "What certifications or training have you completed?", "CREDENTIALS"),
            new MockQuestion(UUID.randomUUID(), "How do you apply your skills to solve complex problems?", "PROBLEM_SOLVING"),
            new MockQuestion(UUID.randomUUID(), "What tools and frameworks are you most comfortable with?", "TOOLS")
        ));

        // Leadership Questions
        map.put(FocusArea.LEADERSHIP, List.of(
            new MockQuestion(UUID.randomUUID(), "Describe your leadership style.", "STYLE"),
            new MockQuestion(UUID.randomUUID(), "Tell me about a time you led a team through a difficult situation.", "CHALLENGE"),
            new MockQuestion(UUID.randomUUID(), "How do you motivate team members?", "MOTIVATION"),
            new MockQuestion(UUID.randomUUID(), "Describe a time you had to give difficult feedback.", "FEEDBACK"),
            new MockQuestion(UUID.randomUUID(), "How do you handle conflicts within your team?", "CONFLICT"),
            new MockQuestion(UUID.randomUUID(), "What's your approach to delegating tasks?", "DELEGATION"),
            new MockQuestion(UUID.randomUUID(), "How do you develop and mentor junior team members?", "MENTORING"),
            new MockQuestion(UUID.randomUUID(), "Describe a successful project you led from start to finish.", "PROJECT"),
            new MockQuestion(UUID.randomUUID(), "How do you balance being a leader and a contributor?", "BALANCE"),
            new MockQuestion(UUID.randomUUID(), "What's the most important lesson you've learned as a leader?", "LESSONS")
        ));

        // Add default for other areas
        for (FocusArea area : FocusArea.values()) {
            if (!map.containsKey(area)) {
                map.put(area, map.get(FocusArea.WORK_EXPERIENCE));
            }
        }

        return map;
    }

    /**
     * Inner class for mock questions
     */
    private static class MockQuestion {
        private UUID id;
        private String question;
        private String category;

        public MockQuestion() {}

        public MockQuestion(UUID id, String question, String category) {
            this.id = id;
            this.question = question;
            this.category = category;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}
