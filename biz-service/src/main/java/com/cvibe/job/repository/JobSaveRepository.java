package com.cvibe.job.repository;

import com.cvibe.job.entity.JobSave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for JobSave entities
 */
@Repository
public interface JobSaveRepository extends JpaRepository<JobSave, UUID> {

    /**
     * Check if user has saved a job
     */
    boolean existsByUserIdAndJobId(UUID userId, UUID jobId);

    /**
     * Find saves by user ID
     */
    Page<JobSave> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find saves by user ID (list)
     */
    List<JobSave> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find save by user ID and job ID
     */
    Optional<JobSave> findByUserIdAndJobId(UUID userId, UUID jobId);

    /**
     * Delete save by user ID and job ID
     */
    void deleteByUserIdAndJobId(UUID userId, UUID jobId);

    /**
     * Count saves by user ID
     */
    long countByUserId(UUID userId);
}
