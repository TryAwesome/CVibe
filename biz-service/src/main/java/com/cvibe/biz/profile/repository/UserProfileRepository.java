package com.cvibe.biz.profile.repository;

import com.cvibe.biz.profile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    @Query("SELECT p FROM UserProfile p " +
           "LEFT JOIN FETCH p.experiences " +
           "LEFT JOIN FETCH p.educations " +
           "LEFT JOIN FETCH p.skills " +
           "WHERE p.user.id = :userId")
    Optional<UserProfile> findByUserIdWithDetails(UUID userId);

    @Query("SELECT p FROM UserProfile p " +
           "LEFT JOIN FETCH p.experiences " +
           "LEFT JOIN FETCH p.educations " +
           "LEFT JOIN FETCH p.skills " +
           "LEFT JOIN FETCH p.certifications " +
           "LEFT JOIN FETCH p.projects " +
           "WHERE p.user.id = :userId")
    Optional<UserProfile> findByUserIdWithAllDetails(UUID userId);
}
