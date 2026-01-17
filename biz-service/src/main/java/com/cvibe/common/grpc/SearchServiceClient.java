package com.cvibe.common.grpc;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Search Service gRPC 客户端
 * 
 * 封装对 search-service 的调用。
 * 当 gRPC 服务不可用时，返回 Mock 数据。
 */
@Slf4j
@Component
public class SearchServiceClient {

    private final GrpcConfig grpcConfig;

    public SearchServiceClient(GrpcConfig grpcConfig) {
        this.grpcConfig = grpcConfig;
    }

    private boolean isAvailable() {
        try {
            var channel = grpcConfig.searchServiceChannel();
            return channel != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 搜索职位
     */
    public SearchJobsResult searchJobs(
            String query,
            List<String> locations,
            List<String> industries,
            String experienceLevel,
            String salaryRange,
            int page,
            int pageSize,
            String sortBy
    ) {
        log.info("Search Service: SearchJobs query={}", query);
        
        if (!isAvailable()) {
            return mockSearchJobs(query, page, pageSize);
        }

        // TODO: 实际调用 gRPC 服务
        return mockSearchJobs(query, page, pageSize);
    }

    /**
     * 简历-职位匹配
     */
    public MatchResult matchResumeToJob(ResumeProfile resume, JobData job) {
        log.info("Search Service: MatchResumeToJob user={}", resume.getUserId());
        
        MatchResult result = new MatchResult();
        result.setOverallScore(78.5);
        result.setDetails(new MatchDetails(85.0, 70.0, 80.0, 75.0, 82.0));
        
        List<String> matchReasons = new ArrayList<>();
        matchReasons.add("Strong skill match");
        matchReasons.add("Experience level fits");
        result.setMatchReasons(matchReasons);
        
        List<String> gapReasons = new ArrayList<>();
        gapReasons.add("Location preference differs");
        result.setGapReasons(gapReasons);
        
        return result;
    }

    /**
     * 批量匹配
     */
    public BatchMatchResult batchMatch(ResumeProfile resume, List<String> jobIds) {
        log.info("Search Service: BatchMatch jobs={}", jobIds.size());
        
        BatchMatchResult result = new BatchMatchResult();
        
        List<JobMatchItem> matches = new ArrayList<>();
        for (int i = 0; i < jobIds.size(); i++) {
            matches.add(new JobMatchItem(
                jobIds.get(i),
                85.0 - i * 5,
                new MatchDetails(80.0, 75.0, 70.0, 65.0, 60.0)
            ));
        }
        result.setMatches(matches);
        result.setProcessed(jobIds.size());
        result.setFailed(0);
        
        return result;
    }

    /**
     * 获取推荐职位
     */
    public RecommendResult getRecommendations(
            String userId,
            ResumeProfile resume,
            int limit,
            List<String> excludeJobIds
    ) {
        log.info("Search Service: GetRecommendations user={}", userId);
        
        RecommendResult result = new RecommendResult();
        
        List<RecommendedJob> recommendations = new ArrayList<>();
        recommendations.add(new RecommendedJob(
            createMockJob("job-001", "Senior Software Engineer", "Google"),
            92.5,
            "Your skills match well with this position"
        ));
        recommendations.add(new RecommendedJob(
            createMockJob("job-002", "Backend Developer", "ByteDance"),
            88.0,
            "Great experience match"
        ));
        recommendations.add(new RecommendedJob(
            createMockJob("job-003", "Full Stack Developer", "Alibaba"),
            85.0,
            "Location and skills match"
        ));
        
        result.setRecommendations(recommendations);
        return result;
    }

    /**
     * 搜索建议
     */
    public SuggestionResult getSuggestions(String prefix, int limit) {
        log.info("Search Service: GetSuggestions prefix={}", prefix);
        
        SuggestionResult result = new SuggestionResult();
        
        List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(new Suggestion("Software Engineer", "job_title", 150));
        suggestions.add(new Suggestion("Senior Developer", "job_title", 120));
        suggestions.add(new Suggestion("Google", "company", 80));
        
        result.setSuggestions(suggestions);
        return result;
    }

    // ==================== Mock implementations ====================

    private SearchJobsResult mockSearchJobs(String query, int page, int pageSize) {
        SearchJobsResult result = new SearchJobsResult();
        
        List<JobData> jobs = new ArrayList<>();
        jobs.add(createMockJob("job-001", "Senior Software Engineer", "Google"));
        jobs.add(createMockJob("job-002", "Backend Developer", "ByteDance"));
        jobs.add(createMockJob("job-003", "Full Stack Developer", "Alibaba"));
        jobs.add(createMockJob("job-004", "Frontend Engineer", "Tencent"));
        jobs.add(createMockJob("job-005", "Data Scientist", "Huawei"));
        
        result.setJobs(jobs);
        result.setTotal(jobs.size());
        result.setPage(page);
        result.setTotalPages(1);
        
        return result;
    }

    private JobData createMockJob(String id, String title, String company) {
        JobData job = new JobData();
        job.setId(id);
        job.setTitle(title);
        job.setCompany(company);
        job.setCompanyLogo("https://logo.clearbit.com/" + company.toLowerCase() + ".com");
        job.setLocation("Beijing, China");
        job.setSalaryRange("$100,000 - $150,000");
        job.setExperience("3-5 years");
        job.setEmploymentType("full-time");
        job.setDescription("Join our team to build amazing products...");
        
        List<String> requirements = new ArrayList<>();
        requirements.add("5+ years experience");
        requirements.add("Strong programming skills");
        job.setRequirements(requirements);
        
        List<String> benefits = new ArrayList<>();
        benefits.add("Competitive salary");
        benefits.add("Health insurance");
        job.setBenefits(benefits);
        
        job.setPostedAt("2026-01-15T00:00:00Z");
        job.setSource("direct");
        job.setSourceUrl("https://careers." + company.toLowerCase() + ".com/jobs/" + id);
        
        return job;
    }

    // ==================== DTOs ====================

    @Data
    public static class SearchJobsResult {
        private List<JobData> jobs = new ArrayList<>();
        private int total;
        private int page;
        private int totalPages;
        private List<Facet> facets = new ArrayList<>();
    }

    @Data
    public static class JobData {
        private String id;
        private String title;
        private String company;
        private String companyLogo;
        private String location;
        private String salaryRange;
        private String experience;
        private String employmentType;
        private String description;
        private List<String> requirements = new ArrayList<>();
        private List<String> benefits = new ArrayList<>();
        private String postedAt;
        private String source;
        private String sourceUrl;
        private double matchScore;
    }

    @Data
    public static class Facet {
        private String name;
        private List<FacetItem> items = new ArrayList<>();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FacetItem {
        private String value;
        private int count;
    }

    @Data
    public static class ResumeProfile {
        private String userId;
        private String title;
        private List<String> skills = new ArrayList<>();
        private int yearsExperience;
        private List<String> preferredLocations = new ArrayList<>();
        private String expectedSalary;
    }

    @Data
    public static class MatchResult {
        private double overallScore;
        private MatchDetails details;
        private List<String> matchReasons = new ArrayList<>();
        private List<String> gapReasons = new ArrayList<>();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MatchDetails {
        private double skillMatch;
        private double experienceMatch;
        private double locationMatch;
        private double salaryMatch;
        private double titleMatch;
    }

    @Data
    public static class BatchMatchResult {
        private List<JobMatchItem> matches = new ArrayList<>();
        private int processed;
        private int failed;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JobMatchItem {
        private String jobId;
        private double score;
        private MatchDetails details;
    }

    @Data
    public static class RecommendResult {
        private List<RecommendedJob> recommendations = new ArrayList<>();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecommendedJob {
        private JobData job;
        private double matchScore;
        private String recommendReason;
    }

    @Data
    public static class SuggestionResult {
        private List<Suggestion> suggestions = new ArrayList<>();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Suggestion {
        private String text;
        private String type;
        private int count;
    }
}
