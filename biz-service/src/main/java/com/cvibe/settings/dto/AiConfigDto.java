package com.cvibe.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 配置 DTO，用于请求和响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfigDto {

    /**
     * 语言偏好: zh（中文）, en（英文）
     */
    @NotBlank(message = "语言不能为空")
    private String language;

    /**
     * AI 响应风格: concise（简洁）, detailed（详细）, balanced（平衡）
     */
    @NotBlank(message = "响应风格不能为空")
    private String responseStyle;

    /**
     * 面试难度: easy（简单）, medium（中等）, hard（困难）
     */
    private String interviewDifficulty;

    /**
     * 关注领域列表
     * 例如: ["algorithms", "system-design", "behavioral"]
     */
    private List<String> focusAreas;

    /**
     * 自定义指令
     */
    @Size(max = 1000, message = "自定义指令不能超过 1000 个字符")
    private String customInstructions;

    /**
     * 创建默认配置
     */
    public static AiConfigDto createDefault() {
        return AiConfigDto.builder()
                .language("zh")
                .responseStyle("balanced")
                .interviewDifficulty("medium")
                .focusAreas(List.of())
                .customInstructions(null)
                .build();
    }
}
