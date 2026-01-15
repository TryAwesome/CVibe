package com.cvibe.biz.job.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Job Embedding Entity - Vector embedding for semantic job matching
 * Uses pgvector for similarity search
 */
@Entity
@Table(name = "job_embeddings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEmbedding {

    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "job_id")
    private Job job;

    /**
     * Vector embedding of job description
     * Dimensions: 1536 (OpenAI text-embedding-3-small)
     * 
     * Note: Using String to store vector for H2 compatibility in local dev
     * In production with PostgreSQL + pgvector, use proper vector type
     */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;  // JSON array of floats: "[0.1, 0.2, ...]"

    @Column(name = "embedding_model", length = 100)
    @Builder.Default
    private String embeddingModel = "text-embedding-3-small";
}
