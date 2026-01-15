package com.cvibe.biz.profile.repository;

import com.cvibe.biz.profile.entity.ProfileSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileSkillRepository extends JpaRepository<ProfileSkill, UUID> {

    List<ProfileSkill> findByProfileId(UUID profileId);

    List<ProfileSkill> findByProfileIdAndIsPrimaryTrue(UUID profileId);

    List<ProfileSkill> findByProfileIdAndCategory(UUID profileId, ProfileSkill.SkillCategory category);

    long countByProfileId(UUID profileId);
}
