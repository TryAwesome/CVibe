package com.cvibe.biz.job.repository;

import com.cvibe.biz.job.entity.JobMatch;
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

@Repository
public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {

    @Query("SELECT jm FROM JobMatch jm JOIN FETCH jm.job WHERE jm.user.id = :userId ORDER BY jm.matchScore DESC")
    Page<JobMatch> findByUserIdOrderByMatchScoreDesc(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT jm FROM JobMatch jm JOIN FETCH jm.job WHERE jm.user.id = :userId ORDER BY jm.matchedAt DESC")
    Page<JobMatch> findByUserIdOrderByMatchedAtDesc(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT jm FROM JobMatch jm JOIN FETCH jm.job WHERE jm.user.id = :userId AND jm.isSaved = true ORDER BY jm.matchScore DESC")
    List<JobMatch> findSavedByUserId(@Param("userId") UUID userId);

    @Query("SELECT jm FROM JobMatch jm JOIN FETCH jm.job WHERE jm.user.id = :userId AND jm.isApplied = true ORDER BY jm.appliedAt DESC")
    List<JobMatch> findAppliedByUserId(@Param("userId") UUID userId);

    @Query("SELECT jm FROM JobMatch jm JOIN FETCH jm.job WHERE jm.user.id = :userId AND jm.isViewed = false ORDER BY jm.matchScore DESC")
    List<JobMatch> findUnviewedByUserId(@Param("userId") UUID userId);

    Optional<JobMatch> findByUserIdAndJobId(UUID userId, UUID jobId);

    boolean existsByUserIdAndJobId(UUID userId, UUID jobId);

    @Query("SELECT COUNT(jm) FROM JobMatch jm WHERE jm.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(jm) FROM JobMatch jm WHERE jm.user.id = :userId AND jm.matchScore >= :minScore")
    long countHighScoreMatches(@Param("userId") UUID userId, @Param("minScore") Double minScore);

    @Query("SELECT AVG(jm.matchScore) FROM JobMatch jm WHERE jm.user.id = :userId")
    Double getAverageMatchScore(@Param("userId") UUID userId);

    @Query("SELECT jm FROM JobMatch jm JOIN FETCH jm.job WHERE jm.user.id = :userId " +
           "AND jm.matchScore >= :minScore ORDER BY jm.matchScore DESC")
    List<JobMatch> findTopMatches(@Param("userId") UUID userId, @Param("minScore") Double minScore);

    @Query("SELECT jm FROM JobMatch jm WHERE jm.matchedAt >= :since")
    List<JobMatch> findMatchesSince(@Param("since") Instant since);

    @Modifying
    @Query("DELETE FROM JobMatch jm WHERE jm.job.id = :jobId")
    void deleteByJobId(@Param("jobId") UUID jobId);

    @Modifying
    @Query("UPDATE JobMatch jm SET jm.isViewed = true, jm.viewedAt = :now WHERE jm.id = :matchId")
    void markAsViewed(@Param("matchId") UUID matchId, @Param("now") Instant now);
}
