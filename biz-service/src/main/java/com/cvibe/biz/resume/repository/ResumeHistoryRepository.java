package com.cvibe.biz.resume.repository;

import com.cvibe.biz.resume.entity.ResumeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeHistoryRepository extends JpaRepository<ResumeHistory, UUID> {

    Page<ResumeHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<ResumeHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ResumeHistory> findByIdAndUserId(UUID id, UUID userId);

    Optional<ResumeHistory> findByUserIdAndIsPrimaryTrue(UUID userId);

    @Modifying
    @Query("UPDATE ResumeHistory r SET r.isPrimary = false WHERE r.user.id = :userId")
    void clearPrimaryForUser(UUID userId);

    long countByUserId(UUID userId);
}
