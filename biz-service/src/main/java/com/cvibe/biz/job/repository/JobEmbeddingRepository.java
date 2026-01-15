package com.cvibe.biz.job.repository;

import com.cvibe.biz.job.entity.JobEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobEmbeddingRepository extends JpaRepository<JobEmbedding, UUID> {

    @Query("SELECT je FROM JobEmbedding je WHERE je.jobId IN :jobIds")
    List<JobEmbedding> findByJobIdIn(@Param("jobIds") List<UUID> jobIds);

    @Query("SELECT je FROM JobEmbedding je WHERE je.embedding IS NOT NULL")
    List<JobEmbedding> findAllWithEmbeddings();

    /**
     * Note: In production with PostgreSQL + pgvector, this would use:
     * SELECT * FROM job_embeddings 
     * ORDER BY embedding <=> :queryEmbedding 
     * LIMIT :limit
     * 
     * For H2/development, we'll implement similarity search in the service layer
     */
    @Query("SELECT je FROM JobEmbedding je JOIN FETCH je.job WHERE je.embedding IS NOT NULL")
    List<JobEmbedding> findAllWithJobs();

    boolean existsByJobId(UUID jobId);
}
