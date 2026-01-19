package com.lingoflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 单词-词典关联实体类
 * Phase 6: 词典系统
 */
@Data
public class WordDictionaryTag {
    private Long id;
    private Long wordId;
    private Long dictionaryId;
    private LocalDateTime createdAt;
}
