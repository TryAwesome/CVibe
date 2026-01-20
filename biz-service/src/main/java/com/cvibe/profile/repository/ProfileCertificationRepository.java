package com.cvibe.profile.repository;

import com.cvibe.profile.entity.ProfileCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileCertificationRepository extends JpaRepository<ProfileCertification, UUID> {
    List<ProfileCertification> findByProfileIdOrderByIssueDateDesc(UUID profileId);
    void deleteByProfileId(UUID profileId);
}
