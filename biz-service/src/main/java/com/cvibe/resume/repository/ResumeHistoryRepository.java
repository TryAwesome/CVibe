package com.cvibe.resume.repository;

import com.cvibe.resume.entity.ResumeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 简历历史数据访问层
 */
@Repository
public interface ResumeHistoryRepository extends JpaRepository<ResumeHistory, UUID> {

    /**
     * 获取用户所有简历，按创建时间倒序
     */
    List<ResumeHistory> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * 获取用户的主简历
     */
    Optional<ResumeHistory> findFirstByUserIdAndIsPrimaryTrue(UUID userId);

    /**
     * 获取用户简历列表，主简历优先，然后按创建时间倒序
     */
    @Query("SELECT r FROM ResumeHistory r WHERE r.user.id = :userId " +
           "ORDER BY r.isPrimary DESC, r.createdAt DESC")
    List<ResumeHistory> findAllByUserIdOrdered(@Param("userId") UUID userId);

    /**
     * 获取用户最新的简历（不论是否为主简历）
     */
    Optional<ResumeHistory> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * 获取用户最新的简历（排除指定 ID）
     * 用于删除主简历后重新指定主简历
     */
    Optional<ResumeHistory> findFirstByUserIdAndIdNotOrderByCreatedAtDesc(
            UUID userId, UUID excludeId);

    /**
     * 统计用户的简历数量
     */
    long countByUserId(UUID userId);

    /**
     * 清除用户所有简历的主简历标记
     */
    @Modifying
    @Query("UPDATE ResumeHistory r SET r.isPrimary = false WHERE r.user.id = :userId")
    void clearPrimaryByUserId(@Param("userId") UUID userId);

    /**
     * 检查简历是否属于用户
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);
}
