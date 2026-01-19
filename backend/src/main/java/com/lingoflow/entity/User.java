package com.lingoflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String avatarUrl;
    private Integer dailyGoal;
    private String difficultyLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
