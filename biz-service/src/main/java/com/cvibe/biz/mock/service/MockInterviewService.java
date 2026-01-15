package com.cvibe.biz.mock.service;

import com.cvibe.biz.mock.dto.*;
import com.cvibe.biz.mock.entity.*;
import com.cvibe.biz.mock.entity.InterviewRound.RoundStatus;
import com.cvibe.biz.mock.entity.InterviewRound.RoundType;
import com.cvibe.biz.mock.entity.MockInterview.DifficultyLevel;
import com.cvibe.biz.mock.entity.MockInterview.InterviewStatus;
import com.cvibe.biz.mock.entity.MockInterview.InterviewType;
import com.cvibe.biz.mock.entity.MockQuestion.QuestionCategory;
import com.cvibe.biz.mock.entity.MockQuestion.QuestionDifficulty;
import com.cvibe.biz.mock.repository.*;
import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MockInterviewService
 * 
 * Handles mock interview sessions, question generation, and answer evaluation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MockInterviewService {

    private final MockInterviewRepository interviewRepository;
    private final InterviewRoundRepository roundRepository;
    private final MockQuestionRepository questionRepository;
    private final MockAnswerRepository answerRepository;
    private final UserRepository userRepository;

    // ================== Interview Management ==================

    /**
     * Start a new mock interview
     */
    @Transactional
    public MockInterviewDto startInterview(UUID userId, StartInterviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        MockInterview interview = MockInterview.builder()
                .user(user)
                .targetPosition(request.getTargetPosition())
                .targetCompany(request.getTargetCompany())
                .interviewType(request.getInterviewType())
                .difficulty(request.getDifficulty())
                .totalQuestions(request.getQuestionCount())
                .skills(request.getSkills() != null ? String.join(",", request.getSkills()) : null)
                .status(InterviewStatus.CREATED)
                .answeredQuestions(0)
                .build();

        interview = interviewRepository.save(interview);

        // Generate questions based on type
        generateQuestions(interview, request);

        // Start the interview
        interview.start();
        interview = interviewRepository.save(interview);

        log.info("Started mock interview {} for user {}", interview.getId(), userId);
        return MockInterviewDto.from(interview, true);
    }

    /**
     * Get interview details
     */
    @Transactional(readOnly = true)
    public MockInterviewDto getInterview(UUID userId, UUID interviewId) {
        MockInterview interview = getInterviewForUser(userId, interviewId);
        return MockInterviewDto.from(interview, true);
    }

    /**
     * Get user's interview history
     */
    @Transactional(readOnly = true)
    public Page<MockInterviewDto> getInterviewHistory(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return interviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(i -> MockInterviewDto.from(i, false));
    }

    /**
     * Resume a paused/created interview
     */
    @Transactional
    public MockInterviewDto resumeInterview(UUID userId, UUID interviewId) {
        MockInterview interview = getInterviewForUser(userId, interviewId);
        
        if (!interview.canBeResumed()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Interview cannot be resumed");
        }

        interview.resume();
        interview = interviewRepository.save(interview);
        
        return MockInterviewDto.from(interview, true);
    }

    /**
     * Pause an interview
     */
    @Transactional
    public MockInterviewDto pauseInterview(UUID userId, UUID interviewId) {
        MockInterview interview = getInterviewForUser(userId, interviewId);
        
        if (!interview.isInProgress()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Interview is not in progress");
        }

        interview.pause();
        interview = interviewRepository.save(interview);
        
        return MockInterviewDto.from(interview);
    }

    /**
     * Cancel an interview
     */
    @Transactional
    public void cancelInterview(UUID userId, UUID interviewId) {
        MockInterview interview = getInterviewForUser(userId, interviewId);
        interview.setStatus(InterviewStatus.CANCELLED);
        interviewRepository.save(interview);
        log.info("Cancelled interview {} for user {}", interviewId, userId);
    }

    // ================== Questions ==================

    /**
     * Get current/next question
     */
    @Transactional(readOnly = true)
    public MockQuestionDto getNextQuestion(UUID userId, UUID interviewId) {
        MockInterview interview = getInterviewForUser(userId, interviewId);
        
        List<MockQuestion> nextQuestions = questionRepository.findNextQuestions(
                interviewId, PageRequest.of(0, 1));
        
        if (nextQuestions.isEmpty()) {
            return null;  // No more questions
        }

        return MockQuestionDto.forInterview(nextQuestions.get(0));
    }

    /**
     * Get specific question
     */
    @Transactional(readOnly = true)
    public MockQuestionDto getQuestion(UUID userId, UUID interviewId, Integer questionNumber) {
        getInterviewForUser(userId, interviewId);  // Verify access
        
        MockQuestion question = questionRepository.findByInterviewIdAndQuestionNumber(interviewId, questionNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Question not found"));

        return MockQuestionDto.from(question, question.getIsAnswered());
    }

    /**
     * Get all questions for an interview
     */
    @Transactional(readOnly = true)
    public List<MockQuestionDto> getAllQuestions(UUID userId, UUID interviewId) {
        MockInterview interview = getInterviewForUser(userId, interviewId);
        boolean showAnswers = interview.getStatus() == InterviewStatus.COMPLETED 
                           || interview.getStatus() == InterviewStatus.EVALUATED;

        return questionRepository.findByInterviewIdOrderByQuestionNumberAsc(interviewId)
                .stream()
                .map(q -> showAnswers ? MockQuestionDto.from(q, true) : MockQuestionDto.forInterview(q))
                .collect(Collectors.toList());
    }

    /**
     * Skip a question
     */
    @Transactional
    public MockQuestionDto skipQuestion(UUID userId, UUID interviewId, UUID questionId) {
        getInterviewForUser(userId, interviewId);

        MockQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        question.markSkipped();
        question = questionRepository.save(question);

        return MockQuestionDto.forInterview(question);
    }

    // ================== Answers ==================

    /**
     * Submit an answer
     */
    @Transactional
    public MockAnswerDto submitAnswer(UUID userId, UUID interviewId, SubmitAnswerRequest request) {
        MockInterview interview = getInterviewForUser(userId, interviewId);
        
        if (!interview.isInProgress()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Interview is not in progress");
        }

        MockQuestion question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Question not found"));

        if (question.getIsAnswered()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Question already answered");
        }

        // Create answer
        MockAnswer answer = MockAnswer.builder()
                .question(question)
                .answerText(request.getAnswerText())
                .codeAnswer(request.getCodeAnswer())
                .programmingLanguage(request.getProgrammingLanguage())
                .isEvaluated(false)
                .build();

        answer.recordSubmission(request.getStartedAt());
        answer = answerRepository.save(answer);

        // Update question status
        question.markAnswered();
        question.setAnswerEntity(answer);
        questionRepository.save(question);

        // Update interview progress
        interview.incrementAnswered();
        interviewRepository.save(interview);

        // Evaluate answer (TODO: Replace with AI evaluation)
        evaluateAnswer(answer, question);

        log.info("Submitted answer for question {} in interview {}", request.getQuestionId(), interviewId);
        return MockAnswerDto.from(answer);
    }

    /**
     * Get answer for a question
     */
    @Transactional(readOnly = true)
    public MockAnswerDto getAnswer(UUID userId, UUID questionId) {
        MockAnswer answer = answerRepository.findByQuestionId(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Answer not found"));

        if (!answer.getQuestion().getInterview().belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return MockAnswerDto.from(answer);
    }

    // ================== Evaluation & Feedback ==================

    /**
     * Generate overall feedback for completed interview
     */
    @Transactional
    public MockInterviewDto generateFeedback(UUID userId, UUID interviewId) {
        MockInterview interview = getInterviewForUser(userId, interviewId);

        if (interview.getStatus() != InterviewStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Interview not completed");
        }

        // Calculate scores
        List<MockAnswer> answers = answerRepository.findByInterviewId(interviewId);
        
        int technical = 0, communication = 0, problemSolving = 0;
        int count = 0;

        for (MockAnswer answer : answers) {
            if (answer.getIsEvaluated() && answer.getScore() != null) {
                technical += answer.getAccuracyScore() != null ? answer.getAccuracyScore() : answer.getScore();
                communication += answer.getClarityScore() != null ? answer.getClarityScore() : answer.getScore();
                problemSolving += answer.getCompletenessScore() != null ? answer.getCompletenessScore() : answer.getScore();
                count++;
            }
        }

        if (count > 0) {
            interview.recordScores(technical / count, communication / count, problemSolving / count);
        }

        // Generate feedback summary
        String feedback = generateFeedbackSummary(interview, answers);
        String strengths = identifyStrengths(answers);
        String improvements = identifyImprovements(answers);

        interview.setFeedbackSummary(feedback);
        interview.setStrengths(strengths);
        interview.setImprovements(improvements);
        interview.setStatus(InterviewStatus.EVALUATED);
        
        interview = interviewRepository.save(interview);

        log.info("Generated feedback for interview {}", interviewId);
        return MockInterviewDto.from(interview, true);
    }

    // ================== Dashboard ==================

    /**
     * Get mock interview summary for dashboard
     */
    @Transactional(readOnly = true)
    public MockInterviewSummary getSummary(UUID userId) {
        long total = interviewRepository.countByUserIdAndStatus(userId, InterviewStatus.EVALUATED)
                   + interviewRepository.countByUserIdAndStatus(userId, InterviewStatus.COMPLETED);
        long inProgress = interviewRepository.countByUserIdAndStatus(userId, InterviewStatus.IN_PROGRESS)
                        + interviewRepository.countByUserIdAndStatus(userId, InterviewStatus.PAUSED);

        Double avgScore = interviewRepository.getAverageScore(userId);
        Long totalSeconds = interviewRepository.getTotalDurationSeconds(userId);

        List<Integer> recentScores = interviewRepository.getScoreTrend(userId, PageRequest.of(0, 10));

        // Score by type
        Map<InterviewType, Double> scoreByType = new EnumMap<>(InterviewType.class);
        for (InterviewType type : InterviewType.values()) {
            Double typeScore = interviewRepository.getAverageScoreByType(userId, type);
            if (typeScore != null) {
                scoreByType.put(type, typeScore);
            }
        }

        // Get active interview
        MockInterview inProgressInterview = interviewRepository
                .findFirstByUserIdAndStatusOrderByUpdatedAtDesc(userId, InterviewStatus.IN_PROGRESS)
                .orElse(null);

        // Get recent interviews
        List<MockInterviewDto> recent = interviewRepository
                .findCompletedInterviews(userId, PageRequest.of(0, 5))
                .map(i -> MockInterviewDto.from(i, false))
                .getContent();

        return MockInterviewSummary.builder()
                .totalInterviews(total)
                .completedInterviews(total)
                .inProgressInterviews(inProgress)
                .averageScore(avgScore)
                .highestScore(recentScores.isEmpty() ? null : Collections.max(recentScores))
                .lowestScore(recentScores.isEmpty() ? null : Collections.min(recentScores))
                .totalPracticeMinutes(totalSeconds != null ? totalSeconds / 60 : 0)
                .scoreByType(scoreByType)
                .recentScores(recentScores)
                .performanceTrend(MockInterviewSummary.calculateTrend(recentScores))
                .inProgressInterview(inProgressInterview != null ? MockInterviewDto.from(inProgressInterview) : null)
                .recentInterviews(recent)
                .build();
    }

    // ================== Private Helper Methods ==================

    private MockInterview getInterviewForUser(UUID userId, UUID interviewId) {
        MockInterview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Interview not found"));

        if (!interview.belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return interview;
    }

    /**
     * Generate questions based on interview type
     * TODO: Replace with AI-powered question generation
     */
    private void generateQuestions(MockInterview interview, StartInterviewRequest request) {
        List<QuestionTemplate> templates = getQuestionTemplates(
                request.getInterviewType(), 
                request.getDifficulty(),
                request.getQuestionCount()
        );

        int questionNumber = 1;
        for (QuestionTemplate template : templates) {
            MockQuestion question = MockQuestion.builder()
                    .interview(interview)
                    .questionNumber(questionNumber++)
                    .category(template.category)
                    .difficulty(mapDifficulty(request.getDifficulty()))
                    .questionText(template.question)
                    .expectedPoints(template.expectedPoints)
                    .sampleAnswer(template.sampleAnswer)
                    .relatedSkill(template.skill)
                    .timeLimitSeconds(request.getTimeLimitPerQuestion())
                    .isAnswered(false)
                    .isSkipped(false)
                    .build();

            interview.addQuestion(question);
        }
    }

    private QuestionDifficulty mapDifficulty(DifficultyLevel level) {
        return switch (level) {
            case EASY -> QuestionDifficulty.EASY;
            case MEDIUM -> QuestionDifficulty.MEDIUM;
            case HARD -> QuestionDifficulty.HARD;
            case EXPERT -> QuestionDifficulty.EXPERT;
        };
    }

    /**
     * Evaluate an answer
     * TODO: Replace with AI evaluation via gRPC
     */
    private void evaluateAnswer(MockAnswer answer, MockQuestion question) {
        // Simple keyword-based evaluation (placeholder)
        String answerText = answer.getAnswerText().toLowerCase();
        String expected = question.getExpectedPoints() != null ? question.getExpectedPoints().toLowerCase() : "";

        int accuracy = 70;
        int completeness = 60;
        int clarity = 75;
        int relevance = 70;

        // Basic scoring based on answer length and keyword matching
        if (answerText.length() > 100) completeness += 10;
        if (answerText.length() > 300) completeness += 10;

        String[] keywords = expected.split("[,;]");
        int matched = 0;
        for (String keyword : keywords) {
            if (answerText.contains(keyword.trim().toLowerCase())) {
                matched++;
            }
        }
        if (keywords.length > 0) {
            accuracy = 50 + (matched * 50 / keywords.length);
        }

        String feedback = generateAnswerFeedback(accuracy, completeness, clarity);
        String strengths = accuracy > 70 ? "Good understanding of key concepts" : "Showed effort in addressing the question";
        String improvements = accuracy < 80 ? "Consider covering more key points" : "Continue practicing for refinement";

        answer.evaluate(accuracy, completeness, clarity, relevance, feedback, strengths, improvements);
        answerRepository.save(answer);
    }

    private String generateAnswerFeedback(int accuracy, int completeness, int clarity) {
        StringBuilder sb = new StringBuilder();
        if (accuracy >= 80) sb.append("Strong technical accuracy. ");
        else if (accuracy >= 60) sb.append("Good grasp of concepts, with room for improvement. ");
        else sb.append("Review the key concepts for this topic. ");

        if (completeness >= 80) sb.append("Comprehensive answer. ");
        else sb.append("Consider elaborating more. ");

        return sb.toString();
    }

    private String generateFeedbackSummary(MockInterview interview, List<MockAnswer> answers) {
        int avgScore = interview.getOverallScore() != null ? interview.getOverallScore() : 0;
        
        if (avgScore >= 85) {
            return "Excellent performance! You demonstrated strong knowledge and communication skills.";
        } else if (avgScore >= 70) {
            return "Good performance with solid fundamentals. Focus on deepening technical knowledge.";
        } else if (avgScore >= 55) {
            return "Satisfactory performance. Regular practice will help improve your responses.";
        } else {
            return "There's room for improvement. Focus on core concepts and practice more.";
        }
    }

    private String identifyStrengths(List<MockAnswer> answers) {
        return answers.stream()
                .filter(a -> a.getScore() != null && a.getScore() >= 75)
                .map(a -> a.getQuestion().getCategory().name())
                .distinct()
                .limit(3)
                .collect(Collectors.joining(", "));
    }

    private String identifyImprovements(List<MockAnswer> answers) {
        return answers.stream()
                .filter(a -> a.getScore() != null && a.getScore() < 70)
                .map(a -> a.getQuestion().getCategory().name())
                .distinct()
                .limit(3)
                .collect(Collectors.joining(", "));
    }

    // Question templates (placeholder - will be replaced by AI)
    private List<QuestionTemplate> getQuestionTemplates(InterviewType type, DifficultyLevel difficulty, int count) {
        List<QuestionTemplate> all = new ArrayList<>();

        if (type == InterviewType.BEHAVIORAL || type == InterviewType.MIXED) {
            all.addAll(BEHAVIORAL_QUESTIONS);
        }
        if (type == InterviewType.TECHNICAL || type == InterviewType.MIXED) {
            all.addAll(TECHNICAL_QUESTIONS);
        }
        if (type == InterviewType.SYSTEM_DESIGN || type == InterviewType.MIXED) {
            all.addAll(SYSTEM_DESIGN_QUESTIONS);
        }
        if (type == InterviewType.CODING) {
            all.addAll(CODING_QUESTIONS);
        }

        if (all.isEmpty()) {
            all.addAll(BEHAVIORAL_QUESTIONS);
        }

        Collections.shuffle(all);
        return all.stream().limit(count).collect(Collectors.toList());
    }

    // ================== Question Templates ==================

    private record QuestionTemplate(QuestionCategory category, String question, 
                                     String expectedPoints, String sampleAnswer, String skill) {}

    private static final List<QuestionTemplate> BEHAVIORAL_QUESTIONS = List.of(
            new QuestionTemplate(QuestionCategory.LEADERSHIP,
                    "Tell me about a time when you had to lead a project under tight deadlines.",
                    "Leadership, Time management, Prioritization, Team coordination",
                    "Describe the situation, your specific actions, and the outcome with metrics.",
                    "Leadership"),
            new QuestionTemplate(QuestionCategory.TEAMWORK,
                    "Describe a situation where you had to work with a difficult team member.",
                    "Conflict resolution, Communication, Empathy, Problem-solving",
                    "Focus on understanding their perspective, finding common ground, and achieving results.",
                    "Collaboration"),
            new QuestionTemplate(QuestionCategory.PROBLEM_SOLVING,
                    "Tell me about a time you solved a complex problem with limited resources.",
                    "Analytical thinking, Creativity, Resourcefulness, Results",
                    "Explain your approach, constraints, and how you achieved success.",
                    "Problem Solving"),
            new QuestionTemplate(QuestionCategory.ADAPTABILITY,
                    "Describe a situation where you had to quickly adapt to unexpected changes.",
                    "Flexibility, Quick thinking, Resilience, Positive attitude",
                    "Show how you stayed calm, reassessed priorities, and delivered results.",
                    "Adaptability")
    );

    private static final List<QuestionTemplate> TECHNICAL_QUESTIONS = List.of(
            new QuestionTemplate(QuestionCategory.DATA_STRUCTURES,
                    "Explain the difference between ArrayList and LinkedList. When would you use each?",
                    "Time complexity, Memory allocation, Use cases, Trade-offs",
                    "ArrayList: O(1) random access, LinkedList: O(1) insertion/deletion",
                    "Data Structures"),
            new QuestionTemplate(QuestionCategory.DATABASE,
                    "What is database indexing and how does it improve query performance?",
                    "B-tree structure, Query optimization, Trade-offs, Index types",
                    "Explain how indexes create sorted data structures for faster lookups.",
                    "Database"),
            new QuestionTemplate(QuestionCategory.PROGRAMMING,
                    "Explain the SOLID principles and provide an example of one.",
                    "Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion",
                    "Provide concrete examples showing how each principle improves code quality.",
                    "OOP"),
            new QuestionTemplate(QuestionCategory.NETWORKING,
                    "Explain how HTTPS works and why it's important for security.",
                    "TLS/SSL, Certificate authority, Encryption, Authentication",
                    "Describe the handshake process and how encryption protects data.",
                    "Security")
    );

    private static final List<QuestionTemplate> SYSTEM_DESIGN_QUESTIONS = List.of(
            new QuestionTemplate(QuestionCategory.SYSTEM_DESIGN,
                    "Design a URL shortening service like bit.ly.",
                    "Hash function, Database design, Scalability, Caching, Analytics",
                    "Discuss API design, storage, encoding scheme, and scaling strategies.",
                    "System Design"),
            new QuestionTemplate(QuestionCategory.SYSTEM_DESIGN,
                    "How would you design a rate limiter for an API?",
                    "Token bucket, Sliding window, Distributed systems, Redis",
                    "Explain different algorithms and trade-offs for distributed rate limiting.",
                    "System Design")
    );

    private static final List<QuestionTemplate> CODING_QUESTIONS = List.of(
            new QuestionTemplate(QuestionCategory.ALGORITHMS,
                    "Write a function to find the longest substring without repeating characters.",
                    "Sliding window, Hash set, Time complexity O(n)",
                    "Use two pointers and a set to track characters in current window.",
                    "Algorithms"),
            new QuestionTemplate(QuestionCategory.ALGORITHMS,
                    "Implement a function to check if a binary tree is balanced.",
                    "Recursion, Height calculation, DFS",
                    "Check if height difference of subtrees is at most 1 at every node.",
                    "Data Structures")
    );
}
