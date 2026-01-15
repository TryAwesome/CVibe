package com.cvibe.biz.resume.service;

import com.cvibe.biz.profile.entity.*;
import com.cvibe.biz.profile.repository.*;
import com.cvibe.biz.resume.dto.*;
import com.cvibe.biz.resume.entity.ResumeGeneration;
import com.cvibe.biz.resume.entity.ResumeGeneration.GenerationStatus;
import com.cvibe.biz.resume.entity.ResumeTemplate;
import com.cvibe.biz.resume.entity.ResumeTemplate.TemplateCategory;
import com.cvibe.biz.resume.entity.ResumeTemplate.TemplateType;
import com.cvibe.biz.resume.repository.ResumeGenerationRepository;
import com.cvibe.biz.resume.repository.ResumeTemplateRepository;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ResumeBuilderService
 * 
 * Handles resume template management and resume generation/tailoring.
 * Integrates with Profile Database and AI Engine for content generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeBuilderService {

    private final ResumeTemplateRepository templateRepository;
    private final ResumeGenerationRepository generationRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final ProfileSkillRepository skillRepository;
    private final ProfileExperienceRepository experienceRepository;
    private final ProfileEducationRepository educationRepository;
    private final ProfileProjectRepository projectRepository;
    private final ProfileCertificationRepository certificationRepository;
    private final ObjectMapper objectMapper;

    // ================== Template Management ==================

    /**
     * Get all available templates for user
     */
    @Transactional(readOnly = true)
    public Page<ResumeTemplateDto> getAvailableTemplates(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return templateRepository.findAvailableTemplates(userId, pageable)
                .map(t -> ResumeTemplateDto.from(t, userId));
    }

    /**
     * Get templates by category
     */
    @Transactional(readOnly = true)
    public List<ResumeTemplateDto> getTemplatesByCategory(TemplateCategory category) {
        return templateRepository.findByCategoryAndIsActiveTrueOrderByUsageCountDesc(category)
                .stream()
                .map(ResumeTemplateDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Get featured templates
     */
    @Transactional(readOnly = true)
    public List<ResumeTemplateDto> getFeaturedTemplates() {
        return templateRepository.findByIsFeaturedTrueAndIsActiveTrueOrderByUsageCountDesc()
                .stream()
                .map(ResumeTemplateDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Get user's custom templates
     */
    @Transactional(readOnly = true)
    public List<ResumeTemplateDto> getUserTemplates(UUID userId) {
        return templateRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId)
                .stream()
                .map(t -> ResumeTemplateDto.from(t, userId))
                .collect(Collectors.toList());
    }

    /**
     * Get template content (LaTeX)
     */
    @Transactional(readOnly = true)
    public String getTemplateContent(UUID templateId, UUID userId) {
        ResumeTemplate template = templateRepository.findByIdAndIsActiveTrue(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Template not found"));

        // Check access: system templates are public, user templates are private
        if (template.isUserTemplate() && !template.belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return template.getLatexContent();
    }

    /**
     * Create user template
     */
    @Transactional
    public ResumeTemplateDto createUserTemplate(UUID userId, TemplateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Check for duplicate name
        if (templateRepository.existsByUserIdAndNameIgnoreCase(userId, request.getName())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Template with this name already exists");
        }

        ResumeTemplate template = ResumeTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .templateType(TemplateType.USER)
                .category(request.getCategory())
                .latexContent(request.getLatexContent())
                .user(user)
                .isActive(true)
                .isFeatured(false)
                .usageCount(0)
                .build();

        template = templateRepository.save(template);
        log.info("Created user template {} for user {}", template.getId(), userId);

        return ResumeTemplateDto.from(template, userId);
    }

    /**
     * Update user template
     */
    @Transactional
    public ResumeTemplateDto updateUserTemplate(UUID userId, UUID templateId, TemplateRequest request) {
        ResumeTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Template not found"));

        if (!template.belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setCategory(request.getCategory());
        template.setLatexContent(request.getLatexContent());

        template = templateRepository.save(template);
        return ResumeTemplateDto.from(template, userId);
    }

    /**
     * Delete user template
     */
    @Transactional
    public void deleteUserTemplate(UUID userId, UUID templateId) {
        ResumeTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Template not found"));

        if (!template.belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        template.setIsActive(false);  // Soft delete
        templateRepository.save(template);
        log.info("Deleted user template {} for user {}", templateId, userId);
    }

    // ================== Resume Generation ==================

    /**
     * Generate tailored resume
     */
    @Transactional
    public GeneratedResumeDto generateResume(UUID userId, GenerateResumeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ResumeTemplate template = templateRepository.findByIdAndIsActiveTrue(request.getTemplateId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Template not found"));

        // Check template access
        if (template.isUserTemplate() && !template.belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // Get user profile
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        // Create generation record
        ResumeGeneration generation = ResumeGeneration.builder()
                .user(user)
                .template(template)
                .targetJobTitle(request.getTargetJobTitle())
                .targetCompany(request.getTargetCompany())
                .jobDescription(request.getJobDescription())
                .jdFilePath(request.getJdFilePath())
                .status(GenerationStatus.PENDING)
                .build();

        generation = generationRepository.save(generation);

        // Generate resume content
        try {
            generation.markAsAnalyzing();
            generationRepository.save(generation);

            // Extract profile data
            ProfileData profileData = extractProfileData(profile);

            // Analyze JD and extract keywords
            List<String> keywords = analyzeJobDescription(request.getJobDescription());
            generation.setMatchedKeywords(toJsonArray(keywords));

            generation.markAsGenerating();
            generationRepository.save(generation);

            // Generate tailored LaTeX content
            String tailoredLatex = generateTailoredLatex(
                    template.getLatexContent(),
                    profileData,
                    request.getJobDescription(),
                    keywords,
                    request.getCustomInstructions()
            );

            generation.setGeneratedLatex(tailoredLatex);
            generation.setFinalLatex(tailoredLatex);  // Initial version
            generation.setTailoringNotes(generateTailoringNotes(keywords, profileData));

            // Mark as completed (PDF compilation would be done separately)
            generation.markAsCompleted(null);  // PDF path will be set when compiled
            generationRepository.save(generation);

            // Increment template usage
            template.incrementUsage();
            templateRepository.save(template);

            log.info("Generated resume {} for user {}", generation.getId(), userId);

            return GeneratedResumeDto.from(generation, true);

        } catch (Exception e) {
            log.error("Failed to generate resume for user {}: {}", userId, e.getMessage());
            generation.markAsFailed(e.getMessage());
            generationRepository.save(generation);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Resume generation failed: " + e.getMessage());
        }
    }

    /**
     * Update generated resume LaTeX (user edits)
     */
    @Transactional
    public GeneratedResumeDto updateGeneratedLatex(UUID userId, UUID generationId, String newLatex) {
        ResumeGeneration generation = getGenerationForUser(userId, generationId);

        generation.updateFinalLatex(newLatex);
        generation = generationRepository.save(generation);

        return GeneratedResumeDto.from(generation, true);
    }

    /**
     * Export resume (mark as exported, trigger PDF generation)
     */
    @Transactional
    public GeneratedResumeDto exportResume(UUID userId, UUID generationId) {
        ResumeGeneration generation = getGenerationForUser(userId, generationId);

        if (!generation.isCompleted()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Resume generation not completed");
        }

        // TODO: Trigger actual LaTeX -> PDF compilation via AI Engine
        // For now, we just mark as exported
        generation.markAsExported();
        generation = generationRepository.save(generation);

        log.info("Exported resume {} for user {}", generationId, userId);

        return GeneratedResumeDto.from(generation);
    }

    /**
     * Get user's generation history
     */
    @Transactional(readOnly = true)
    public Page<GeneratedResumeDto> getGenerationHistory(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return generationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(GeneratedResumeDto::from);
    }

    /**
     * Get generation details
     */
    @Transactional(readOnly = true)
    public GeneratedResumeDto getGeneration(UUID userId, UUID generationId, boolean includeLatex) {
        ResumeGeneration generation = getGenerationForUser(userId, generationId);
        return GeneratedResumeDto.from(generation, includeLatex);
    }

    /**
     * Submit rating for generation
     */
    @Transactional
    public GeneratedResumeDto submitRating(UUID userId, UUID generationId, Integer rating, String feedback) {
        ResumeGeneration generation = getGenerationForUser(userId, generationId);
        generation.submitRating(rating, feedback);
        generation = generationRepository.save(generation);
        return GeneratedResumeDto.from(generation);
    }

    // ================== Dashboard ==================

    /**
     * Get resume builder summary for dashboard
     */
    @Transactional(readOnly = true)
    public ResumeBuilderSummary getSummary(UUID userId) {
        long totalGenerations = generationRepository.countByUserId(userId);
        long exportedCount = generationRepository.countByUserIdAndIsExportedTrue(userId);
        long customTemplates = templateRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId).size();

        List<ResumeGeneration> recent = generationRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);
        List<ResumeTemplate> featured = templateRepository.findByIsFeaturedTrueAndIsActiveTrueOrderByUsageCountDesc();

        // Get top companies from user's generations
        List<String> topCompanies = recent.stream()
                .map(ResumeGeneration::getTargetCompany)
                .filter(Objects::nonNull)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        return ResumeBuilderSummary.builder()
                .totalGenerations(totalGenerations)
                .exportedCount(exportedCount)
                .customTemplates(customTemplates)
                .topCompanies(topCompanies)
                .recentGenerations(recent.stream()
                        .map(GeneratedResumeDto::from)
                        .collect(Collectors.toList()))
                .featuredTemplates(featured.stream()
                        .limit(3)
                        .map(ResumeTemplateDto::from)
                        .collect(Collectors.toList()))
                .build();
    }

    // ================== Admin Methods ==================

    /**
     * Create system template (admin only)
     */
    @Transactional
    public ResumeTemplateDto createSystemTemplate(TemplateRequest request) {
        if (templateRepository.existsByTemplateTypeAndNameIgnoreCase(TemplateType.SYSTEM, request.getName())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "System template with this name already exists");
        }

        ResumeTemplate template = ResumeTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .templateType(TemplateType.SYSTEM)
                .category(request.getCategory())
                .latexContent(request.getLatexContent())
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .isActive(true)
                .usageCount(0)
                .build();

        template = templateRepository.save(template);
        log.info("Created system template {}", template.getId());

        return ResumeTemplateDto.from(template);
    }

    /**
     * Get template statistics (admin)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalSystemTemplates", templateRepository.countByTemplateType(TemplateType.SYSTEM));
        stats.put("totalUserTemplates", templateRepository.countByTemplateType(TemplateType.USER));
        stats.put("totalGenerations", generationRepository.count());
        stats.put("completedGenerations", generationRepository.countByStatus(GenerationStatus.COMPLETED));
        stats.put("failedGenerations", generationRepository.countByStatus(GenerationStatus.FAILED));
        stats.put("averageRating", generationRepository.getAverageRating());
        stats.put("topTemplates", templateRepository.findTop10ByIsActiveTrueOrderByUsageCountDesc()
                .stream()
                .map(ResumeTemplateDto::from)
                .collect(Collectors.toList()));

        return stats;
    }

    // ================== Private Helper Methods ==================

    private ResumeGeneration getGenerationForUser(UUID userId, UUID generationId) {
        ResumeGeneration generation = generationRepository.findById(generationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Generation not found"));

        if (!generation.belongsToUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return generation;
    }

    /**
     * Extract all profile data for template injection
     */
    private ProfileData extractProfileData(UserProfile profile) {
        List<ProfileSkill> skills = skillRepository.findByProfileId(profile.getId());
        List<ProfileExperience> experiences = experienceRepository.findByProfileIdOrderByStartDateDesc(profile.getId());
        List<ProfileEducation> educations = educationRepository.findByProfileIdOrderByStartDateDesc(profile.getId());
        List<ProfileProject> projects = projectRepository.findByProfileIdOrderByStartDateDesc(profile.getId());
        List<ProfileCertification> certs = certificationRepository.findByProfileId(profile.getId());

        return new ProfileData(profile, skills, experiences, educations, projects, certs);
    }

    /**
     * Analyze job description and extract keywords
     * TODO: Replace with AI-powered analysis
     */
    private List<String> analyzeJobDescription(String jd) {
        if (jd == null || jd.isBlank()) {
            return List.of();
        }

        // Simple keyword extraction (in production, use AI)
        Set<String> keywords = new HashSet<>();
        String lower = jd.toLowerCase();

        TECH_KEYWORDS.forEach(keyword -> {
            if (lower.contains(keyword.toLowerCase())) {
                keywords.add(keyword);
            }
        });

        return new ArrayList<>(keywords);
    }

    /**
     * Generate tailored LaTeX content
     * TODO: Replace with AI-powered generation via gRPC to AI Engine
     */
    private String generateTailoredLatex(String templateContent, ProfileData data,
                                          String jd, List<String> keywords, String customInstructions) {
        String latex = templateContent;

        // Replace placeholders with profile data
        latex = latex.replace("{{NAME}}", data.profile.getUser().getFullName() != null 
                ? data.profile.getUser().getFullName() : "");
        latex = latex.replace("{{EMAIL}}", data.profile.getUser().getEmail() != null 
                ? escapeLatex(data.profile.getUser().getEmail()) : "");
        latex = latex.replace("{{PHONE}}", data.profile.getPhone() != null 
                ? data.profile.getPhone() : "");
        latex = latex.replace("{{LOCATION}}", data.profile.getLocation() != null 
                ? escapeLatex(data.profile.getLocation()) : "");
        latex = latex.replace("{{HEADLINE}}", data.profile.getHeadline() != null 
                ? escapeLatex(data.profile.getHeadline()) : "");
        latex = latex.replace("{{SUMMARY}}", data.profile.getSummary() != null 
                ? escapeLatex(data.profile.getSummary()) : "");
        latex = latex.replace("{{LINKEDIN}}", data.profile.getLinkedinUrl() != null 
                ? escapeLatex(data.profile.getLinkedinUrl()) : "");
        latex = latex.replace("{{GITHUB}}", data.profile.getGithubUrl() != null 
                ? escapeLatex(data.profile.getGithubUrl()) : "");
        latex = latex.replace("{{PORTFOLIO}}", data.profile.getPortfolioUrl() != null 
                ? escapeLatex(data.profile.getPortfolioUrl()) : "");

        // Generate skills section
        String skillsContent = generateSkillsSection(data.skills, keywords);
        latex = latex.replace("{{SKILLS}}", skillsContent);

        // Generate experience section
        String experienceContent = generateExperienceSection(data.experiences, keywords);
        latex = latex.replace("{{EXPERIENCE}}", experienceContent);

        // Generate education section
        String educationContent = generateEducationSection(data.educations);
        latex = latex.replace("{{EDUCATION}}", educationContent);

        // Generate projects section
        String projectsContent = generateProjectsSection(data.projects, keywords);
        latex = latex.replace("{{PROJECTS}}", projectsContent);

        // Generate certifications section
        String certsContent = generateCertificationsSection(data.certifications);
        latex = latex.replace("{{CERTIFICATIONS}}", certsContent);

        return latex;
    }

    private String generateSkillsSection(List<ProfileSkill> skills, List<String> keywords) {
        if (skills.isEmpty()) {
            return "";
        }

        // Prioritize skills that match JD keywords
        Set<String> keywordSet = keywords.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<ProfileSkill> prioritized = new ArrayList<>(skills);
        prioritized.sort((a, b) -> {
            boolean aMatch = keywordSet.contains(a.getName().toLowerCase());
            boolean bMatch = keywordSet.contains(b.getName().toLowerCase());
            if (aMatch && !bMatch) return -1;
            if (!aMatch && bMatch) return 1;
            return Integer.compare(b.getYearsOfExperience() != null ? b.getYearsOfExperience() : 0,
                    a.getYearsOfExperience() != null ? a.getYearsOfExperience() : 0);
        });

        return prioritized.stream()
                .map(s -> escapeLatex(s.getName()))
                .collect(Collectors.joining(", "));
    }

    private String generateExperienceSection(List<ProfileExperience> experiences, List<String> keywords) {
        if (experiences.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");

        for (ProfileExperience exp : experiences) {
            sb.append("\\textbf{").append(escapeLatex(exp.getTitle())).append("} \\hfill ");
            sb.append(formatDateRange(exp.getStartDate(), exp.getEndDate(), exp.getIsCurrent())).append(" \\\\\n");
            sb.append("\\textit{").append(escapeLatex(exp.getCompany()));
            if (exp.getLocation() != null) {
                sb.append(", ").append(escapeLatex(exp.getLocation()));
            }
            sb.append("} \\\\\n");

            if (exp.getDescription() != null) {
                sb.append("\\begin{itemize}[noitemsep]\n");
                // Split description into bullet points
                String[] points = exp.getDescription().split("\n|\\. ");
                for (String point : points) {
                    point = point.trim();
                    if (!point.isEmpty()) {
                        sb.append("  \\item ").append(escapeLatex(point));
                        if (!point.endsWith(".")) sb.append(".");
                        sb.append("\n");
                    }
                }
                sb.append("\\end{itemize}\n");
            }
            sb.append("\\vspace{0.5em}\n\n");
        }

        return sb.toString();
    }

    private String generateEducationSection(List<ProfileEducation> educations) {
        if (educations.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (ProfileEducation edu : educations) {
            sb.append("\\textbf{").append(escapeLatex(edu.getDegree()));
            if (edu.getFieldOfStudy() != null) {
                sb.append(" in ").append(escapeLatex(edu.getFieldOfStudy()));
            }
            sb.append("} \\hfill ");
            sb.append(formatDateRange(edu.getStartDate(), edu.getEndDate(), false)).append(" \\\\\n");
            sb.append("\\textit{").append(escapeLatex(edu.getInstitution()));
            if (edu.getLocation() != null) {
                sb.append(", ").append(escapeLatex(edu.getLocation()));
            }
            sb.append("}");
            if (edu.getGpa() != null) {
                sb.append(" \\hfill GPA: ").append(edu.getGpa());
            }
            sb.append(" \\\\\n\\vspace{0.5em}\n\n");
        }

        return sb.toString();
    }

    private String generateProjectsSection(List<ProfileProject> projects, List<String> keywords) {
        if (projects.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (ProfileProject proj : projects) {
            sb.append("\\textbf{").append(escapeLatex(proj.getName())).append("}");
            if (proj.getProjectUrl() != null) {
                sb.append(" \\href{").append(escapeLatex(proj.getProjectUrl())).append("}{[Link]}");
            }
            sb.append(" \\hfill ");
            sb.append(formatDateRange(proj.getStartDate(), proj.getEndDate(), false)).append(" \\\\\n");

            if (proj.getDescription() != null) {
                sb.append(escapeLatex(proj.getDescription())).append(" \\\\\n");
            }

            if (proj.getTechnologies() != null) {
                sb.append("\\textit{Technologies: ").append(escapeLatex(proj.getTechnologies())).append("} \\\\\n");
            }
            sb.append("\\vspace{0.5em}\n\n");
        }

        return sb.toString();
    }

    private String generateCertificationsSection(List<ProfileCertification> certs) {
        if (certs.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (ProfileCertification cert : certs) {
            sb.append("\\textbf{").append(escapeLatex(cert.getName())).append("}");
            sb.append(" -- ").append(escapeLatex(cert.getIssuingOrganization()));
            if (cert.getIssueDate() != null) {
                sb.append(" \\hfill ").append(cert.getIssueDate().format(DateTimeFormatter.ofPattern("MMM yyyy")));
            }
            sb.append(" \\\\\n");
        }

        return sb.toString();
    }

    private String formatDateRange(LocalDate start, LocalDate end, Boolean isCurrent) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
        String startStr = start != null ? start.format(fmt) : "";
        String endStr = Boolean.TRUE.equals(isCurrent) ? "Present" : (end != null ? end.format(fmt) : "");
        return startStr + " -- " + endStr;
    }

    private String escapeLatex(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\textbackslash{}")
                .replace("&", "\\&")
                .replace("%", "\\%")
                .replace("$", "\\$")
                .replace("#", "\\#")
                .replace("_", "\\_")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("~", "\\textasciitilde{}")
                .replace("^", "\\textasciicircum{}");
    }

    private String generateTailoringNotes(List<String> keywords, ProfileData data) {
        StringBuilder notes = new StringBuilder();
        notes.append("Resume tailored based on the following:\n\n");
        notes.append("**Matched Keywords:** ").append(String.join(", ", keywords)).append("\n\n");
        notes.append("**Included Sections:**\n");
        notes.append("- Skills: ").append(data.skills.size()).append(" items\n");
        notes.append("- Experience: ").append(data.experiences.size()).append(" positions\n");
        notes.append("- Education: ").append(data.educations.size()).append(" entries\n");
        notes.append("- Projects: ").append(data.projects.size()).append(" projects\n");
        notes.append("- Certifications: ").append(data.certifications.size()).append(" certifications\n");
        return notes.toString();
    }

    private String toJsonArray(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    // Profile data container
    private record ProfileData(
            UserProfile profile,
            List<ProfileSkill> skills,
            List<ProfileExperience> experiences,
            List<ProfileEducation> educations,
            List<ProfileProject> projects,
            List<ProfileCertification> certifications
    ) {}

    // Common tech keywords for extraction
    private static final List<String> TECH_KEYWORDS = List.of(
            "Java", "Python", "JavaScript", "TypeScript", "Go", "Rust", "C++", "C#", "Ruby", "Kotlin",
            "React", "Angular", "Vue", "Node.js", "Spring", "Django", "Flask", "FastAPI",
            "AWS", "GCP", "Azure", "Docker", "Kubernetes", "Terraform", "Ansible",
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Elasticsearch", "Cassandra",
            "Kafka", "RabbitMQ", "GraphQL", "REST", "gRPC", "WebSocket",
            "Machine Learning", "Deep Learning", "NLP", "Computer Vision", "TensorFlow", "PyTorch",
            "Agile", "Scrum", "CI/CD", "DevOps", "Microservices", "Distributed Systems",
            "Linux", "Git", "Jenkins", "GitHub Actions", "Prometheus", "Grafana"
    );
}
