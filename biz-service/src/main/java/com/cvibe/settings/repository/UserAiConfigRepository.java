package com.cvibe.settings.repository;

import com.cvibe.settings.entity.UserAiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户 AI 配置数据访问层
 */
@Repository
public interface UserAiConfigRepository extends JpaRepository<UserAiConfig, UUID> {

    /**
     * 根据用户 ID 查找 AI 配置
     */
    Optional<UserAiConfig> findByUserId(UUID userId);

    /**
     * 检查用户是否已有 AI 配置
     */
    boolean existsByUserId(UUID userId);

    /**
     * 根据用户 ID 删除 AI 配置
     */
    void deleteByUserId(UUID userId);
}
