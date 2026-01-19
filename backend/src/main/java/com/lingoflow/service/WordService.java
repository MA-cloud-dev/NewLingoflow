package com.lingoflow.service;

import com.lingoflow.entity.Word;
import com.lingoflow.mapper.WordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WordService {

    private final WordMapper wordMapper;

    public List<Word> getAllWords() {
        return wordMapper.findAll();
    }

    public Word getWordById(Long id) {
        return wordMapper.findById(id);
    }

    public List<Word> getWordsByDifficulty(String difficulty) {
        return wordMapper.findByDifficulty(difficulty);
    }

    public List<Word> getWordsForLearning(Long userId, String difficulty, int count) {
        return wordMapper.findWordsNotInUserVocabulary(userId, difficulty, count);
    }

    public int getTotalCount() {
        return wordMapper.count();
    }
}
