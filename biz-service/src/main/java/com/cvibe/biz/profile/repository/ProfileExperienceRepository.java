package com.cvibe.biz.profile.repository;

import com.cvibe.biz.profile.entity.ProfileExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileExperienceRepository extends JpaRepository<ProfileExperience, UUID> {

    List<ProfileExperience> findByProfileIdOrderByStartDateDesc(UUID profileId);

    List<ProfileExperience> findByProfileIdAndIsCurrentTrue(UUID profileId);

    long countByProfileId(UUID profileId);
}
