package com.lingoflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SessionWord {
    private Long id;
    private Long sessionId;
    private Long vocabularyId;
    private String actionType;
    private String userSentence;
    private String aiFeedback;
    private Integer score;
    private String questionType;
    private String questionData;
    private LocalDateTime createdAt;
}
