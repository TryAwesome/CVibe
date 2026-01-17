package com.cvibe.settings.service;

import com.cvibe.auth.entity.User;
import com.cvibe.auth.repository.UserRepository;
import com.cvibe.common.exception.BusinessException;
import com.cvibe.common.exception.ErrorCode;
import com.cvibe.settings.dto.AiConfigDto;
import com.cvibe.settings.dto.ChangePasswordRequest;
import com.cvibe.settings.entity.UserAiConfig;
import com.cvibe.settings.repository.UserAiConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 设置服务
 * 处理密码修改和 AI 配置管理
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SettingsService {

    private final UserRepository userRepository;
    private final UserAiConfigRepository aiConfigRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    // 有效的语言选项
    private static final Set<String> VALID_LANGUAGES = Set.of("zh", "en");
    
    // 有效的响应风格
    private static final Set<String> VALID_RESPONSE_STYLES = Set.of("concise", "detailed", "balanced");
    
    // 有效的面试难度
    private static final Set<String> VALID_DIFFICULTIES = Set.of("easy", "medium", "hard");

    /**
     * 修改用户密码
     * 
     * @param userId 用户 ID
     * @param request 密码修改请求
     * @throws BusinessException 当前密码错误或新密码与旧密码相同时抛出
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 验证用户是否有密码（Google 登录用户可能没有密码）
        if (user.getPasswordHash() == null) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "此账户使用第三方登录，无法修改密码");
        }

        // 验证当前密码
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 验证新密码不能与旧密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("用户 {} 成功修改密码", userId);
    }

    /**
     * 获取用户 AI 配置
     * 如果用户没有配置，返回默认配置
     * 
     * @param userId 用户 ID
     * @return AI 配置 DTO
     */
    @Transactional(readOnly = true)
    public AiConfigDto getAiConfig(UUID userId) {
        return aiConfigRepository.findByUserId(userId)
                .map(this::toAiConfigDto)
                .orElse(AiConfigDto.createDefault());
    }

    /**
     * 更新用户 AI 配置
     * 如果配置不存在则创建新配置
     * 
     * @param userId 用户 ID
     * @param request AI 配置请求
     * @return 更新后的 AI 配置
     * @throws BusinessException 当配置值无效时抛出
     */
    @Transactional
    public AiConfigDto updateAiConfig(UUID userId, AiConfigDto request) {
        // 验证配置值
        validateAiConfig(request);

        UserAiConfig config = aiConfigRepository.findByUserId(userId)
                .orElseGet(() -> createNewConfig(userId));

        // 更新配置
        config.setLanguage(request.getLanguage());
        config.setResponseStyle(request.getResponseStyle());
        
        if (request.getInterviewDifficulty() != null) {
            config.setInterviewDifficulty(request.getInterviewDifficulty());
        }
        
        config.setFocusAreas(toJsonArray(request.getFocusAreas()));
        config.setCustomInstructions(request.getCustomInstructions());

        config = aiConfigRepository.save(config);
        
        log.info("用户 {} 更新了 AI 配置", userId);
        return toAiConfigDto(config);
    }

    /**
     * 删除用户 AI 配置（重置为默认）
     * 
     * @param userId 用户 ID
     */
    @Transactional
    public void deleteAiConfig(UUID userId) {
        aiConfigRepository.deleteByUserId(userId);
        log.info("用户 {} 删除了 AI 配置", userId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证 AI 配置的有效性
     */
    private void validateAiConfig(AiConfigDto request) {
        if (!VALID_LANGUAGES.contains(request.getLanguage())) {
            throw new BusinessException(ErrorCode.INVALID_LANGUAGE, 
                    "无效的语言选项，有效值: " + VALID_LANGUAGES);
        }

        if (!VALID_RESPONSE_STYLES.contains(request.getResponseStyle())) {
            throw new BusinessException(ErrorCode.INVALID_RESPONSE_STYLE, 
                    "无效的响应风格，有效值: " + VALID_RESPONSE_STYLES);
        }

        if (request.getInterviewDifficulty() != null 
                && !VALID_DIFFICULTIES.contains(request.getInterviewDifficulty())) {
            throw new BusinessException(ErrorCode.INVALID_DIFFICULTY, 
                    "无效的面试难度，有效值: " + VALID_DIFFICULTIES);
        }
    }

    /**
     * 为用户创建新的 AI 配置
     */
    private UserAiConfig createNewConfig(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserAiConfig.builder()
                .user(user)
                .language("zh")
                .responseStyle("balanced")
                .interviewDifficulty("medium")
                .build();
    }

    /**
     * 将实体转换为 DTO
     */
    private AiConfigDto toAiConfigDto(UserAiConfig config) {
        return AiConfigDto.builder()
                .language(config.getLanguage())
                .responseStyle(config.getResponseStyle())
                .interviewDifficulty(config.getInterviewDifficulty())
                .focusAreas(parseJsonArray(config.getFocusAreas()))
                .customInstructions(config.getCustomInstructions())
                .build();
    }

    /**
     * 将列表转换为 JSON 字符串
     */
    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("序列化 JSON 数组失败", e);
            return null;
        }
    }

    /**
     * 将 JSON 字符串解析为列表
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析 JSON 数组失败", e);
            return Collections.emptyList();
        }
    }
}
