package com.lingoflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LearningSession {
    private Long id;
    private Long userId;
    private String sessionType;
    private String aiArticle;
    private Integer wordsLearned;
    private Integer wordsCorrect;
    private Integer durationSeconds;
    private String theme;
    private String difficultyPreference;
    private Integer targetWordCount;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
