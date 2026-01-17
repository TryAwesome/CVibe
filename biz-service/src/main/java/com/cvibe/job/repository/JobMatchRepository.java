package com.cvibe.job.repository;

import com.cvibe.job.entity.JobMatch;
import com.cvibe.job.entity.MatchStatus;
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
 * Repository for JobMatch entities
 */
@Repository
public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {

    /**
     * Find matches by user ID ordered by match score descending
     */
    Page<JobMatch> findByUserIdOrderByMatchScoreDesc(UUID userId, Pageable pageable);

    /**
     * Find matches by user ID ordered by match score descending (list)
     */
    List<JobMatch> findByUserIdOrderByMatchScoreDesc(UUID userId);

    /**
     * Find match by user ID and job ID
     */
    Optional<JobMatch> findByUserIdAndJobId(UUID userId, UUID jobId);

    /**
     * Check if match exists for user and job
     */
    boolean existsByUserIdAndJobId(UUID userId, UUID jobId);

    /**
     * Find matches by user ID and status
     */
    Page<JobMatch> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, MatchStatus status, Pageable pageable);

    /**
     * Find matches by user ID and status (list)
     */
    List<JobMatch> findByUserIdAndStatus(UUID userId, MatchStatus status);

    /**
     * Count matches by user ID
     */
    long countByUserId(UUID userId);

    /**
     * Count matches by user ID and status
     */
    long countByUserIdAndStatus(UUID userId, MatchStatus status);

    /**
     * Calculate average match score for user
     */
    @Query("SELECT AVG(m.matchScore) FROM JobMatch m WHERE m.user.id = :userId")
    Double calculateAverageMatchScore(@Param("userId") UUID userId);

    /**
     * Find top matches for user
     */
    @Query("SELECT m FROM JobMatch m WHERE m.user.id = :userId ORDER BY m.matchScore DESC")
    List<JobMatch> findTopMatchesByUserId(@Param("userId") UUID userId, Pageable pageable);
}
