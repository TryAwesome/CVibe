package com.cvibe.biz.user.repository;

import com.cvibe.biz.user.entity.UserAiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * User AI Config Repository
 */
@Repository
public interface UserAiConfigRepository extends JpaRepository<UserAiConfig, UUID> {

    /**
     * Find AI config by user ID
     */
    Optional<UserAiConfig> findByUserId(UUID userId);

    /**
     * Delete AI config by user ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Check if user has AI config
     */
    boolean existsByUserId(UUID userId);
}
