package com.lingoflow.dto;

import com.lingoflow.entity.Word;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class LearningStateDto {
    private List<Word> words = new ArrayList<>();
    private int currentIndex = 0;
    private List<Word> selectedWords = new ArrayList<>();
    private long timestamp = System.currentTimeMillis();
}
