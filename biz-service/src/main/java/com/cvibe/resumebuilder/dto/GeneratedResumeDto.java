package com.cvibe.resumebuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成的简历 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedResumeDto {

    private String id;
    private String templateId;
    private String templateName;
    private String targetPosition;
    private String targetCompany;
    
    /**
     * 状态: PROCESSING, COMPLETED, FAILED
     */
    private String status;
    
    /**
     * 生成的简历内容
     */
    private ResumeContent content;
    
    /**
     * LaTeX 源代码
     */
    private String latexContent;
    
    /**
     * HTML 预览
     */
    private String htmlPreview;
    
    /**
     * PDF 下载 URL
     */
    private String pdfUrl;
    
    /**
     * 下载 URL（pdfUrl 别名）
     */
    private String downloadUrl;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    private String createdAt;
    private String updatedAt;

    /**
     * 简历内容结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeContent {
        private PersonalInfo personalInfo;
        private String summary;
        private List<ExperienceItem> experiences;
        private List<EducationItem> education;
        private SkillsGroup skills;

        public static ResumeContent empty() {
            return ResumeContent.builder()
                    .personalInfo(new PersonalInfo())
                    .summary("")
                    .experiences(new ArrayList<>())
                    .education(new ArrayList<>())
                    .skills(new SkillsGroup())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalInfo {
        private String name;
        private String email;
        private String phone;
        private String location;
        private String linkedin;
        private String github;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperienceItem {
        private String company;
        private String title;
        private String startDate;
        private String endDate;
        private String description;
        @Builder.Default
        private List<String> bullets = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationItem {
        private String school;
        private String degree;
        private String field;
        private String graduationDate;
        private String gpa;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillsGroup {
        @Builder.Default
        private List<String> technical = new ArrayList<>();
        @Builder.Default
        private List<String> soft = new ArrayList<>();
    }
}
