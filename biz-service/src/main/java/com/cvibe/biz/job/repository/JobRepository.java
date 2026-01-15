package com.cvibe.biz.job.repository;

import com.cvibe.biz.job.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByUrlHash(String urlHash);

    boolean existsByUrlHash(String urlHash);

    Page<Job> findByIsActiveTrueOrderByFirstSeenAtDesc(Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.isActive = true " +
           "AND (:company IS NULL OR LOWER(j.company) LIKE LOWER(CONCAT('%', :company, '%'))) " +
           "AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "AND (:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "ORDER BY j.firstSeenAt DESC")
    Page<Job> searchJobs(
            @Param("company") String company,
            @Param("location") String location,
            @Param("title") String title,
            Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.isActive = true " +
           "AND j.source = :source " +
           "ORDER BY j.firstSeenAt DESC")
    List<Job> findBySource(@Param("source") Job.JobSource source);

    @Query("SELECT j FROM Job j WHERE j.isActive = true " +
           "AND j.experienceLevel = :level " +
           "ORDER BY j.firstSeenAt DESC")
    Page<Job> findByExperienceLevel(@Param("level") Job.ExperienceLevel level, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.isActive = true " +
           "AND j.isRemote = true " +
           "ORDER BY j.firstSeenAt DESC")
    Page<Job> findRemoteJobs(Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.isActive = true " +
           "AND j.firstSeenAt >= :since " +
           "ORDER BY j.firstSeenAt DESC")
    List<Job> findNewJobsSince(@Param("since") Instant since);

    @Query("SELECT j FROM Job j WHERE j.lastCrawledAt < :before AND j.isActive = true")
    List<Job> findStaleJobs(@Param("before") Instant before);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.isActive = true")
    long countActiveJobs();

    @Query("SELECT COUNT(j) FROM Job j WHERE j.firstSeenAt >= :since")
    long countNewJobsSince(@Param("since") Instant since);

    @Query("SELECT j.company, COUNT(j) FROM Job j WHERE j.isActive = true " +
           "GROUP BY j.company ORDER BY COUNT(j) DESC")
    List<Object[]> countJobsByCompany();

    @Query("SELECT j.location, COUNT(j) FROM Job j WHERE j.isActive = true " +
           "GROUP BY j.location ORDER BY COUNT(j) DESC")
    List<Object[]> countJobsByLocation();
}
