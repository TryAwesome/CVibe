package com.cvibe.interview.repository;

import com.cvibe.interview.entity.InterviewSession;
import com.cvibe.interview.entity.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for interview sessions
 */
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    /**
     * Find all sessions by user ID with pagination
     */
    Page<InterviewSession> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find all sessions by user ID
     */
    List<InterviewSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find session by ID and user ID (for ownership verification)
     */
    Optional<InterviewSession> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find sessions by user ID and status
     */
    List<InterviewSession> findByUserIdAndStatus(UUID userId, SessionStatus status);

    /**
     * Count sessions by user ID and status
     */
    long countByUserIdAndStatus(UUID userId, SessionStatus status);

    /**
     * Count completed sessions for a user
     */
    @Query("SELECT COUNT(s) FROM InterviewSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED'")
    long countCompletedByUserId(@Param("userId") UUID userId);

    /**
     * Find in-progress sessions for a user
     */
    @Query("SELECT s FROM InterviewSession s WHERE s.user.id = :userId AND s.status = 'IN_PROGRESS' ORDER BY s.lastActivityAt DESC")
    List<InterviewSession> findActiveSessionsByUserId(@Param("userId") UUID userId);

    /**
     * Expire old sessions that have been inactive
     */
    @Modifying
    @Query("UPDATE InterviewSession s SET s.status = 'EXPIRED' " +
           "WHERE s.status = 'IN_PROGRESS' AND s.lastActivityAt < :cutoff")
    int expireInactiveSessions(@Param("cutoff") Instant cutoff);
}
