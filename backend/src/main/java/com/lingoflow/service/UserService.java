package com.lingoflow.service;

import com.lingoflow.dto.ChangePasswordRequest;
import com.lingoflow.dto.UpdateProfileRequest;
import com.lingoflow.dto.UserProfileDto;
import com.lingoflow.entity.User;
import com.lingoflow.exception.BusinessException;
import com.lingoflow.mapper.UserMapper;
import com.lingoflow.mapper.VocabularyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final VocabularyMapper vocabularyMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取用户个人信息
     */
    public UserProfileDto getUserProfile(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 获取学习统计 - 用户词汇本总词数
        Integer totalWordsLearned = vocabularyMapper.countByUserId(userId, null);
        Integer streakDays = calculateStreakDays(userId);

        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .dailyGoal(user.getDailyGoal())
                .difficultyLevel(user.getDifficultyLevel())
                .createdAt(user.getCreatedAt())
                .totalWordsLearned(totalWordsLearned != null ? totalWordsLearned : 0)
                .streakDays(streakDays != null ? streakDays : 0)
                .build();
    }

    /**
     * 更新用户个人信息
     */
    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 更新邮箱（检查是否被其他用户使用）
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            User existingUser = userMapper.findByEmail(request.getEmail());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new BusinessException(400, "该邮箱已被其他用户使用");
            }
            user.setEmail(request.getEmail());
        }

        // 更新头像
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // 更新每日目标
        if (request.getDailyGoal() != null) {
            if (request.getDailyGoal() < 1 || request.getDailyGoal() > 100) {
                throw new BusinessException(400, "每日目标需要在1-100之间");
            }
            user.setDailyGoal(request.getDailyGoal());
        }

        // 更新难度等级
        if (request.getDifficultyLevel() != null) {
            if (!isValidDifficultyLevel(request.getDifficultyLevel())) {
                throw new BusinessException(400, "无效的难度等级");
            }
            user.setDifficultyLevel(request.getDifficultyLevel());
        }

        userMapper.update(user);

        return getUserProfile(userId);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 验证新密码与确认密码一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "两次输入的新密码不一致");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 验证当前密码
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(400, "当前密码错误");
        }

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.update(user);
    }

    /**
     * 计算连续学习天数
     */
    private Integer calculateStreakDays(Long userId) {
        // 简单实现：返回0，后续可以根据学习记录计算
        return 0;
    }

    /**
     * 验证难度等级是否有效
     */
    private boolean isValidDifficultyLevel(String level) {
        return "easy".equals(level) || "medium".equals(level) || "hard".equals(level);
    }
}
