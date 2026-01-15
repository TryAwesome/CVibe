package com.cvibe.biz.profile.repository;

import com.cvibe.biz.profile.entity.ProfileEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileEducationRepository extends JpaRepository<ProfileEducation, UUID> {

    List<ProfileEducation> findByProfileIdOrderByStartDateDesc(UUID profileId);

    long countByProfileId(UUID profileId);
}
