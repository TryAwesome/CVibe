package com.cvibe.biz.mock.repository;

import com.cvibe.biz.mock.entity.MockAnswer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MockAnswer entity
 */
@Repository
public interface MockAnswerRepository extends JpaRepository<MockAnswer, UUID> {

    /**
     * Find answer by question ID
     */
    Optional<MockAnswer> findByQuestionId(UUID questionId);

    /**
     * Find all answers for an interview
     */
    @Query("SELECT a FROM MockAnswer a WHERE a.question.interview.id = :interviewId " +
           "ORDER BY a.question.questionNumber ASC")
    List<MockAnswer> findByInterviewId(@Param("interviewId") UUID interviewId);

    /**
     * Find unevaluated answers
     */
    @Query("SELECT a FROM MockAnswer a WHERE a.question.interview.id = :interviewId " +
           "AND a.isEvaluated = false")
    List<MockAnswer> findUnevaluatedByInterviewId(@Param("interviewId") UUID interviewId);

    /**
     * Find answers for a round
     */
    @Query("SELECT a FROM MockAnswer a WHERE a.question.round.id = :roundId " +
           "ORDER BY a.question.questionNumber ASC")
    List<MockAnswer> findByRoundId(@Param("roundId") UUID roundId);

    /**
     * Count evaluated answers
     */
    @Query("SELECT COUNT(a) FROM MockAnswer a WHERE a.question.interview.id = :interviewId " +
           "AND a.isEvaluated = true")
    long countEvaluatedByInterviewId(@Param("interviewId") UUID interviewId);

    // ================== Statistics ==================

    /**
     * Get average score for an interview
     */
    @Query("SELECT AVG(a.score) FROM MockAnswer a " +
           "WHERE a.question.interview.id = :interviewId AND a.score IS NOT NULL")
    Double getAverageScoreForInterview(@Param("interviewId") UUID interviewId);

    /**
     * Get average score for a round
     */
    @Query("SELECT AVG(a.score) FROM MockAnswer a " +
           "WHERE a.question.round.id = :roundId AND a.score IS NOT NULL")
    Double getAverageScoreForRound(@Param("roundId") UUID roundId);

    /**
     * Get average scores breakdown
     */
    @Query("SELECT AVG(a.accuracyScore), AVG(a.completenessScore), " +
           "AVG(a.clarityScore), AVG(a.relevanceScore) FROM MockAnswer a " +
           "WHERE a.question.interview.id = :interviewId AND a.isEvaluated = true")
    List<Object[]> getScoreBreakdown(@Param("interviewId") UUID interviewId);

    /**
     * Get total time spent on interview
     */
    @Query("SELECT SUM(a.timeTakenSeconds) FROM MockAnswer a " +
           "WHERE a.question.interview.id = :interviewId AND a.timeTakenSeconds IS NOT NULL")
    Long getTotalTimeSpent(@Param("interviewId") UUID interviewId);

    /**
     * Get best answers (highest scores)
     */
    @Query("SELECT a FROM MockAnswer a WHERE a.question.interview.user.id = :userId " +
           "AND a.score IS NOT NULL ORDER BY a.score DESC")
    List<MockAnswer> getBestAnswers(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Get answers needing improvement
     */
    @Query("SELECT a FROM MockAnswer a WHERE a.question.interview.user.id = :userId " +
           "AND a.score IS NOT NULL AND a.score < 60 ORDER BY a.score ASC")
    List<MockAnswer> getLowScoreAnswers(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Get average score by question category for a user
     */
    @Query("SELECT q.category, AVG(a.score) FROM MockAnswer a " +
           "JOIN a.question q WHERE q.interview.user.id = :userId " +
           "AND a.score IS NOT NULL GROUP BY q.category")
    List<Object[]> getAverageScoreByCategory(@Param("userId") UUID userId);
}
