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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedContent {

    private PersonalInfo personalInfo;
    private String summary;
    private List<WorkExperience> experiences;
    private List<Education> education;
    private List<String> skills;

    /**
     * 创建空的解析内容（避免前端 null 报错）
     */
    public static ParsedContent empty() {
        return ParsedContent.builder()
                .personalInfo(new PersonalInfo())
                .summary("")
                .experiences(new ArrayList<>())
                .education(new ArrayList<>())
                .skills(new ArrayList<>())
                .build();
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
    }

    /**
     * 工作经历
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkExperience {
        private String company;
        private String title;
        private String startDate;
        private String endDate;
        private String description;
        private List<String> achievements;
    }

    /**
     * 教育经历
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Education {
        private String school;
        private String degree;
        private String field;
        private String graduationDate;
        private String gpa;
    }
}
