package com.cvibe.biz.profile.repository;

import com.cvibe.biz.profile.entity.ProfileCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ProfileCertification entity
 */
@Repository
public interface ProfileCertificationRepository extends JpaRepository<ProfileCertification, UUID> {

    /**
     * Find all certifications for a profile
     */
    List<ProfileCertification> findByProfileId(UUID profileId);

    /**
     * Find certifications ordered by issue date
     */
    List<ProfileCertification> findByProfileIdOrderByIssueDateDesc(UUID profileId);

    /**
     * Delete all certifications for a profile
     */
    void deleteByProfileId(UUID profileId);

    /**
     * Count certifications for a profile
     */
    long countByProfileId(UUID profileId);
}
