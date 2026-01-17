package com.cvibe.resumebuilder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成简历请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResumeRequest {

    @NotBlank(message = "模板 ID 不能为空")
    private String templateId;

    @NotBlank(message = "目标职位不能为空")
    @Size(max = 100, message = "目标职位不能超过 100 个字符")
    private String targetPosition;

    @Size(max = 100, message = "目标公司不能超过 100 个字符")
    private String targetCompany;

    /**
     * 自定义指令（给 AI 的额外说明）
     */
    private String customInstructions;

    /**
     * 定制选项
     */
    private Customizations customizations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customizations {
        /**
         * 语气风格: professional（专业）, friendly（友好）, confident（自信）
         */
        private String tone;

        /**
         * 侧重点: technical（技术）, leadership（领导力）, achievements（成就）
         */
        private String focus;

        /**
         * 长度: brief（简短）, standard（标准）, detailed（详细）
         */
        private String length;
    }
}
