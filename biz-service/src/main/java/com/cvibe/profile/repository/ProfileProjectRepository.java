package com.cvibe.profile.repository;

import com.cvibe.profile.entity.ProfileProject;
import com.cvibe.profile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ProfileProject entity.
 */
@Repository
public interface ProfileProjectRepository extends JpaRepository<ProfileProject, UUID> {
    
    List<ProfileProject> findByProfileOrderByStartDateDesc(UserProfile profile);
    
    void deleteByProfileId(UUID profileId);
}
