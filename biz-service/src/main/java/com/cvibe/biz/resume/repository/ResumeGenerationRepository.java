package com.cvibe.biz.resume.repository;

import com.cvibe.biz.resume.entity.ResumeGeneration;
import com.cvibe.biz.resume.entity.ResumeGeneration.GenerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ResumeGeneration entity
 */
@Repository
public interface ResumeGenerationRepository extends JpaRepository<ResumeGeneration, UUID> {

    // ==================== User Queries ====================

    /**
     * Find all generations by user, ordered by creation date
     */
    Page<ResumeGeneration> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find user's completed generations
     */
    List<ResumeGeneration> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, GenerationStatus status);

    /**
     * Find user's exported resumes
     */
    List<ResumeGeneration> findByUserIdAndIsExportedTrueOrderByExportedAtDesc(UUID userId);

    /**
     * Find generations by target company
     */
    List<ResumeGeneration> findByUserIdAndTargetCompanyContainingIgnoreCaseOrderByCreatedAtDesc(
            UUID userId, String company);

    /**
     * Get user's recent generations (last N)
     */
    List<ResumeGeneration> findTop5ByUserIdOrderByCreatedAtDesc(UUID userId);

    // ==================== Status Queries ====================

    /**
     * Find pending generations (for processing queue)
     */
    List<ResumeGeneration> findByStatusOrderByCreatedAtAsc(GenerationStatus status);

    /**
     * Find stuck generations (in processing state for too long)
     */
    @Query("SELECT g FROM ResumeGeneration g WHERE g.status IN :statuses " +
           "AND g.createdAt < :threshold ORDER BY g.createdAt ASC")
    List<ResumeGeneration> findStuckGenerations(
            @Param("statuses") List<GenerationStatus> statuses,
            @Param("threshold") Instant threshold);

    // ==================== Statistics Queries ====================

    /**
     * Count generations by user
     */
    long countByUserId(UUID userId);

    /**
     * Count exported resumes by user
     */
    long countByUserIdAndIsExportedTrue(UUID userId);

    /**
     * Count generations by status
     */
    long countByStatus(GenerationStatus status);

    /**
     * Get average rating for completed generations
     */
    @Query("SELECT AVG(g.userRating) FROM ResumeGeneration g " +
           "WHERE g.userRating IS NOT NULL")
    Double getAverageRating();

    /**
     * Get template usage count
     */
    long countByTemplateId(UUID templateId);

    /**
     * Count generations in time range
     */
    @Query("SELECT COUNT(g) FROM ResumeGeneration g WHERE g.createdAt >= :since")
    long countGenerationsSince(@Param("since") Instant since);

    // ==================== Analytics Queries ====================

    /**
     * Get most targeted companies
     */
    @Query("SELECT g.targetCompany, COUNT(g) as cnt FROM ResumeGeneration g " +
           "WHERE g.targetCompany IS NOT NULL " +
           "GROUP BY g.targetCompany ORDER BY cnt DESC")
    List<Object[]> getMostTargetedCompanies();

    /**
     * Get generation trends by date
     */
    @Query("SELECT CAST(g.createdAt AS DATE), COUNT(g) FROM ResumeGeneration g " +
           "WHERE g.createdAt >= :since GROUP BY CAST(g.createdAt AS DATE) " +
           "ORDER BY CAST(g.createdAt AS DATE)")
    List<Object[]> getGenerationTrends(@Param("since") Instant since);

    // ==================== Cleanup Queries ====================

    /**
     * Find old failed generations for cleanup
     */
    @Query("SELECT g FROM ResumeGeneration g WHERE g.status = 'FAILED' " +
           "AND g.createdAt < :threshold")
    List<ResumeGeneration> findOldFailedGenerations(@Param("threshold") Instant threshold);
}
