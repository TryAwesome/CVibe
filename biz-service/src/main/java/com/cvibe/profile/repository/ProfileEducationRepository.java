package com.cvibe.profile.repository;

import com.cvibe.profile.entity.ProfileEducation;
import com.cvibe.profile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ProfileEducation entity.
 */
@Repository
public interface ProfileEducationRepository extends JpaRepository<ProfileEducation, UUID> {
    
    List<ProfileEducation> findByProfileOrderByStartDateDesc(UserProfile profile);
    
    void deleteByProfileId(UUID profileId);
}
