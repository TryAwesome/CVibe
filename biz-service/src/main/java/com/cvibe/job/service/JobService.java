package com.cvibe.job.service;

import com.cvibe.auth.entity.User;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import com.cvibe.job.dto.*;
import com.cvibe.job.entity.*;
import com.cvibe.job.repository.JobMatchRepository;
import com.cvibe.job.repository.JobRepository;
import com.cvibe.job.repository.JobSaveRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for job management and matching
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobMatchRepository jobMatchRepository;
    private final JobSaveRepository jobSaveRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // Mock job data for demo purposes
    private static final List<MockJobData> MOCK_JOBS = createMockJobs();

    /**
     * Search jobs with criteria
     */
    @Transactional(readOnly = true)
    public PagedResponse<JobDto> searchJobs(JobSearchRequest request) {
        // Parse enums if provided
        JobType jobType = null;
        if (request.getType() != null && !request.getType().isEmpty()) {
            try {
                jobType = JobType.valueOf(request.getType());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid job type: {}", request.getType());
            }
        }

        ExperienceLevel expLevel = null;
        if (request.getExperienceLevel() != null && !request.getExperienceLevel().isEmpty()) {
            try {
                expLevel = ExperienceLevel.valueOf(request.getExperienceLevel());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid experience level: {}", request.getExperienceLevel());
            }
        }

        Pageable pageable = PageRequest.of(
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 20
        );

        Page<Job> jobPage = jobRepository.searchJobs(
                request.getKeyword(),
                request.getLocation(),
                jobType,
                expLevel,
                request.getSalaryMin(),
                request.getSalaryMax(),
                pageable
        );

        // If no jobs in DB, return mock data
        if (jobPage.isEmpty()) {
            return getMockJobsPage(request);
        }

        List<JobDto> jobDtos = jobPage.getContent().stream()
                .map(JobDto::fromEntity)
                .collect(Collectors.toList());

        return PagedResponse.of(jobDtos, jobPage.getNumber(), jobPage.getSize(), jobPage.getTotalElements());
    }

    /**
     * Get job by ID
     */
    @Transactional(readOnly = true)
    public JobDto getJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
        return JobDto.fromEntity(job);
    }

    /**
     * Get latest jobs
     */
    @Transactional(readOnly = true)
    public PagedResponse<JobDto> getLatestJobs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Job> jobPage = jobRepository.findAllByOrderByPostedAtDesc(pageable);

        // If no jobs in DB, return mock data
        if (jobPage.isEmpty()) {
            return getMockJobsPage(JobSearchRequest.builder().page(page).size(size).build());
        }

        List<JobDto> jobDtos = jobPage.getContent().stream()
                .map(JobDto::fromEntity)
                .collect(Collectors.toList());

        return PagedResponse.of(jobDtos, jobPage.getNumber(), jobPage.getSize(), jobPage.getTotalElements());
    }

    /**
     * Get remote jobs
     */
    @Transactional(readOnly = true)
    public PagedResponse<JobDto> getRemoteJobs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Job> jobPage = jobRepository.findByIsRemoteTrueOrderByPostedAtDesc(pageable);

        // If no jobs in DB, return mock remote jobs
        if (jobPage.isEmpty()) {
            List<JobDto> mockRemote = MOCK_JOBS.stream()
                    .filter(m -> m.isRemote)
                    .skip((long) page * size)
                    .limit(size)
                    .map(this::convertMockToDto)
                    .collect(Collectors.toList());
            long total = MOCK_JOBS.stream().filter(m -> m.isRemote).count();
            return PagedResponse.of(mockRemote, page, size, total);
        }

        List<JobDto> jobDtos = jobPage.getContent().stream()
                .map(JobDto::fromEntity)
                .collect(Collectors.toList());

        return PagedResponse.of(jobDtos, jobPage.getNumber(), jobPage.getSize(), jobPage.getTotalElements());
    }

    /**
     * Generate job matches for a user (mock scoring)
     */
    @Transactional
    public List<JobMatchDto> generateMatches(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Get all jobs from DB or use mock data
        List<Job> jobs = jobRepository.findAll();
        
        List<JobMatch> matches = new ArrayList<>();
        Random random = new Random();

        if (jobs.isEmpty()) {
            // Create mock jobs and save them
            for (MockJobData mockJob : MOCK_JOBS) {
                Job job = createJobFromMock(mockJob);
                job = jobRepository.save(job);
                jobs.add(job);
            }
        }

        // Generate matches for each job
        for (Job job : jobs) {
            // Skip if match already exists
            if (jobMatchRepository.existsByUserIdAndJobId(userId, job.getId())) {
                continue;
            }

            // Generate mock score (60-100)
            int score = 60 + random.nextInt(41);
            
            // Generate mock reasons
            List<String> reasons = generateMockMatchReasons(job, score);
            String reasonsJson = toJson(JobMatchDto.MatchDetailsDto.builder()
                    .reasons(reasons)
                    .matchingSkills(List.of("Java", "Spring Boot", "React"))
                    .skillMatchPercentage(score)
                    .experienceMatchPercentage(Math.min(100, score + random.nextInt(20) - 10))
                    .locationMatchPercentage(job.getIsRemote() ? 100 : 80 + random.nextInt(21))
                    .build());

            JobMatch match = JobMatch.builder()
                    .user(user)
                    .job(job)
                    .matchScore(score)
                    .matchReasonsJson(reasonsJson)
                    .status(MatchStatus.NEW)
                    .build();

            matches.add(jobMatchRepository.save(match));
        }

        log.info("Generated {} matches for user {}", matches.size(), userId);

        return matches.stream()
                .map(JobMatchDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get user's job matches
     */
    @Transactional(readOnly = true)
    public PagedResponse<JobMatchDto> getMatches(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobMatch> matchPage = jobMatchRepository.findByUserIdOrderByMatchScoreDesc(userId, pageable);

        List<JobMatchDto> matchDtos = matchPage.getContent().stream()
                .map(JobMatchDto::fromEntity)
                .collect(Collectors.toList());

        return PagedResponse.of(matchDtos, matchPage.getNumber(), matchPage.getSize(), matchPage.getTotalElements());
    }

    /**
     * Get match summary for user
     */
    @Transactional(readOnly = true)
    public JobMatchSummaryDto getMatchSummary(UUID userId) {
        long total = jobMatchRepository.countByUserId(userId);
        long newMatches = jobMatchRepository.countByUserIdAndStatus(userId, MatchStatus.NEW);
        long saved = jobMatchRepository.countByUserIdAndStatus(userId, MatchStatus.SAVED);
        long applied = jobMatchRepository.countByUserIdAndStatus(userId, MatchStatus.APPLIED);
        long viewed = jobMatchRepository.countByUserIdAndStatus(userId, MatchStatus.VIEWED);
        Double avgScore = jobMatchRepository.calculateAverageMatchScore(userId);

        return JobMatchSummaryDto.builder()
                .totalMatches(total)
                .newMatches(newMatches)
                .savedJobs(saved)
                .appliedJobs(applied)
                .viewedMatches(viewed)
                .averageMatchScore(avgScore != null ? avgScore : 0.0)
                .build();
    }

    /**
     * Mark match as viewed
     */
    @Transactional
    public JobMatchDto markViewed(UUID userId, UUID matchId) {
        JobMatch match = getMatchForUser(userId, matchId);
        
        if (match.getStatus() == MatchStatus.NEW) {
            match.setStatus(MatchStatus.VIEWED);
            match = jobMatchRepository.save(match);
        }

        return JobMatchDto.fromEntity(match);
    }

    /**
     * Save a job match
     */
    @Transactional
    public JobMatchDto saveMatch(UUID userId, UUID matchId) {
        JobMatch match = getMatchForUser(userId, matchId);
        match.setStatus(MatchStatus.SAVED);
        match = jobMatchRepository.save(match);

        // Also create a JobSave entry
        if (!jobSaveRepository.existsByUserIdAndJobId(userId, match.getJob().getId())) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            
            JobSave save = JobSave.builder()
                    .user(user)
                    .job(match.getJob())
                    .build();
            jobSaveRepository.save(save);
        }

        return JobMatchDto.fromEntity(match);
    }

    /**
     * Mark match as applied
     */
    @Transactional
    public JobMatchDto markApplied(UUID userId, UUID matchId) {
        JobMatch match = getMatchForUser(userId, matchId);
        match.setStatus(MatchStatus.APPLIED);
        match = jobMatchRepository.save(match);

        return JobMatchDto.fromEntity(match);
    }

    /**
     * Get saved jobs
     */
    @Transactional(readOnly = true)
    public PagedResponse<JobDto> getSavedJobs(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobSave> savePage = jobSaveRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<JobDto> jobDtos = savePage.getContent().stream()
                .map(save -> JobDto.fromEntity(save.getJob()))
                .collect(Collectors.toList());

        return PagedResponse.of(jobDtos, savePage.getNumber(), savePage.getSize(), savePage.getTotalElements());
    }

    /**
     * Get applied jobs
     */
    @Transactional(readOnly = true)
    public PagedResponse<JobMatchDto> getAppliedJobs(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobMatch> matchPage = jobMatchRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, MatchStatus.APPLIED, pageable);

        List<JobMatchDto> matchDtos = matchPage.getContent().stream()
                .map(JobMatchDto::fromEntity)
                .collect(Collectors.toList());

        return PagedResponse.of(matchDtos, matchPage.getNumber(), matchPage.getSize(), matchPage.getTotalElements());
    }

    /**
     * Unsave a job
     */
    @Transactional
    public void unsaveJob(UUID userId, UUID jobId) {
        jobSaveRepository.deleteByUserIdAndJobId(userId, jobId);
        
        // Update match status if exists
        jobMatchRepository.findByUserIdAndJobId(userId, jobId)
                .ifPresent(match -> {
                    if (match.getStatus() == MatchStatus.SAVED) {
                        match.setStatus(MatchStatus.VIEWED);
                        jobMatchRepository.save(match);
                    }
                });
    }

    // ==================== Helper Methods ====================

    private JobMatch getMatchForUser(UUID userId, UUID matchId) {
        JobMatch match = jobMatchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_MATCH_NOT_FOUND));
        
        if (!match.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Match does not belong to user");
        }
        
        return match;
    }

    private PagedResponse<JobDto> getMockJobsPage(JobSearchRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;

        List<MockJobData> filtered = MOCK_JOBS.stream()
                .filter(m -> request.getKeyword() == null || 
                        m.title.toLowerCase().contains(request.getKeyword().toLowerCase()) ||
                        m.company.toLowerCase().contains(request.getKeyword().toLowerCase()))
                .filter(m -> request.getLocation() == null ||
                        m.location.toLowerCase().contains(request.getLocation().toLowerCase()))
                .filter(m -> request.getType() == null ||
                        m.type.name().equals(request.getType()))
                .collect(Collectors.toList());

        List<JobDto> content = filtered.stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::convertMockToDto)
                .collect(Collectors.toList());

        return PagedResponse.of(content, page, size, filtered.size());
    }

    private JobDto convertMockToDto(MockJobData mock) {
        return JobDto.builder()
                .id(mock.id.toString())
                .title(mock.title)
                .company(mock.company)
                .companyLogo(mock.companyLogo)
                .location(mock.location)
                .type(mock.type.name())
                .salary(JobDto.SalaryDto.builder()
                        .min(mock.salaryMin)
                        .max(mock.salaryMax)
                        .currency("USD")
                        .period("year")
                        .formatted(String.format("USD %,d - %,d/year", mock.salaryMin, mock.salaryMax))
                        .build())
                .description(mock.description)
                .requirements(mock.requirements)
                .responsibilities(mock.responsibilities)
                .benefits(mock.benefits)
                .skills(mock.skills)
                .experienceLevel(mock.experienceLevel.name())
                .postedAt(mock.postedAt.toString())
                .isRemote(mock.isRemote)
                .createdAt(Instant.now().toString())
                .build();
    }

    private Job createJobFromMock(MockJobData mock) {
        return Job.builder()
                .title(mock.title)
                .company(mock.company)
                .companyLogo(mock.companyLogo)
                .location(mock.location)
                .type(mock.type)
                .salaryMin(mock.salaryMin)
                .salaryMax(mock.salaryMax)
                .salaryCurrency("USD")
                .salaryPeriod("year")
                .description(mock.description)
                .requirements(toJson(mock.requirements))
                .responsibilities(toJson(mock.responsibilities))
                .benefits(toJson(mock.benefits))
                .skills(toJson(mock.skills))
                .experienceLevel(mock.experienceLevel)
                .postedAt(mock.postedAt)
                .isRemote(mock.isRemote)
                .source("mock")
                .sourceId(mock.id.toString())
                .build();
    }

    private List<String> generateMockMatchReasons(Job job, int score) {
        List<String> reasons = new ArrayList<>();
        
        if (score >= 90) {
            reasons.add("Excellent skill match - your experience aligns perfectly with requirements");
            reasons.add("Location preference matches");
        } else if (score >= 80) {
            reasons.add("Strong skill match with most required technologies");
            reasons.add("Experience level aligns well");
        } else if (score >= 70) {
            reasons.add("Good match on core skills");
            reasons.add("Opportunity to grow in some areas");
        } else {
            reasons.add("Partial skill match - great learning opportunity");
        }

        if (job.getIsRemote()) {
            reasons.add("Remote work available - matches your preference");
        }

        return reasons;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize to JSON", e);
            return "[]";
        }
    }

    // ==================== Mock Data ====================

    private static List<MockJobData> createMockJobs() {
        List<MockJobData> jobs = new ArrayList<>();

        jobs.add(new MockJobData(
                UUID.randomUUID(),
                "Senior Software Engineer",
                "Google",
                "https://logo.clearbit.com/google.com",
                "Mountain View, CA",
                JobType.FULL_TIME,
                180000, 250000,
                "Join Google's Cloud team to build next-generation infrastructure services.",
                List.of("5+ years of software development experience", "Strong knowledge of distributed systems",
                        "Experience with Kubernetes and containerization", "Bachelor's degree in CS or equivalent"),
                List.of("Design and implement scalable cloud services", "Mentor junior engineers",
                        "Participate in code reviews and architecture discussions"),
                List.of("Competitive salary and equity", "Health, dental, and vision insurance",
                        "Unlimited PTO", "Free meals and snacks"),
                List.of("Java", "Go", "Kubernetes", "GCP", "Distributed Systems"),
                ExperienceLevel.SENIOR,
                Instant.now().minusSeconds(86400),
                false
        ));

        jobs.add(new MockJobData(
                UUID.randomUUID(),
                "Full Stack Developer",
                "Netflix",
                "https://logo.clearbit.com/netflix.com",
                "Los Gatos, CA",
                JobType.FULL_TIME,
                150000, 220000,
                "Build the future of entertainment with Netflix's product engineering team.",
                List.of("3+ years of full stack development", "Experience with React and Node.js",
                        "Strong understanding of web performance", "Experience with microservices"),
                List.of("Develop new features for Netflix UI", "Optimize performance for millions of users",
                        "Collaborate with design and product teams"),
                List.of("Top-tier compensation", "Stock options", "Unlimited vacation",
                        "Premium health benefits"),
                List.of("React", "Node.js", "TypeScript", "AWS", "GraphQL"),
                ExperienceLevel.MID,
                Instant.now().minusSeconds(172800),
                false
        ));

        jobs.add(new MockJobData(
                UUID.randomUUID(),
                "Remote Backend Engineer",
                "Stripe",
                "https://logo.clearbit.com/stripe.com",
                "Remote",
                JobType.REMOTE,
                160000, 230000,
                "Help build the economic infrastructure for the internet.",
                List.of("4+ years of backend development", "Experience with payment systems a plus",
                        "Strong knowledge of Ruby or Go", "Experience with distributed databases"),
                List.of("Design and implement payment APIs", "Ensure high availability and reliability",
                        "Work on fraud prevention systems"),
                List.of("Remote-first culture", "Equity compensation", "Learning budget",
                        "Home office setup allowance"),
                List.of("Ruby", "Go", "PostgreSQL", "Redis", "Kafka"),
                ExperienceLevel.SENIOR,
                Instant.now().minusSeconds(259200),
                true
        ));

        jobs.add(new MockJobData(
                UUID.randomUUID(),
                "Frontend Developer",
                "Airbnb",
                "https://logo.clearbit.com/airbnb.com",
                "San Francisco, CA",
                JobType.FULL_TIME,
                140000, 200000,
                "Create beautiful and intuitive experiences for travelers worldwide.",
                List.of("3+ years of frontend development", "Expert in React and TypeScript",
                        "Strong CSS and animation skills", "Experience with design systems"),
                List.of("Build responsive web applications", "Implement pixel-perfect designs",
                        "Optimize for accessibility and performance"),
                List.of("Travel credits", "Competitive salary", "Health benefits",
                        "Stock options"),
                List.of("React", "TypeScript", "CSS", "Next.js", "Figma"),
                ExperienceLevel.MID,
                Instant.now().minusSeconds(345600),
                false
        ));

        jobs.add(new MockJobData(
                UUID.randomUUID(),
                "DevOps Engineer",
                "Amazon",
                "https://logo.clearbit.com/amazon.com",
                "Seattle, WA",
                JobType.FULL_TIME,
                155000, 210000,
                "Build and maintain AWS infrastructure serving millions of customers.",
                List.of("4+ years of DevOps/SRE experience", "Expert in AWS services",
                        "Strong scripting skills (Python, Bash)", "Experience with Terraform"),
                List.of("Design and implement CI/CD pipelines", "Manage Kubernetes clusters",
                        "Implement monitoring and alerting solutions"),
                List.of("Sign-on bonus", "RSUs", "Comprehensive benefits",
                        "Career growth opportunities"),
                List.of("AWS", "Kubernetes", "Terraform", "Python", "Docker"),
                ExperienceLevel.SENIOR,
                Instant.now().minusSeconds(432000),
                false
        ));

        jobs.add(new MockJobData(
                UUID.randomUUID(),
                "Junior Software Developer",
                "Shopify",
                "https://logo.clearbit.com/shopify.com",
                "Remote",
                JobType.REMOTE,
                80000, 120000,
                "Start your career building commerce solutions for entrepreneurs.",
                List.of("0-2 years of development experience", "Knowledge of Ruby or Python",
                        "Enthusiasm for learning", "Strong problem-solving skills"),
                List.of("Develop features for Shopify platform", "Learn from senior engineers",
                        "Participate in code reviews"),
                List.of("Remote work", "Stock options", "Learning budget",
                        "Mentorship program"),
                List.of("Ruby", "Rails", "React", "GraphQL", "MySQL"),
                ExperienceLevel.ENTRY,
                Instant.now().minusSeconds(518400),
                true
        ));

        jobs.add(new MockJobData(
                UUID.randomUUID(),
                "Machine Learning Engineer",
                "OpenAI",
                "https://logo.clearbit.com/openai.com",
                "San Francisco, CA",
                JobType.FULL_TIME,
                200000, 350000,
                "Build the future of artificial intelligence.",
                List.of("MS/PhD in ML, CS, or related field", "Experience with deep learning frameworks",
                        "Published research is a plus", "Strong Python skills"),
                List.of("Research and develop new ML models", "Scale training infrastructure",
                        "Collaborate with research scientists"),
                List.of("Industry-leading compensation", "Equity", "Health benefits",
                        "Access to cutting-edge compute"),
                List.of("Python", "PyTorch", "TensorFlow", "CUDA", "Transformers"),
                ExperienceLevel.SENIOR,
                Instant.now().minusSeconds(604800),
                false
        ));

        jobs.add(new MockJobData(
                UUID.randomUUID(),
                "Contract React Developer",
                "Meta",
                "https://logo.clearbit.com/meta.com",
                "Remote",
                JobType.CONTRACT,
                100, 150,
                "6-month contract to help build next-gen social features.",
                List.of("3+ years React experience", "Experience with React Native a plus",
                        "Available for 6-month engagement", "Strong communication skills"),
                List.of("Build new features for Facebook/Instagram", "Work with design team",
                        "Write comprehensive tests"),
                List.of("Competitive hourly rate", "Flexible schedule", "Remote work",
                        "Potential for extension"),
                List.of("React", "React Native", "JavaScript", "GraphQL", "Jest"),
                ExperienceLevel.MID,
                Instant.now().minusSeconds(691200),
                true
        ));

        jobs.add(new MockJobData(
                UUID.randomUUID(),
                "Software Engineering Intern",
                "Microsoft",
                "https://logo.clearbit.com/microsoft.com",
                "Redmond, WA",
                JobType.INTERNSHIP,
                50000, 70000,
                "Summer internship with Azure team.",
                List.of("Currently pursuing CS degree", "Knowledge of C# or Java",
                        "Available for summer internship", "Strong academic record"),
                List.of("Work on real Azure features", "Learn from experienced engineers",
                        "Present project at end of internship"),
                List.of("Competitive intern salary", "Housing assistance", "Full-time offer potential",
                        "Networking events"),
                List.of("C#", ".NET", "Azure", "TypeScript", "Git"),
                ExperienceLevel.ENTRY,
                Instant.now().minusSeconds(777600),
                false
        ));

        jobs.add(new MockJobData(
                UUID.randomUUID(),
                "Engineering Lead",
                "Spotify",
                "https://logo.clearbit.com/spotify.com",
                "New York, NY",
                JobType.FULL_TIME,
                220000, 300000,
                "Lead a team building the future of audio streaming.",
                List.of("8+ years of software development", "3+ years of technical leadership",
                        "Experience with distributed systems", "Strong communication skills"),
                List.of("Lead and mentor engineering team", "Define technical roadmap",
                        "Partner with product and design"),
                List.of("Executive compensation", "Equity package", "Premium benefits",
                        "Spotify Premium for life"),
                List.of("Java", "Scala", "Kubernetes", "GCP", "Machine Learning"),
                ExperienceLevel.LEAD,
                Instant.now().minusSeconds(864000),
                false
        ));

        return jobs;
    }

    /**
     * Inner class for mock job data
     */
    private static class MockJobData {
        UUID id;
        String title;
        String company;
        String companyLogo;
        String location;
        JobType type;
        Integer salaryMin;
        Integer salaryMax;
        String description;
        List<String> requirements;
        List<String> responsibilities;
        List<String> benefits;
        List<String> skills;
        ExperienceLevel experienceLevel;
        Instant postedAt;
        boolean isRemote;

        MockJobData(UUID id, String title, String company, String companyLogo, String location,
                    JobType type, Integer salaryMin, Integer salaryMax, String description,
                    List<String> requirements, List<String> responsibilities, List<String> benefits,
                    List<String> skills, ExperienceLevel experienceLevel, Instant postedAt, boolean isRemote) {
            this.id = id;
            this.title = title;
            this.company = company;
            this.companyLogo = companyLogo;
            this.location = location;
            this.type = type;
            this.salaryMin = salaryMin;
            this.salaryMax = salaryMax;
            this.description = description;
            this.requirements = requirements;
            this.responsibilities = responsibilities;
            this.benefits = benefits;
            this.skills = skills;
            this.experienceLevel = experienceLevel;
            this.postedAt = postedAt;
            this.isRemote = isRemote;
        }
    }
}
