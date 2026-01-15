package com.cvibe.biz.interview.repository;

import com.cvibe.biz.interview.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    List<InterviewSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<InterviewSession> findByUserIdAndStatus(UUID userId, InterviewSession.SessionStatus status);

    Optional<InterviewSession> findByUserIdAndStatusAndSessionType(
            UUID userId, 
            InterviewSession.SessionStatus status, 
            InterviewSession.SessionType sessionType);

    @Query("SELECT s FROM InterviewSession s WHERE s.user.id = :userId AND s.status = 'IN_PROGRESS' ORDER BY s.lastActivityAt DESC")
    List<InterviewSession> findActiveSessionsByUserId(@Param("userId") UUID userId);

    @Query("SELECT s FROM InterviewSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED' AND s.extractionStatus = 'COMPLETED' ORDER BY s.completedAt DESC")
    List<InterviewSession> findCompletedWithExtraction(@Param("userId") UUID userId);

    @Query("SELECT COUNT(s) FROM InterviewSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED'")
    long countCompletedByUserId(@Param("userId") UUID userId);

    @Query("SELECT s FROM InterviewSession s WHERE s.status = 'COMPLETED' AND s.extractionStatus = 'PENDING'")
    List<InterviewSession> findSessionsPendingExtraction();
}
