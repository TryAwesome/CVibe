package com.cvibe.job.repository;

import com.cvibe.job.entity.ExperienceLevel;
import com.cvibe.job.entity.Job;
import com.cvibe.job.entity.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Job entities
 */
@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    /**
     * Search jobs with multiple criteria
     */
    @Query("SELECT j FROM Job j WHERE " +
           "(:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(j.company) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "AND (:type IS NULL OR j.type = :type) " +
           "AND (:experienceLevel IS NULL OR j.experienceLevel = :experienceLevel) " +
           "AND (:salaryMin IS NULL OR j.salaryMax >= :salaryMin) " +
           "AND (:salaryMax IS NULL OR j.salaryMin <= :salaryMax) " +
           "ORDER BY j.postedAt DESC")
    Page<Job> searchJobs(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("type") JobType type,
            @Param("experienceLevel") ExperienceLevel experienceLevel,
            @Param("salaryMin") Integer salaryMin,
            @Param("salaryMax") Integer salaryMax,
            Pageable pageable);

    /**
     * Find remote jobs
     */
    Page<Job> findByIsRemoteTrueOrderByPostedAtDesc(Pageable pageable);

    /**
     * Find latest jobs
     */
    Page<Job> findAllByOrderByPostedAtDesc(Pageable pageable);

    /**
     * Find jobs by company
     */
    List<Job> findByCompanyIgnoreCaseOrderByPostedAtDesc(String company);

    /**
     * Find jobs by type
     */
    Page<Job> findByTypeOrderByPostedAtDesc(JobType type, Pageable pageable);

    /**
     * Find jobs by experience level
     */
    Page<Job> findByExperienceLevelOrderByPostedAtDesc(ExperienceLevel experienceLevel, Pageable pageable);

    /**
     * Find jobs by source ID (for deduplication)
     */
    boolean existsBySourceAndSourceId(String source, String sourceId);

    /**
     * Count jobs by type
     */
    long countByType(JobType type);

    /**
     * Count remote jobs
     */
    long countByIsRemoteTrue();
}
