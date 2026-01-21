package com.lingoflow.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String email;
    private String avatarUrl;
    private Integer dailyGoal;
    private String difficultyLevel;
}
