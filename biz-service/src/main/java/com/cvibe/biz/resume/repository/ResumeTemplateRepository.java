package com.cvibe.biz.resume.repository;

import com.cvibe.biz.resume.entity.ResumeTemplate;
import com.cvibe.biz.resume.entity.ResumeTemplate.TemplateCategory;
import com.cvibe.biz.resume.entity.ResumeTemplate.TemplateType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ResumeTemplate entity
 */
@Repository
public interface ResumeTemplateRepository extends JpaRepository<ResumeTemplate, UUID> {

    // ==================== Basic Queries ====================

    /**
     * Find all active system templates
     */
    List<ResumeTemplate> findByTemplateTypeAndIsActiveTrueOrderByUsageCountDesc(TemplateType type);

    /**
     * Find templates by category
     */
    List<ResumeTemplate> findByCategoryAndIsActiveTrueOrderByUsageCountDesc(TemplateCategory category);

    /**
     * Find featured templates
     */
    List<ResumeTemplate> findByIsFeaturedTrueAndIsActiveTrueOrderByUsageCountDesc();

    /**
     * Find user's custom templates
     */
    List<ResumeTemplate> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId);

    /**
     * Find template by ID and ensure it's active
     */
    Optional<ResumeTemplate> findByIdAndIsActiveTrue(UUID id);

    // ==================== Search Queries ====================

    /**
     * Search templates by name (for both system and user templates)
     */
    @Query("SELECT t FROM ResumeTemplate t WHERE t.isActive = true " +
           "AND (t.templateType = 'SYSTEM' OR t.user.id = :userId) " +
           "AND LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<ResumeTemplate> searchByName(@Param("userId") UUID userId, @Param("keyword") String keyword);

    /**
     * Get all available templates for a user (system + their own)
     */
    @Query("SELECT t FROM ResumeTemplate t WHERE t.isActive = true " +
           "AND (t.templateType = 'SYSTEM' OR t.user.id = :userId) " +
           "ORDER BY t.isFeatured DESC, t.usageCount DESC")
    Page<ResumeTemplate> findAvailableTemplates(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Get templates by type and category
     */
    @Query("SELECT t FROM ResumeTemplate t WHERE t.isActive = true " +
           "AND (:type IS NULL OR t.templateType = :type) " +
           "AND (:category IS NULL OR t.category = :category) " +
           "ORDER BY t.usageCount DESC")
    Page<ResumeTemplate> findByFilters(
            @Param("type") TemplateType type,
            @Param("category") TemplateCategory category,
            Pageable pageable);

    // ==================== Admin Queries ====================

    /**
     * Find all system templates (including inactive, for admin)
     */
    List<ResumeTemplate> findByTemplateTypeOrderByCreatedAtDesc(TemplateType type);

    /**
     * Count templates by type
     */
    long countByTemplateType(TemplateType type);

    /**
     * Get most used templates
     */
    List<ResumeTemplate> findTop10ByIsActiveTrueOrderByUsageCountDesc();

    // ==================== Validation Queries ====================

    /**
     * Check if user already has a template with this name
     */
    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    /**
     * Check if system template with this name exists
     */
    boolean existsByTemplateTypeAndNameIgnoreCase(TemplateType type, String name);
}
