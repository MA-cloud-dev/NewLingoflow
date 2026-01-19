package com.lingoflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewRecord {
    private Long id;
    private Long userId;
    private Long vocabularyId;
    private String rating; // known, unknown
    private Boolean testPassed;
    private Integer responseTimeMs;
    private LocalDateTime createdAt;
}
