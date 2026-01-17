package com.cvibe.resumebuilder.repository;

import com.cvibe.resumebuilder.entity.ResumeTemplate;
import com.cvibe.resumebuilder.entity.TemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 简历模板数据访问层
 */
@Repository
public interface ResumeTemplateRepository extends JpaRepository<ResumeTemplate, UUID> {

    /**
     * 获取所有激活的模板，按高级状态和名称排序
     */
    List<ResumeTemplate> findAllByIsActiveTrueOrderByIsPremiumAscNameAsc();

    /**
     * 按分类获取激活的模板
     */
    List<ResumeTemplate> findByCategoryAndIsActiveTrueOrderByNameAsc(TemplateCategory category);

    /**
     * 获取推荐模板
     */
    List<ResumeTemplate> findByIsFeaturedTrueAndIsActiveTrueOrderByNameAsc();

    /**
     * 获取非高级模板
     */
    List<ResumeTemplate> findByIsPremiumFalseAndIsActiveTrueOrderByNameAsc();
}
