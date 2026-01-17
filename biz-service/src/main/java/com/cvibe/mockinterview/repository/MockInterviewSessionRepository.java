package com.cvibe.mockinterview.repository;

import com.cvibe.mockinterview.entity.MockInterviewSession;
import com.cvibe.mockinterview.entity.MockInterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for mock interview sessions
 */
@Repository
public interface MockInterviewSessionRepository extends JpaRepository<MockInterviewSession, UUID> {

    /**
     * Find all sessions by user ID with pagination, ordered by creation date descending
     */
    Page<MockInterviewSession> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find all sessions by user ID
     */
    List<MockInterviewSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find session by ID and user ID (for ownership verification)
     */
    Optional<MockInterviewSession> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find sessions by user ID and status
     */
    List<MockInterviewSession> findByUserIdAndStatus(UUID userId, MockInterviewStatus status);

    /**
     * Count sessions by user ID
     */
    long countByUserId(UUID userId);

    /**
     * Count completed sessions for a user
     */
    @Query("SELECT COUNT(s) FROM MockInterviewSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED'")
    long countCompletedByUserId(@Param("userId") UUID userId);

    /**
     * Count sessions by user ID and status
     */
    long countByUserIdAndStatus(UUID userId, MockInterviewStatus status);

    /**
     * Get average score for completed sessions
     */
    @Query("SELECT AVG(s.overallScore) FROM MockInterviewSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED' AND s.overallScore IS NOT NULL")
    Double getAverageScoreByUserId(@Param("userId") UUID userId);

    /**
     * Find in-progress sessions for a user
     */
    @Query("SELECT s FROM MockInterviewSession s WHERE s.user.id = :userId AND s.status = 'IN_PROGRESS' ORDER BY s.createdAt DESC")
    List<MockInterviewSession> findActiveSessionsByUserId(@Param("userId") UUID userId);
}
