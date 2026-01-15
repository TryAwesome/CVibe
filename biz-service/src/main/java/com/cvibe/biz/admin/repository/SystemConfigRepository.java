package com.cvibe.biz.admin.repository;

import com.cvibe.biz.admin.entity.SystemConfig;
import com.cvibe.biz.admin.entity.SystemConfig.ConfigCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID> {

    // ================== Basic Queries ==================

    Optional<SystemConfig> findByConfigKey(String configKey);

    boolean existsByConfigKey(String configKey);

    List<SystemConfig> findByCategory(ConfigCategory category);

    List<SystemConfig> findByCategoryAndIsActiveTrue(ConfigCategory category);

    List<SystemConfig> findByIsActiveTrue();

    List<SystemConfig> findByIsEditableTrue();

    // ================== Feature Flags ==================

    @Query("""
            SELECT c FROM SystemConfig c
            WHERE c.category = 'FEATURE_FLAG'
            AND c.isActive = true
            """)
    List<SystemConfig> findAllActiveFeatureFlags();

    @Query("""
            SELECT c FROM SystemConfig c
            WHERE c.category = 'FEATURE_FLAG'
            AND c.configKey = :key
            AND c.isActive = true
            """)
    Optional<SystemConfig> findFeatureFlag(@Param("key") String key);

    @Query("""
            SELECT CASE WHEN c.configValue = 'true' THEN true ELSE false END
            FROM SystemConfig c
            WHERE c.configKey = :key
            AND c.isActive = true
            """)
    Optional<Boolean> isFeatureEnabled(@Param("key") String key);

    // ================== Value Queries ==================

    @Query("""
            SELECT c.configValue FROM SystemConfig c
            WHERE c.configKey = :key
            AND c.isActive = true
            """)
    Optional<String> findValueByKey(@Param("key") String key);

    @Query("""
            SELECT COALESCE(c.configValue, c.defaultValue) FROM SystemConfig c
            WHERE c.configKey = :key
            """)
    Optional<String> findEffectiveValueByKey(@Param("key") String key);

    // ================== Batch Operations ==================

    @Query("""
            SELECT c FROM SystemConfig c
            WHERE c.configKey IN :keys
            """)
    List<SystemConfig> findByConfigKeys(@Param("keys") List<String> keys);

    @Modifying
    @Query("""
            UPDATE SystemConfig c
            SET c.isActive = :active
            WHERE c.category = :category
            """)
    int updateActiveStatusByCategory(
            @Param("category") ConfigCategory category,
            @Param("active") boolean active);

    // ================== Search ==================

    @Query("""
            SELECT c FROM SystemConfig c
            WHERE c.isActive = true
            AND (LOWER(c.configKey) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY c.category, c.configKey
            """)
    List<SystemConfig> searchByKeyword(@Param("keyword") String keyword);

    // ================== Statistics ==================

    @Query("""
            SELECT c.category, COUNT(c) FROM SystemConfig c
            GROUP BY c.category
            ORDER BY c.category
            """)
    List<Object[]> countByCategory();

    long countByIsActiveTrue();

    long countByCategoryAndIsActiveTrue(ConfigCategory category);
}
