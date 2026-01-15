package com.cvibe.biz.mock.repository;

import com.cvibe.biz.mock.entity.InterviewRound;
import com.cvibe.biz.mock.entity.InterviewRound.RoundStatus;
import com.cvibe.biz.mock.entity.InterviewRound.RoundType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for InterviewRound entity
 */
@Repository
public interface InterviewRoundRepository extends JpaRepository<InterviewRound, UUID> {

    /**
     * Find all rounds for an interview
     */
    List<InterviewRound> findByInterviewIdOrderByRoundNumberAsc(UUID interviewId);

    /**
     * Find round by interview and round number
     */
    Optional<InterviewRound> findByInterviewIdAndRoundNumber(UUID interviewId, Integer roundNumber);

    /**
     * Find rounds by status
     */
    List<InterviewRound> findByInterviewIdAndStatus(UUID interviewId, RoundStatus status);

    /**
     * Find rounds by type
     */
    List<InterviewRound> findByInterviewIdAndRoundType(UUID interviewId, RoundType type);

    /**
     * Find current/next round (first non-completed)
     */
    @Query("SELECT r FROM InterviewRound r WHERE r.interview.id = :interviewId " +
           "AND r.status IN ('PENDING', 'IN_PROGRESS') ORDER BY r.roundNumber ASC")
    List<InterviewRound> findCurrentRounds(@Param("interviewId") UUID interviewId);

    /**
     * Find first pending round
     */
    Optional<InterviewRound> findFirstByInterviewIdAndStatusOrderByRoundNumberAsc(
            UUID interviewId, RoundStatus status);

    /**
     * Count completed rounds
     */
    long countByInterviewIdAndStatus(UUID interviewId, RoundStatus status);

    /**
     * Get average score for an interview
     */
    @Query("SELECT AVG(r.score) FROM InterviewRound r " +
           "WHERE r.interview.id = :interviewId AND r.score IS NOT NULL")
    Double getAverageScore(@Param("interviewId") UUID interviewId);

    /**
     * Get average score by round type across all interviews for a user
     */
    @Query("SELECT AVG(r.score) FROM InterviewRound r " +
           "WHERE r.interview.user.id = :userId AND r.roundType = :type AND r.score IS NOT NULL")
    Double getAverageScoreByType(@Param("userId") UUID userId, @Param("type") RoundType type);
}
