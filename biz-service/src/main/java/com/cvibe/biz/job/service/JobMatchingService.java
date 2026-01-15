package com.cvibe.biz.job.service;

import com.cvibe.biz.job.dto.*;
import com.cvibe.biz.job.entity.Job;
import com.cvibe.biz.job.entity.JobMatch;
import com.cvibe.biz.job.repository.JobMatchRepository;
import com.cvibe.biz.job.repository.JobRepository;
import com.cvibe.biz.profile.entity.ProfileSkill;
import com.cvibe.biz.profile.entity.UserProfile;
import com.cvibe.biz.profile.repository.ProfileSkillRepository;
import com.cvibe.biz.profile.repository.UserProfileRepository;
import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.response.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JobMatchingService
 * 
 * Handles job search, matching, and recommendation logic.
 * In production, semantic matching would use vector embeddings via pgvector.
 * For development, we use keyword-based matching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobMatchingService {

    private final JobRepository jobRepository;
    private final JobMatchRepository matchRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final ProfileSkillRepository skillRepository;
    private final ObjectMapper objectMapper;

    // ================== Job Search ==================

    /**
     * Search jobs with filters
     */
    @Transactional(readOnly = true)
    public Page<JobDto> searchJobs(JobSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        
        Page<Job> jobs = jobRepository.searchJobs(
                request.getCompany(),
                request.getLocation(),
                request.getKeyword(),  // Using keyword as title search
                pageable
        );

        return jobs.map(JobDto::from);
    }

    /**
     * Get job by ID
     */
    @Transactional(readOnly = true)
    public JobDto getJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
        return JobDto.from(job);
    }

    /**
     * Get latest jobs
     */
    @Transactional(readOnly = true)
    public Page<JobDto> getLatestJobs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jobRepository.findByIsActiveTrueOrderByFirstSeenAtDesc(pageable)
                .map(JobDto::from);
    }

    /**
     * Get remote jobs
     */
    @Transactional(readOnly = true)
    public Page<JobDto> getRemoteJobs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jobRepository.findRemoteJobs(pageable)
                .map(JobDto::from);
    }

    // ================== Job Matching ==================

    /**
     * Generate matches for a user based on their profile
     * This is the core matching algorithm
     */
    @Transactional
    public List<JobMatchDto> generateMatchesForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        List<ProfileSkill> userSkills = skillRepository.findByProfileId(profile.getId());

        // Get active jobs
        Pageable pageable = PageRequest.of(0, 100);  // Process up to 100 jobs at a time
        Page<Job> jobs = jobRepository.findByIsActiveTrueOrderByFirstSeenAtDesc(pageable);

        List<JobMatch> newMatches = new ArrayList<>();

        for (Job job : jobs) {
            // Skip if already matched
            if (matchRepository.existsByUserIdAndJobId(userId, job.getId())) {
                continue;
            }

            // Calculate match score
            MatchResult result = calculateMatchScore(profile, userSkills, job);

            // Only create match if score is above threshold (30%)
            if (result.score >= 30.0) {
                JobMatch match = JobMatch.builder()
                        .user(user)
                        .job(job)
                        .matchScore(result.score)
                        .matchReason(result.reason)
                        .matchedSkills(toJsonArray(result.matchedSkills))
                        .missingSkills(toJsonArray(result.missingSkills))
                        .build();

                newMatches.add(matchRepository.save(match));
            }
        }

        log.info("Generated {} new matches for user {}", newMatches.size(), userId);

        return newMatches.stream()
                .map(JobMatchDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Get user's job matches
     */
    @Transactional(readOnly = true)
    public Page<JobMatchDto> getUserMatches(UUID userId, int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size);
        
        Page<JobMatch> matches = "date".equals(sortBy)
                ? matchRepository.findByUserIdOrderByMatchedAtDesc(userId, pageable)
                : matchRepository.findByUserIdOrderByMatchScoreDesc(userId, pageable);

        return matches.map(JobMatchDto::from);
    }

    /**
     * Get match summary for dashboard
     */
    @Transactional(readOnly = true)
    public JobMatchSummary getMatchSummary(UUID userId) {
        long totalMatches = matchRepository.countByUserId(userId);
        long highScoreMatches = matchRepository.countHighScoreMatches(userId, 80.0);
        List<JobMatch> unviewed = matchRepository.findUnviewedByUserId(userId);
        List<JobMatch> saved = matchRepository.findSavedByUserId(userId);
        List<JobMatch> applied = matchRepository.findAppliedByUserId(userId);
        Double averageScore = matchRepository.getAverageMatchScore(userId);
        List<JobMatch> topMatches = matchRepository.findTopMatches(userId, 70.0);

        // Get recent matches
        Page<JobMatch> recentPage = matchRepository.findByUserIdOrderByMatchedAtDesc(
                userId, PageRequest.of(0, 5));

        return JobMatchSummary.builder()
                .totalMatches(totalMatches)
                .highScoreMatches(highScoreMatches)
                .unviewedMatches(unviewed.size())
                .savedJobs(saved.size())
                .appliedJobs(applied.size())
                .averageMatchScore(averageScore != null ? averageScore : 0.0)
                .topMatches(topMatches.stream()
                        .limit(5)
                        .map(JobMatchDto::from)
                        .collect(Collectors.toList()))
                .recentMatches(recentPage.getContent().stream()
                        .map(JobMatchDto::from)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Mark match as viewed
     */
    @Transactional
    public JobMatchDto markMatchViewed(UUID userId, UUID matchId) {
        JobMatch match = getMatchForUser(userId, matchId);
        match.markViewed();
        match = matchRepository.save(match);
        return JobMatchDto.from(match);
    }

    /**
     * Toggle saved status
     */
    @Transactional
    public JobMatchDto toggleSaved(UUID userId, UUID matchId) {
        JobMatch match = getMatchForUser(userId, matchId);
        match.toggleSaved();
        match = matchRepository.save(match);
        return JobMatchDto.from(match);
    }

    /**
     * Mark as applied
     */
    @Transactional
    public JobMatchDto markApplied(UUID userId, UUID matchId) {
        JobMatch match = getMatchForUser(userId, matchId);
        match.markApplied();
        match = matchRepository.save(match);
        return JobMatchDto.from(match);
    }

    /**
     * Submit feedback for a match
     */
    @Transactional
    public JobMatchDto submitFeedback(UUID userId, UUID matchId, MatchFeedbackRequest request) {
        JobMatch match = getMatchForUser(userId, matchId);
        match.setUserRating(request.getRating());
        match.setUserFeedback(request.getFeedback());
        match = matchRepository.save(match);
        return JobMatchDto.from(match);
    }

    /**
     * Get saved jobs
     */
    @Transactional(readOnly = true)
    public List<JobMatchDto> getSavedJobs(UUID userId) {
        return matchRepository.findSavedByUserId(userId).stream()
                .map(JobMatchDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Get applied jobs
     */
    @Transactional(readOnly = true)
    public List<JobMatchDto> getAppliedJobs(UUID userId) {
        return matchRepository.findAppliedByUserId(userId).stream()
                .map(JobMatchDto::from)
                .collect(Collectors.toList());
    }

    // ================== Admin Methods ==================

    /**
     * Get job statistics (admin)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getJobStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActiveJobs", jobRepository.countActiveJobs());
        stats.put("newJobsLast24h", jobRepository.countNewJobsSince(
                Instant.now().minusSeconds(86400)));
        stats.put("jobsByCompany", jobRepository.countJobsByCompany().stream()
                .limit(10)
                .collect(Collectors.toList()));
        stats.put("jobsByLocation", jobRepository.countJobsByLocation().stream()
                .limit(10)
                .collect(Collectors.toList()));

        return stats;
    }

    // ================== Private Helper Methods ==================

    private JobMatch getMatchForUser(UUID userId, UUID matchId) {
        JobMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Match not found"));

        if (!match.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return match;
    }

    /**
     * Calculate match score between user profile and job
     * This is a simplified keyword-based matching for development
     * Production would use vector similarity with embeddings
     */
    private MatchResult calculateMatchScore(UserProfile profile, List<ProfileSkill> skills, Job job) {
        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        double score = 0.0;

        // Extract user skills
        Set<String> userSkillNames = skills.stream()
                .map(s -> s.getName().toLowerCase())
                .collect(Collectors.toSet());

        // Extract job requirements (from description or requirements JSON)
        Set<String> jobRequirements = extractJobRequirements(job);

        // Calculate skill match
        int totalRequired = jobRequirements.size();
        int matched = 0;

        for (String requirement : jobRequirements) {
            boolean found = userSkillNames.stream()
                    .anyMatch(skill -> skill.contains(requirement) || requirement.contains(skill));
            
            if (found) {
                matchedSkills.add(requirement);
                matched++;
            } else {
                missingSkills.add(requirement);
            }
        }

        // Skill match contributes 60% of score
        if (totalRequired > 0) {
            score += (double) matched / totalRequired * 60;
        } else {
            score += 30;  // No specific requirements, give base score
        }

        // Experience level match contributes 20%
        if (job.getExperienceLevel() != null) {
            int yearsExp = profile.getYearsOfExperience() != null ? profile.getYearsOfExperience() : 0;
            score += calculateExperienceMatch(yearsExp, job.getExperienceLevel()) * 20;
        } else {
            score += 10;  // No level specified
        }

        // Title/role match contributes 20%
        if (profile.getCurrentTitle() != null && job.getTitle() != null) {
            if (titlesSimilar(profile.getCurrentTitle(), job.getTitle())) {
                score += 20;
            } else {
                score += 5;  // Some base score
            }
        }

        // Build match reason
        String reason = buildMatchReason(matchedSkills, missingSkills, score);

        return new MatchResult(Math.min(score, 100.0), reason, matchedSkills, missingSkills);
    }

    private Set<String> extractJobRequirements(Job job) {
        Set<String> requirements = new HashSet<>();

        // Try to parse requirements JSON
        if (job.getRequirementsJson() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> reqMap = objectMapper.readValue(
                        job.getRequirementsJson(), Map.class);
                if (reqMap.containsKey("tech")) {
                    Object tech = reqMap.get("tech");
                    if (tech instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> techList = (List<String>) tech;
                        techList.forEach(t -> requirements.add(t.toLowerCase()));
                    }
                }
            } catch (JsonProcessingException e) {
                log.debug("Could not parse requirements JSON for job {}", job.getId());
            }
        }

        // Extract from description using common tech keywords
        if (job.getDescriptionMarkdown() != null) {
            String desc = job.getDescriptionMarkdown().toLowerCase();
            COMMON_TECH_KEYWORDS.forEach(keyword -> {
                if (desc.contains(keyword.toLowerCase())) {
                    requirements.add(keyword.toLowerCase());
                }
            });
        }

        return requirements;
    }

    private double calculateExperienceMatch(int userYears, Job.ExperienceLevel required) {
        return switch (required) {
            case ENTRY -> userYears >= 0 ? 1.0 : 0.5;
            case JUNIOR -> userYears >= 1 ? 1.0 : (userYears >= 0 ? 0.7 : 0.3);
            case MID -> userYears >= 3 ? 1.0 : (userYears >= 2 ? 0.7 : 0.4);
            case SENIOR -> userYears >= 5 ? 1.0 : (userYears >= 3 ? 0.6 : 0.3);
            case LEAD -> userYears >= 7 ? 1.0 : (userYears >= 5 ? 0.5 : 0.2);
            case PRINCIPAL, EXECUTIVE -> userYears >= 10 ? 1.0 : (userYears >= 7 ? 0.4 : 0.1);
        };
    }

    private boolean titlesSimilar(String userTitle, String jobTitle) {
        String user = userTitle.toLowerCase();
        String job = jobTitle.toLowerCase();
        
        // Check for common role keywords
        String[] roleKeywords = {"engineer", "developer", "manager", "architect", 
                "analyst", "designer", "scientist", "lead"};
        
        for (String keyword : roleKeywords) {
            if (user.contains(keyword) && job.contains(keyword)) {
                return true;
            }
        }
        
        return user.contains(job) || job.contains(user);
    }

    private String buildMatchReason(List<String> matched, List<String> missing, double score) {
        StringBuilder reason = new StringBuilder();

        if (score >= 80) {
            reason.append("Excellent match! ");
        } else if (score >= 60) {
            reason.append("Strong match. ");
        } else if (score >= 40) {
            reason.append("Good potential. ");
        } else {
            reason.append("Possible fit. ");
        }

        if (!matched.isEmpty()) {
            reason.append("Your skills in ")
                    .append(String.join(", ", matched.subList(0, Math.min(3, matched.size()))))
                    .append(" align well with this role. ");
        }

        if (!missing.isEmpty() && missing.size() <= 3) {
            reason.append("Consider developing: ")
                    .append(String.join(", ", missing))
                    .append(".");
        }

        return reason.toString();
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    // Common tech keywords for extraction
    private static final List<String> COMMON_TECH_KEYWORDS = List.of(
            "Java", "Python", "JavaScript", "TypeScript", "Go", "Rust", "C++", "C#",
            "React", "Angular", "Vue", "Node.js", "Spring", "Django", "Flask",
            "AWS", "GCP", "Azure", "Docker", "Kubernetes", "Terraform",
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Elasticsearch",
            "Kafka", "RabbitMQ", "GraphQL", "REST", "gRPC",
            "Machine Learning", "Deep Learning", "NLP", "Computer Vision",
            "Agile", "Scrum", "CI/CD", "DevOps", "Microservices"
    );

    // Inner class for match result
    private record MatchResult(double score, String reason, 
                               List<String> matchedSkills, List<String> missingSkills) {}
}
