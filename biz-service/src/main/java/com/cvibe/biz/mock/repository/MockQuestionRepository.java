package com.cvibe.biz.mock.repository;

import com.cvibe.biz.mock.entity.MockQuestion;
import com.cvibe.biz.mock.entity.MockQuestion.QuestionCategory;
import com.cvibe.biz.mock.entity.MockQuestion.QuestionDifficulty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MockQuestion entity
 */
@Repository
public interface MockQuestionRepository extends JpaRepository<MockQuestion, UUID> {

    /**
     * Find all questions for an interview
     */
    List<MockQuestion> findByInterviewIdOrderByQuestionNumberAsc(UUID interviewId);

    /**
     * Find questions for a round
     */
    List<MockQuestion> findByRoundIdOrderByQuestionNumberAsc(UUID roundId);

    /**
     * Find question by interview and number
     */
    Optional<MockQuestion> findByInterviewIdAndQuestionNumber(UUID interviewId, Integer questionNumber);

    /**
     * Find unanswered questions
     */
    List<MockQuestion> findByInterviewIdAndIsAnsweredFalseAndIsSkippedFalseOrderByQuestionNumberAsc(UUID interviewId);

    /**
     * Find next unanswered question
     */
    @Query("SELECT q FROM MockQuestion q WHERE q.interview.id = :interviewId " +
           "AND q.isAnswered = false AND q.isSkipped = false ORDER BY q.questionNumber ASC")
    List<MockQuestion> findNextQuestions(@Param("interviewId") UUID interviewId, Pageable pageable);

    /**
     * Find questions by category
     */
    List<MockQuestion> findByInterviewIdAndCategory(UUID interviewId, QuestionCategory category);

    /**
     * Find questions by difficulty
     */
    List<MockQuestion> findByInterviewIdAndDifficulty(UUID interviewId, QuestionDifficulty difficulty);

    /**
     * Count answered questions
     */
    long countByInterviewIdAndIsAnsweredTrue(UUID interviewId);

    /**
     * Count skipped questions
     */
    long countByInterviewIdAndIsSkippedTrue(UUID interviewId);

    /**
     * Count questions by category for interview
     */
    long countByInterviewIdAndCategory(UUID interviewId, QuestionCategory category);

    // ================== Analytics ==================

    /**
     * Get category distribution for interview
     */
    @Query("SELECT q.category, COUNT(q) FROM MockQuestion q " +
           "WHERE q.interview.id = :interviewId GROUP BY q.category")
    List<Object[]> getCategoryDistribution(@Param("interviewId") UUID interviewId);

    /**
     * Get difficulty distribution
     */
    @Query("SELECT q.difficulty, COUNT(q) FROM MockQuestion q " +
           "WHERE q.interview.id = :interviewId GROUP BY q.difficulty")
    List<Object[]> getDifficultyDistribution(@Param("interviewId") UUID interviewId);

    /**
     * Get commonly missed categories for a user
     */
    @Query("SELECT q.category, AVG(a.score) as avgScore FROM MockQuestion q " +
           "JOIN q.answer a WHERE q.interview.user.id = :userId " +
           "GROUP BY q.category ORDER BY avgScore ASC")
    List<Object[]> getWeakCategories(@Param("userId") UUID userId, Pageable pageable);
}
