package com.lingoflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private Integer dailyGoal;
    private String difficultyLevel;
    private LocalDateTime createdAt;

    // 统计信息
    private Integer totalWordsLearned;
    private Integer streakDays;
}
