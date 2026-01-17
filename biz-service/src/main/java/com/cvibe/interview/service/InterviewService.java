package com.cvibe.interview.service;

import com.cvibe.auth.entity.User;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import com.cvibe.interview.dto.*;
import com.cvibe.interview.entity.*;
import com.cvibe.interview.repository.InterviewSessionAnswerRepository;
import com.cvibe.interview.repository.InterviewSessionRepository;
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
