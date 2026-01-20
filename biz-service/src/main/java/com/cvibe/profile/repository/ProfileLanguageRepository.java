package com.cvibe.profile.repository;

import com.cvibe.profile.entity.ProfileLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileLanguageRepository extends JpaRepository<ProfileLanguage, UUID> {
    List<ProfileLanguage> findByProfileIdOrderByLanguage(UUID profileId);
    Optional<ProfileLanguage> findByProfileIdAndLanguage(UUID profileId, String language);
    void deleteByProfileId(UUID profileId);
    boolean existsByProfileIdAndLanguage(UUID profileId, String language);
}
