package com.cvibe.profile.repository;

import com.cvibe.profile.entity.ProfileExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ProfileExperience entity.
 */
@Repository
public interface ProfileExperienceRepository extends JpaRepository<ProfileExperience, UUID> {

    /**
     * Find all experiences for a profile, ordered by start date descending.
     */
    List<ProfileExperience> findByProfileIdOrderByStartDateDesc(UUID profileId);
}
