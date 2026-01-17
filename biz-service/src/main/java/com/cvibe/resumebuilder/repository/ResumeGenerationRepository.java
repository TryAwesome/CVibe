package com.cvibe.resumebuilder.repository;

import com.cvibe.resumebuilder.entity.ResumeGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 简历生成记录数据访问层
 */
@Repository
public interface ResumeGenerationRepository extends JpaRepository<ResumeGeneration, UUID> {

    /**
     * 获取用户的所有生成记录，按创建时间倒序
     */
    List<ResumeGeneration> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * 获取用户的单个生成记录
     */
    Optional<ResumeGeneration> findByIdAndUserId(UUID id, UUID userId);

    /**
     * 统计用户的生成记录数量
     */
    long countByUserId(UUID userId);
}
