package com.cvibe.interview.repository;

import com.cvibe.interview.entity.InterviewSessionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for interview session answers
 */
@Repository
public interface InterviewSessionAnswerRepository extends JpaRepository<InterviewSessionAnswer, UUID> {

    /**
     * Find all answers for a session ordered by question order
     */
    List<InterviewSessionAnswer> findBySessionIdOrderByQuestionOrderAsc(UUID sessionId);

    /**
     * Find answer by session ID and question ID
     */
    Optional<InterviewSessionAnswer> findBySessionIdAndQuestionId(UUID sessionId, UUID questionId);

    /**
     * Count answers for a session
     */
    long countBySessionId(UUID sessionId);

    /**
     * Delete all answers for a session
     */
    void deleteBySessionId(UUID sessionId);
}
