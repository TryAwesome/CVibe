package com.cvibe.biz.interview.repository;

import com.cvibe.biz.interview.entity.InterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, UUID> {

    List<InterviewAnswer> findBySessionIdOrderByQuestionOrder(UUID sessionId);

    List<InterviewAnswer> findBySessionIdAndIsFollowUpFalseOrderByQuestionOrder(UUID sessionId);

    @Query("SELECT a FROM InterviewAnswer a WHERE a.session.id = :sessionId AND a.answerText IS NOT NULL ORDER BY a.questionOrder")
    List<InterviewAnswer> findAnsweredBySessionId(@Param("sessionId") UUID sessionId);

    @Query("SELECT a FROM InterviewAnswer a WHERE a.session.id = :sessionId AND a.answerText IS NULL ORDER BY a.questionOrder LIMIT 1")
    Optional<InterviewAnswer> findNextUnanswered(@Param("sessionId") UUID sessionId);

    @Query("SELECT COUNT(a) FROM InterviewAnswer a WHERE a.session.id = :sessionId AND a.answerText IS NOT NULL")
    long countAnsweredBySessionId(@Param("sessionId") UUID sessionId);

    List<InterviewAnswer> findByParentAnswerId(UUID parentAnswerId);

    @Query("SELECT a FROM InterviewAnswer a WHERE a.session.id = :sessionId AND a.needsClarification = true AND a.answerText IS NULL")
    List<InterviewAnswer> findPendingClarifications(@Param("sessionId") UUID sessionId);

    @Query("SELECT a FROM InterviewAnswer a WHERE a.session.id = :sessionId AND a.extractedEntities IS NOT NULL")
    List<InterviewAnswer> findWithExtractedEntities(@Param("sessionId") UUID sessionId);
}
