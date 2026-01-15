package com.cvibe.biz.interview.service;

import com.cvibe.common.response.ErrorCode;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.biz.interview.dto.*;
import com.cvibe.biz.interview.entity.InterviewAnswer;
import com.cvibe.biz.interview.entity.InterviewSession;
import com.cvibe.biz.interview.entity.QuestionTemplate;
import com.cvibe.biz.interview.repository.InterviewAnswerRepository;
import com.cvibe.biz.interview.repository.InterviewSessionRepository;
import com.cvibe.biz.interview.repository.QuestionTemplateRepository;
import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final QuestionTemplateRepository questionTemplateRepository;
    private final UserRepository userRepository;

    /**
     * Start a new interview session
     */
    @Transactional
    public InterviewStateResponse startSession(UUID userId, StartSessionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Check for existing active sessions
        List<InterviewSession> activeSessions = sessionRepository.findActiveSessionsByUserId(userId);
        if (!activeSessions.isEmpty()) {
            // Return existing active session instead of creating new one
            InterviewSession existingSession = activeSessions.get(0);
            return getSessionState(userId, existingSession.getId());
        }

        InterviewSession.SessionType sessionType;
        try {
            sessionType = InterviewSession.SessionType.valueOf(request.getSessionType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid session type");
        }

        String language = request.getLanguage() != null ? request.getLanguage() : "en";

        // Get questions based on session type
        List<QuestionTemplate> questions = selectQuestionsForSession(sessionType, request.getFocusArea(), language);
        
        if (questions.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "No questions available for this session type");
        }

        // Create session
        InterviewSession session = InterviewSession.builder()
                .user(user)
                .sessionType(sessionType)
                .status(InterviewSession.SessionStatus.IN_PROGRESS)
                .currentQuestionIndex(0)
                .totalQuestions(questions.size())
                .focusArea(request.getFocusArea())
                .targetRole(request.getTargetRole())
                .extractionStatus(InterviewSession.ExtractionStatus.PENDING)
                .startedAt(Instant.now())
                .lastActivityAt(Instant.now())
                .build();

        session = sessionRepository.save(session);

        // Create answer placeholders for each question
        createAnswerPlaceholders(session, questions);

        return getSessionState(userId, session.getId());
    }

    /**
     * Get current session state
     */
    @Transactional(readOnly = true)
    public InterviewStateResponse getSessionState(UUID userId, UUID sessionId) {
        InterviewSession session = getSessionForUser(userId, sessionId);
        
        List<InterviewAnswer> allAnswers = answerRepository.findBySessionIdOrderByQuestionOrder(sessionId);
        List<InterviewAnswer> answered = allAnswers.stream()
                .filter(a -> a.getAnswerText() != null)
                .collect(Collectors.toList());
        
        Optional<InterviewAnswer> nextUnanswered = answerRepository.findNextUnanswered(sessionId);

        boolean hasMore = nextUnanswered.isPresent();
        boolean completed = session.getStatus() == InterviewSession.SessionStatus.COMPLETED;

        InterviewSessionDto sessionDto = InterviewSessionDto.from(session);
        sessionDto.setAnsweredCount(answered.size());
        sessionDto.setProgressPercentage(
                session.getTotalQuestions() > 0 
                        ? (double) answered.size() / session.getTotalQuestions() * 100 
                        : 0.0
        );

        String nextAction = completed ? "COMPLETE" : (hasMore ? "ANSWER" : "REVIEW");

        return InterviewStateResponse.builder()
                .session(sessionDto)
                .currentQuestion(nextUnanswered.map(InterviewAnswerDto::from).orElse(null))
                .answeredQuestions(answered.stream().map(InterviewAnswerDto::from).collect(Collectors.toList()))
                .hasMoreQuestions(hasMore)
                .sessionCompleted(completed)
                .nextAction(nextAction)
                .build();
    }

    /**
     * Submit an answer
     */
    @Transactional
    public InterviewStateResponse submitAnswer(UUID userId, UUID sessionId, SubmitAnswerRequest request) {
        InterviewSession session = getSessionForUser(userId, sessionId);

        if (session.getStatus() != InterviewSession.SessionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Session is not in progress");
        }

        InterviewAnswer answer = answerRepository.findById(request.getAnswerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Answer not found"));

        if (!answer.getSession().getId().equals(sessionId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Answer does not belong to this session");
        }

        // Update answer
        answer.setAnswerText(request.getAnswerText());
        answer.setAnsweredAt(Instant.now());
        answerRepository.save(answer);

        // Update session
        session.setLastActivityAt(Instant.now());
        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);

        // Check if session is complete
        long answeredCount = answerRepository.countAnsweredBySessionId(sessionId);
        if (answeredCount >= session.getTotalQuestions()) {
            session.setStatus(InterviewSession.SessionStatus.COMPLETED);
            session.setCompletedAt(Instant.now());
        }

        sessionRepository.save(session);

        return getSessionState(userId, sessionId);
    }

    /**
     * Add a follow-up question dynamically
     * This would be called by AI service when more details are needed
     */
    @Transactional
    public InterviewAnswerDto addFollowUpQuestion(UUID sessionId, UUID parentAnswerId, String questionText) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));

        InterviewAnswer parentAnswer = answerRepository.findById(parentAnswerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Parent answer not found"));

        // Find max question order
        List<InterviewAnswer> allAnswers = answerRepository.findBySessionIdOrderByQuestionOrder(sessionId);
        int maxOrder = allAnswers.stream()
                .mapToInt(InterviewAnswer::getQuestionOrder)
                .max()
                .orElse(0);

        InterviewAnswer followUp = InterviewAnswer.builder()
                .session(session)
                .questionOrder(maxOrder + 1)
                .questionText(questionText)
                .isFollowUp(true)
                .followUpDepth(parentAnswer.getFollowUpDepth() + 1)
                .parentAnswerId(parentAnswer.getId())
                .needsClarification(false)
                .build();

        followUp = answerRepository.save(followUp);

        // Update session total questions
        session.setTotalQuestions(session.getTotalQuestions() + 1);
        sessionRepository.save(session);

        return InterviewAnswerDto.from(followUp);
    }

    /**
     * Pause a session
     */
    @Transactional
    public InterviewSessionDto pauseSession(UUID userId, UUID sessionId) {
        InterviewSession session = getSessionForUser(userId, sessionId);

        if (session.getStatus() != InterviewSession.SessionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Session is not in progress");
        }

        session.setStatus(InterviewSession.SessionStatus.PAUSED);
        session.setLastActivityAt(Instant.now());
        session = sessionRepository.save(session);

        return InterviewSessionDto.from(session);
    }

    /**
     * Resume a paused session
     */
    @Transactional
    public InterviewStateResponse resumeSession(UUID userId, UUID sessionId) {
        InterviewSession session = getSessionForUser(userId, sessionId);

        if (session.getStatus() != InterviewSession.SessionStatus.PAUSED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Session is not paused");
        }

        session.setStatus(InterviewSession.SessionStatus.IN_PROGRESS);
        session.setLastActivityAt(Instant.now());
        sessionRepository.save(session);

        return getSessionState(userId, sessionId);
    }

    /**
     * Abandon a session
     */
    @Transactional
    public void abandonSession(UUID userId, UUID sessionId) {
        InterviewSession session = getSessionForUser(userId, sessionId);

        session.setStatus(InterviewSession.SessionStatus.ABANDONED);
        session.setLastActivityAt(Instant.now());
        sessionRepository.save(session);
    }

    /**
     * Get all sessions for a user
     */
    @Transactional(readOnly = true)
    public List<InterviewSessionDto> getUserSessions(UUID userId) {
        List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return sessions.stream()
                .map(s -> {
                    InterviewSessionDto dto = InterviewSessionDto.from(s);
                    long answered = answerRepository.countAnsweredBySessionId(s.getId());
                    dto.setAnsweredCount((int) answered);
                    dto.setProgressPercentage(
                            s.getTotalQuestions() > 0 
                                    ? (double) answered / s.getTotalQuestions() * 100 
                                    : 0.0
                    );
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all answers for a session
     */
    @Transactional(readOnly = true)
    public List<InterviewAnswerDto> getSessionAnswers(UUID userId, UUID sessionId) {
        getSessionForUser(userId, sessionId);  // Validate access
        List<InterviewAnswer> answers = answerRepository.findBySessionIdOrderByQuestionOrder(sessionId);
        return answers.stream().map(InterviewAnswerDto::from).collect(Collectors.toList());
    }

    // ================== Admin Methods ==================

    /**
     * Get all question templates (admin)
     */
    @Transactional(readOnly = true)
    public List<QuestionTemplateDto> getAllQuestionTemplates() {
        return questionTemplateRepository.findByIsActiveTrueOrderByOrderWeight()
                .stream()
                .map(QuestionTemplateDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Create a new question template (admin)
     */
    @Transactional
    public QuestionTemplateDto createQuestionTemplate(QuestionTemplate template) {
        template = questionTemplateRepository.save(template);
        return QuestionTemplateDto.from(template);
    }

    /**
     * Update question template (admin)
     */
    @Transactional
    public QuestionTemplateDto updateQuestionTemplate(UUID id, QuestionTemplate updates) {
        QuestionTemplate existing = questionTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Question template not found"));

        if (updates.getQuestionText() != null) existing.setQuestionText(updates.getQuestionText());
        if (updates.getCategory() != null) existing.setCategory(updates.getCategory());
        if (updates.getSubcategory() != null) existing.setSubcategory(updates.getSubcategory());
        if (updates.getQuestionType() != null) existing.setQuestionType(updates.getQuestionType());
        if (updates.getDifficultyLevel() != null) existing.setDifficultyLevel(updates.getDifficultyLevel());
        if (updates.getOrderWeight() != null) existing.setOrderWeight(updates.getOrderWeight());
        if (updates.getIsRequired() != null) existing.setIsRequired(updates.getIsRequired());
        if (updates.getIsActive() != null) existing.setIsActive(updates.getIsActive());
        if (updates.getFollowUpPrompts() != null) existing.setFollowUpPrompts(updates.getFollowUpPrompts());
        if (updates.getExtractionHints() != null) existing.setExtractionHints(updates.getExtractionHints());

        existing = questionTemplateRepository.save(existing);
        return QuestionTemplateDto.from(existing);
    }

    /**
     * Deactivate question template (admin)
     */
    @Transactional
    public void deactivateQuestionTemplate(UUID id) {
        QuestionTemplate template = questionTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Question template not found"));
        template.setIsActive(false);
        questionTemplateRepository.save(template);
    }

    // ================== Helper Methods ==================

    private InterviewSession getSessionForUser(UUID userId, UUID sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Session does not belong to this user");
        }

        return session;
    }

    private List<QuestionTemplate> selectQuestionsForSession(
            InterviewSession.SessionType sessionType, 
            String focusArea, 
            String language) {

        List<QuestionTemplate> questions;

        switch (sessionType) {
            case INITIAL_PROFILE:
                // Get required questions plus some standard ones
                questions = questionTemplateRepository.findByIsRequiredTrueAndIsActiveTrueOrderByOrderWeight();
                List<QuestionTemplate> standard = questionTemplateRepository.findByDifficultyLevel(
                        QuestionTemplate.DifficultyLevel.STANDARD);
                questions.addAll(standard);
                break;

            case DEEP_DIVE:
                // Get detailed and deep dive questions
                questions = questionTemplateRepository.findByDifficultyLevel(
                        QuestionTemplate.DifficultyLevel.DETAILED);
                questions.addAll(questionTemplateRepository.findByDifficultyLevel(
                        QuestionTemplate.DifficultyLevel.DEEP_DIVE));
                break;

            case UPDATE_EXPERIENCE:
                questions = questionTemplateRepository.findByCategoryAndLanguage(
                        QuestionTemplate.QuestionCategory.WORK_EXPERIENCE, language);
                break;

            case UPDATE_EDUCATION:
                questions = questionTemplateRepository.findByCategoryAndLanguage(
                        QuestionTemplate.QuestionCategory.EDUCATION, language);
                break;

            case UPDATE_SKILLS:
                questions = questionTemplateRepository.findByCategoryAndLanguage(
                        QuestionTemplate.QuestionCategory.SKILLS, language);
                break;

            case CAREER_GOALS:
                questions = questionTemplateRepository.findByCategoryAndLanguage(
                        QuestionTemplate.QuestionCategory.CAREER_GOALS, language);
                break;

            default:
                questions = questionTemplateRepository.findActiveByLanguage(language);
        }

        // Remove duplicates and sort by order weight
        return questions.stream()
                .distinct()
                .sorted(Comparator.comparing(QuestionTemplate::getOrderWeight))
                .collect(Collectors.toList());
    }

    private void createAnswerPlaceholders(InterviewSession session, List<QuestionTemplate> questions) {
        int order = 0;
        for (QuestionTemplate template : questions) {
            InterviewAnswer answer = InterviewAnswer.builder()
                    .session(session)
                    .question(template)
                    .questionOrder(order++)
                    .questionText(template.getQuestionText())
                    .isFollowUp(false)
                    .followUpDepth(0)
                    .needsClarification(false)
                    .build();
            answerRepository.save(answer);
        }
    }
}
