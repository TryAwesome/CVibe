package com.cvibe.profile.repository;

import com.cvibe.profile.entity.ProfileSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ProfileSkill entity.
 */
@Repository
public interface ProfileSkillRepository extends JpaRepository<ProfileSkill, UUID> {

    /**
     * Find all skills for a profile.
     */
    List<ProfileSkill> findByProfileId(UUID profileId);

    /**
     * Check if skill with name exists for profile (case-insensitive).
     */
    boolean existsByProfileIdAndNameIgnoreCase(UUID profileId, String name);

    /**
     * Find skill by profile and name (case-insensitive).
     */
    Optional<ProfileSkill> findByProfileIdAndNameIgnoreCase(UUID profileId, String name);
}
