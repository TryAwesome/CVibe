package com.cvibe.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 简历解析内容 DTO
 * 存储 AI 解析后的结构化简历数据
 * 
 * 增强版本 - 包含完整的简历信息：
 * - 个人信息 (含 LinkedIn, GitHub, Website)
 * - 工作经历 (含成就、技术栈)
 * - 教育经历 (含活动、荣誉)
 * - 项目经历
 * - 技能 (含熟练度)
 * - 证书
 * - 语言能力
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedContent {

    private PersonalInfo personalInfo;
    private String headline;           // 职业头衔
    private String summary;            // 个人简介
    private List<WorkExperience> experiences;
    private List<Education> education;
    private List<Project> projects;
    private List<Skill> skills;
    private List<Certification> certifications;
    private List<Language> languages;
    private List<String> achievements; // 荣誉/奖项

    /**
     * 创建空的解析内容（避免前端 null 报错）
     */
    public static ParsedContent empty() {
        return ParsedContent.builder()
                .personalInfo(new PersonalInfo())
                .headline("")
                .summary("")
                .experiences(new ArrayList<>())
                .education(new ArrayList<>())
                .projects(new ArrayList<>())
                .skills(new ArrayList<>())
                .certifications(new ArrayList<>())
                .languages(new ArrayList<>())
                .achievements(new ArrayList<>())
                .build();
    }

    /**
     * 获取技能名称列表（兼容旧接口）
     */
    public List<String> getSkillNames() {
        if (skills == null) return new ArrayList<>();
        return new ArrayList<>(skills.stream()
                .map(Skill::getName)
                .toList());
    }

    /**
     * 个人信息
     */
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
        private String website;
    }

    /**
     * 工作经历 (增强版)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkExperience {
        private String company;
        private String title;
        private String location;
        private String employmentType;   // FULL_TIME, PART_TIME, etc.
        private String startDate;        // YYYY-MM
        private String endDate;          // YYYY-MM or "present"
        private Boolean isCurrent;
        private String description;
        private List<String> achievements;
        private List<String> technologies;
    }

    /**
     * 教育经历 (增强版)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Education {
        private String school;
        private String degree;
        private String field;
        private String location;
        private String startDate;
        private String endDate;
        private String graduationDate;   // 兼容旧字段
        private String gpa;
        private String description;
        private List<String> activities;
        private List<String> honors;
    }

    /**
     * 项目经历
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Project {
        private String name;
        private String description;
        private String url;
        private String repoUrl;
        private List<String> technologies;
        private String startDate;
        private String endDate;
        private List<String> highlights;
    }

    /**
     * 技能 (含熟练度)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Skill {
        private String name;
        private String level;       // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
        private String category;    // 编程语言, 框架, 工具 等
    }

    /**
     * 证书
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Certification {
        private String name;
        private String issuer;
        private String date;
        private String url;
    }

    /**
     * 语言能力
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Language {
        private String language;
        private String proficiency;  // Native, Fluent, Professional, Basic
    }
}
