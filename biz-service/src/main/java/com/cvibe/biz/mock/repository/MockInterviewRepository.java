package com.cvibe.biz.mock.repository;

import com.cvibe.biz.mock.entity.MockInterview;
import com.cvibe.biz.mock.entity.MockInterview.InterviewStatus;
import com.cvibe.biz.mock.entity.MockInterview.InterviewType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MockInterview entity
 */
@Repository
public interface MockInterviewRepository extends JpaRepository<MockInterview, UUID> {

    // ================== User Queries ==================

    /**
     * Find all interviews for a user
     */
    List<MockInterview> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find interviews with pagination
     */
    Page<MockInterview> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find interviews by status
     */
    List<MockInterview> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, InterviewStatus status);

    /**
     * Find interviews by type
     */
    List<MockInterview> findByUserIdAndInterviewTypeOrderByCreatedAtDesc(UUID userId, InterviewType type);

    /**
     * Find in-progress interviews
     */
    @Query("SELECT i FROM MockInterview i WHERE i.user.id = :userId " +
           "AND i.status IN ('CREATED', 'IN_PROGRESS', 'PAUSED') ORDER BY i.updatedAt DESC")
    List<MockInterview> findActiveInterviews(@Param("userId") UUID userId);

    /**
     * Find most recent in-progress interview
     */
    Optional<MockInterview> findFirstByUserIdAndStatusOrderByUpdatedAtDesc(UUID userId, InterviewStatus status);

    /**
     * Find completed interviews (for history)
     */
    @Query("SELECT i FROM MockInterview i WHERE i.user.id = :userId " +
           "AND i.status IN ('COMPLETED', 'EVALUATED') ORDER BY i.completedAt DESC")
    Page<MockInterview> findCompletedInterviews(@Param("userId") UUID userId, Pageable pageable);

    // ================== Statistics ==================

    /**
     * Count interviews by status
     */
    long countByUserIdAndStatus(UUID userId, InterviewStatus status);

    /**
     * Count interviews by type
     */
    long countByUserIdAndInterviewType(UUID userId, InterviewType type);

    /**
     * Get average score
     */
    @Query("SELECT AVG(i.overallScore) FROM MockInterview i " +
           "WHERE i.user.id = :userId AND i.overallScore IS NOT NULL")
    Double getAverageScore(@Param("userId") UUID userId);

    /**
     * Get average score by type
     */
    @Query("SELECT AVG(i.overallScore) FROM MockInterview i " +
           "WHERE i.user.id = :userId AND i.interviewType = :type AND i.overallScore IS NOT NULL")
    Double getAverageScoreByType(@Param("userId") UUID userId, @Param("type") InterviewType type);

    /**
     * Get total interview time
     */
    @Query("SELECT SUM(i.durationSeconds) FROM MockInterview i " +
           "WHERE i.user.id = :userId AND i.durationSeconds IS NOT NULL")
    Long getTotalDurationSeconds(@Param("userId") UUID userId);

    /**
     * Get score trend (last N interviews)
     */
    @Query("SELECT i.overallScore FROM MockInterview i " +
           "WHERE i.user.id = :userId AND i.overallScore IS NOT NULL " +
           "ORDER BY i.completedAt DESC")
    List<Integer> getScoreTrend(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Count interviews in date range
     */
    @Query("SELECT COUNT(i) FROM MockInterview i " +
           "WHERE i.user.id = :userId AND i.createdAt BETWEEN :start AND :end")
    long countInDateRange(@Param("userId") UUID userId, 
                          @Param("start") Instant start, 
                          @Param("end") Instant end);

    // ================== Admin Queries ==================

    /**
     * Get popular target positions
     */
    @Query("SELECT i.targetPosition, COUNT(i) as cnt FROM MockInterview i " +
           "GROUP BY i.targetPosition ORDER BY cnt DESC")
    List<Object[]> getPopularTargetPositions(Pageable pageable);

    /**
     * Get interview type distribution
     */
    @Query("SELECT i.interviewType, COUNT(i) FROM MockInterview i GROUP BY i.interviewType")
    List<Object[]> getInterviewTypeDistribution();

    /**
     * Get average scores by difficulty
     */
    @Query("SELECT i.difficulty, AVG(i.overallScore) FROM MockInterview i " +
           "WHERE i.overallScore IS NOT NULL GROUP BY i.difficulty")
    List<Object[]> getAverageScoresByDifficulty();
}
