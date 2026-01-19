package com.lingoflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 词典实体类
 * Phase 6: 词典系统
 */
@Data
public class Dictionary {
    private Long id;
    private String name;
    private String description;
    private Integer totalWords;
    private LocalDateTime createdAt;
}
