package com.cvibe.biz.interview.service;

import com.cvibe.biz.interview.entity.InterviewAnswer;
import com.cvibe.biz.interview.entity.InterviewSession;
import com.cvibe.biz.interview.entity.QuestionTemplate;
import com.cvibe.biz.interview.repository.InterviewAnswerRepository;
import com.cvibe.biz.interview.repository.InterviewSessionRepository;
import com.cvibe.biz.profile.entity.*;
import com.cvibe.biz.profile.repository.*;
import com.cvibe.biz.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * ProfileBuilderService
 * 
 * This service extracts structured profile data from interview answers.
 * In production, this would integrate with AI service to parse natural language answers.
 * For now, it provides the infrastructure for profile building from interviews.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileBuilderService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final UserProfileRepository profileRepository;
    private final ProfileExperienceRepository experienceRepository;
    private final ProfileEducationRepository educationRepository;
    private final ProfileSkillRepository skillRepository;
    private final ObjectMapper objectMapper;

    /**
     * Process a completed interview session and extract profile data
     * This method would be called after AI processes the answers
     */
    @Transactional
    public void processCompletedSession(UUID sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() != InterviewSession.SessionStatus.COMPLETED) {
            log.warn("Session {} is not completed, skipping extraction", sessionId);
            return;
        }

        session.setExtractionStatus(InterviewSession.ExtractionStatus.PROCESSING);
        sessionRepository.save(session);

        try {
            User user = session.getUser();
            
            // Get or create profile
            UserProfile profile = profileRepository.findByUserId(user.getId())
                    .orElseGet(() -> createNewProfile(user));

            // Get all answered questions
            List<InterviewAnswer> answers = answerRepository.findWithExtractedEntities(sessionId);

            // Process answers by category
            for (InterviewAnswer answer : answers) {
                if (answer.getExtractedEntities() != null && answer.getQuestion() != null) {
                    processAnswer(profile, answer);
                }
            }

            // Update profile completeness
            updateProfileCompleteness(profile);
            profileRepository.save(profile);

            // Aggregate extracted data for session
            Map<String, Object> aggregatedData = aggregateExtractedData(answers);
            try {
                session.setExtractedData(objectMapper.writeValueAsString(aggregatedData));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize extracted data", e);
            }

            session.setExtractionStatus(InterviewSession.ExtractionStatus.COMPLETED);
            sessionRepository.save(session);

            log.info("Successfully processed session {} for user {}", sessionId, user.getId());

        } catch (Exception e) {
            log.error("Failed to process session {}", sessionId, e);
            session.setExtractionStatus(InterviewSession.ExtractionStatus.FAILED);
            sessionRepository.save(session);
        }
    }

    /**
     * Update extracted entities for an answer (called by AI service)
     */
    @Transactional
    public void updateAnswerExtraction(UUID answerId, String extractedEntitiesJson, 
                                        String aiAnalysis, Double confidenceScore) {
        InterviewAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));

        answer.setExtractedEntities(extractedEntitiesJson);
        answer.setAiAnalysis(aiAnalysis);
        answer.setConfidenceScore(confidenceScore);
        answerRepository.save(answer);
    }

    /**
     * Mark answer as needing clarification
     */
    @Transactional
    public void markNeedsClarification(UUID answerId, String reason) {
        InterviewAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));

        answer.setNeedsClarification(true);
        // Note: clarificationReason would need to be added to entity if needed
        answerRepository.save(answer);
    }

    /**
     * Get extraction summary for a session
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getExtractionSummary(UUID sessionId) {
        List<InterviewAnswer> answers = answerRepository.findWithExtractedEntities(sessionId);
        return aggregateExtractedData(answers);
    }

    // ================== Private Helper Methods ==================

    private UserProfile createNewProfile(User user) {
        UserProfile profile = UserProfile.builder()
                .user(user)
                .completenessScore(0)
                .build();
        return profileRepository.save(profile);
    }

    private void processAnswer(UserProfile profile, InterviewAnswer answer) {
        QuestionTemplate.QuestionCategory category = answer.getQuestion().getCategory();

        try {
            Map<String, Object> entities = objectMapper.readValue(
                    answer.getExtractedEntities(), 
                    new TypeReference<Map<String, Object>>() {});

            switch (category) {
                case PERSONAL_INFO:
                    processPersonalInfo(profile, entities);
                    break;
                case WORK_EXPERIENCE:
                    processWorkExperience(profile, entities);
                    break;
                case EDUCATION:
                    processEducation(profile, entities);
                    break;
                case SKILLS:
                    processSkills(profile, entities);
                    break;
                case CAREER_GOALS:
                    processCareerGoals(profile, entities);
                    break;
                default:
                    log.debug("Unhandled category: {}", category);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse extracted entities for answer {}", answer.getId(), e);
        }
    }

    private void processPersonalInfo(UserProfile profile, Map<String, Object> entities) {
        if (entities.containsKey("job_title")) {
            profile.setCurrentTitle((String) entities.get("job_title"));
        }
        if (entities.containsKey("city")) {
            profile.setLocation((String) entities.get("city"));
        }
        if (entities.containsKey("country")) {
            String location = profile.getLocation();
            if (location != null) {
                profile.setLocation(location + ", " + entities.get("country"));
            } else {
                profile.setLocation((String) entities.get("country"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processWorkExperience(UserProfile profile, Map<String, Object> entities) {
        ProfileExperience experience = new ProfileExperience();
        experience.setProfile(profile);

        if (entities.containsKey("company")) {
            experience.setCompany((String) entities.get("company"));
        }
        if (entities.containsKey("role")) {
            experience.setTitle((String) entities.get("role"));
        }
        if (entities.containsKey("responsibilities")) {
            Object resp = entities.get("responsibilities");
            if (resp instanceof List) {
                experience.setDescription(String.join("\n- ", (List<String>) resp));
            } else {
                experience.setDescription((String) resp);
            }
        }
        if (entities.containsKey("achievement")) {
            String existing = experience.getDescription();
            String achievement = "Achievement: " + entities.get("achievement");
            if (entities.containsKey("metrics")) {
                achievement += " (" + entities.get("metrics") + ")";
            }
            experience.setDescription(existing != null ? existing + "\n" + achievement : achievement);
        }
        if (entities.containsKey("technologies")) {
            Object tech = entities.get("technologies");
            if (tech instanceof List) {
                experience.setTechnologies(String.join(", ", (List<String>) tech));
            } else {
                experience.setTechnologies((String) tech);
            }
        }

        // Only save if we have meaningful data
        if (experience.getCompany() != null || experience.getTitle() != null) {
            experienceRepository.save(experience);
        }
    }

    private void processEducation(UserProfile profile, Map<String, Object> entities) {
        ProfileEducation education = new ProfileEducation();
        education.setProfile(profile);

        if (entities.containsKey("institution")) {
            education.setInstitution((String) entities.get("institution"));
        }
        if (entities.containsKey("degree")) {
            education.setDegree((String) entities.get("degree"));
        }
        if (entities.containsKey("major")) {
            education.setFieldOfStudy((String) entities.get("major"));
        }
        if (entities.containsKey("graduation_year")) {
            try {
                int year = Integer.parseInt(entities.get("graduation_year").toString());
                education.setEndDate(LocalDate.of(year, 6, 1));
            } catch (NumberFormatException e) {
                log.debug("Could not parse graduation year");
            }
        }

        // Only save if we have meaningful data
        if (education.getInstitution() != null || education.getDegree() != null) {
            educationRepository.save(education);
        }
    }

    @SuppressWarnings("unchecked")
    private void processSkills(UserProfile profile, Map<String, Object> entities) {
        if (entities.containsKey("skills")) {
            Object skillsObj = entities.get("skills");
            List<String> skills;
            
            if (skillsObj instanceof List) {
                skills = (List<String>) skillsObj;
            } else {
                skills = Arrays.asList(skillsObj.toString().split(","));
            }

            Map<String, String> proficiencyMap = new HashMap<>();
            if (entities.containsKey("proficiency_levels") && entities.get("proficiency_levels") instanceof Map) {
                proficiencyMap = (Map<String, String>) entities.get("proficiency_levels");
            }

            for (String skill : skills) {
                String trimmed = skill.trim();
                if (!trimmed.isEmpty()) {
                    ProfileSkill profileSkill = new ProfileSkill();
                    profileSkill.setProfile(profile);
                    profileSkill.setName(trimmed);
                    profileSkill.setCategory(ProfileSkill.SkillCategory.PROGRAMMING_LANGUAGE);
                    
                    String proficiency = proficiencyMap.getOrDefault(trimmed, "INTERMEDIATE");
                    try {
                        profileSkill.setProficiencyLevel(
                                ProfileSkill.ProficiencyLevel.valueOf(proficiency.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        profileSkill.setProficiencyLevel(ProfileSkill.ProficiencyLevel.INTERMEDIATE);
                    }
                    
                    skillRepository.save(profileSkill);
                }
            }
        }

        if (entities.containsKey("soft_skills")) {
            Object softSkillsObj = entities.get("soft_skills");
            List<String> softSkills;
            
            if (softSkillsObj instanceof List) {
                softSkills = (List<String>) softSkillsObj;
            } else {
                softSkills = Arrays.asList(softSkillsObj.toString().split(","));
            }

            for (String skill : softSkills) {
                String trimmed = skill.trim();
                if (!trimmed.isEmpty()) {
                    ProfileSkill profileSkill = new ProfileSkill();
                    profileSkill.setProfile(profile);
                    profileSkill.setName(trimmed);
                    profileSkill.setCategory(ProfileSkill.SkillCategory.SOFT_SKILL);
                    profileSkill.setProficiencyLevel(ProfileSkill.ProficiencyLevel.ADVANCED);
                    skillRepository.save(profileSkill);
                }
            }
        }
    }

    private void processCareerGoals(UserProfile profile, Map<String, Object> entities) {
        StringBuilder bio = new StringBuilder();
        
        if (entities.containsKey("target_role")) {
            bio.append("Target Role: ").append(entities.get("target_role")).append("\n");
        }
        if (entities.containsKey("job_type")) {
            bio.append("Job Type: ").append(entities.get("job_type")).append("\n");
        }
        if (entities.containsKey("industry")) {
            bio.append("Industry: ").append(entities.get("industry")).append("\n");
        }
        if (entities.containsKey("career_goals")) {
            bio.append("Goals: ").append(entities.get("career_goals")).append("\n");
        }
        if (entities.containsKey("work_mode")) {
            bio.append("Work Mode: ").append(entities.get("work_mode")).append("\n");
        }
        if (entities.containsKey("preferred_locations")) {
            bio.append("Preferred Locations: ").append(entities.get("preferred_locations")).append("\n");
        }

        if (bio.length() > 0) {
            String existingSummary = profile.getSummary();
            if (existingSummary != null && !existingSummary.isEmpty()) {
                profile.setSummary(existingSummary + "\n\n--- Career Goals ---\n" + bio);
            } else {
                profile.setSummary("--- Career Goals ---\n" + bio.toString());
            }
        }
    }

    private void updateProfileCompleteness(UserProfile profile) {
        int score = 0;
        
        // Basic info (30%)
        if (profile.getCurrentTitle() != null) score += 10;
        if (profile.getLocation() != null) score += 10;
        if (profile.getSummary() != null && !profile.getSummary().isEmpty()) score += 10;

        // Experience (25%)
        long expCount = experienceRepository.countByProfileId(profile.getId());
        if (expCount > 0) score += 15;
        if (expCount > 2) score += 10;

        // Education (15%)
        long eduCount = educationRepository.countByProfileId(profile.getId());
        if (eduCount > 0) score += 15;

        // Skills (20%)
        long skillCount = skillRepository.countByProfileId(profile.getId());
        if (skillCount > 0) score += 10;
        if (skillCount >= 5) score += 10;

        // LinkedIn adds 10%
        if (profile.getLinkedinUrl() != null) score += 10;

        profile.setCompletenessScore(Math.min(score, 100));
    }

    private Map<String, Object> aggregateExtractedData(List<InterviewAnswer> answers) {
        Map<String, Object> aggregated = new HashMap<>();
        
        List<Map<String, Object>> experiences = new ArrayList<>();
        List<Map<String, Object>> education = new ArrayList<>();
        List<String> skills = new ArrayList<>();
        Map<String, Object> personalInfo = new HashMap<>();
        Map<String, Object> careerGoals = new HashMap<>();

        for (InterviewAnswer answer : answers) {
            if (answer.getExtractedEntities() == null || answer.getQuestion() == null) {
                continue;
            }

            try {
                Map<String, Object> entities = objectMapper.readValue(
                        answer.getExtractedEntities(), 
                        new TypeReference<Map<String, Object>>() {});

                switch (answer.getQuestion().getCategory()) {
                    case PERSONAL_INFO:
                        personalInfo.putAll(entities);
                        break;
                    case WORK_EXPERIENCE:
                        experiences.add(entities);
                        break;
                    case EDUCATION:
                        education.add(entities);
                        break;
                    case SKILLS:
                        if (entities.containsKey("skills")) {
                            Object s = entities.get("skills");
                            if (s instanceof List) {
                                skills.addAll((List<String>) s);
                            }
                        }
                        break;
                    case CAREER_GOALS:
                        careerGoals.putAll(entities);
                        break;
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to parse entities for aggregation", e);
            }
        }

        aggregated.put("personalInfo", personalInfo);
        aggregated.put("experiences", experiences);
        aggregated.put("education", education);
        aggregated.put("skills", skills);
        aggregated.put("careerGoals", careerGoals);

        return aggregated;
    }
}
