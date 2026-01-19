package com.lingoflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Word {
    private Long id;
    private String word;
    private String phonetic;
    private String meaningCn;
    private String meaningEn;
    private String exampleSentence;
    private String difficulty;
    private LocalDateTime createdAt;
}
