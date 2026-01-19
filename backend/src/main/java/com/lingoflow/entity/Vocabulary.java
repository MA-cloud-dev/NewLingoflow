package com.lingoflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Vocabulary {
    private Long id;
    private Long userId;
    private Long wordId;
    private Integer familiarity;
    private Integer reviewCount;
    private Float easinessFactor;
    private Integer intervalDays;
    private LocalDateTime nextReviewDate;
    private LocalDateTime lastReviewDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 关联的单词信息（用于联表查询）
    private Word word;
}
