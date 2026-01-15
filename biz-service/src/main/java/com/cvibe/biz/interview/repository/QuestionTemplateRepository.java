package com.cvibe.biz.interview.repository;

import com.cvibe.biz.interview.entity.QuestionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, UUID> {

    List<QuestionTemplate> findByIsActiveTrueOrderByOrderWeight();

    List<QuestionTemplate> findByCategoryAndIsActiveTrueOrderByOrderWeight(QuestionTemplate.QuestionCategory category);

    List<QuestionTemplate> findByIsRequiredTrueAndIsActiveTrueOrderByOrderWeight();

    @Query("SELECT q FROM QuestionTemplate q WHERE q.isActive = true AND q.language = :language ORDER BY q.orderWeight")
    List<QuestionTemplate> findActiveByLanguage(@Param("language") String language);

    @Query("SELECT q FROM QuestionTemplate q WHERE q.isActive = true AND q.category = :category AND q.language = :language ORDER BY q.orderWeight")
    List<QuestionTemplate> findByCategoryAndLanguage(
            @Param("category") QuestionTemplate.QuestionCategory category,
            @Param("language") String language);

    @Query("SELECT q FROM QuestionTemplate q WHERE q.isActive = true AND q.difficultyLevel = :level ORDER BY q.orderWeight")
    List<QuestionTemplate> findByDifficultyLevel(@Param("level") QuestionTemplate.DifficultyLevel level);

    @Query("SELECT DISTINCT q.category FROM QuestionTemplate q WHERE q.isActive = true")
    List<QuestionTemplate.QuestionCategory> findActiveCategories();

    @Query("SELECT COUNT(q) FROM QuestionTemplate q WHERE q.isActive = true AND q.category = :category")
    long countActiveByCategory(@Param("category") QuestionTemplate.QuestionCategory category);
}
