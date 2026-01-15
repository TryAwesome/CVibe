package com.cvibe.biz.profile.repository;

import com.cvibe.biz.profile.entity.ProfileProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ProfileProject entity
 */
@Repository
public interface ProfileProjectRepository extends JpaRepository<ProfileProject, UUID> {

    /**
     * Find all projects for a profile, ordered by start date descending
     */
    List<ProfileProject> findByProfileIdOrderByStartDateDesc(UUID profileId);

    /**
     * Find projects by profile
     */
    List<ProfileProject> findByProfileId(UUID profileId);

    /**
     * Delete all projects for a profile
     */
    void deleteByProfileId(UUID profileId);

    /**
     * Count projects for a profile
     */
    long countByProfileId(UUID profileId);
}
