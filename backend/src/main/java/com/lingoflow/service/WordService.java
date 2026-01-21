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
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public List<Word> getAllWords() {
        return wordMapper.findAll();
    }

    public Word getWordById(Long id) {
        return wordMapper.findById(id);
    }

    public List<Word> getWordsByDifficulty(String difficulty) {
        return wordMapper.findByDifficulty(difficulty);
    }

    public com.lingoflow.dto.LearningStateDto getWordsForLearning(Long userId, String difficulty, int count) {
        String key = "learning:selection:" + userId;
        String cachedValue = redisTemplate.opsForValue().get(key);

        if (cachedValue != null) {
            try {
                return objectMapper.readValue(cachedValue, com.lingoflow.dto.LearningStateDto.class);
            } catch (Exception e) {
                // Ignore cache error and fetch from DB
            }
        }

        List<Word> words = wordMapper.findWordsNotInUserVocabulary(userId, difficulty, count);

        com.lingoflow.dto.LearningStateDto state = new com.lingoflow.dto.LearningStateDto();
        state.setWords(words);
        state.setCurrentIndex(0);
        state.setSelectedWords(new java.util.ArrayList<>());

        if (!words.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(state);
                redisTemplate.opsForValue().set(key, json, java.time.Duration.ofHours(1));
            } catch (Exception e) {
                // Ignore cache error
            }
        }

        return state;
    }

    public void updateLearningProgress(Long userId, int currentIndex, List<Word> selectedWords) {
        String key = "learning:selection:" + userId;
        String cachedValue = redisTemplate.opsForValue().get(key);

        if (cachedValue != null) {
            try {
                com.lingoflow.dto.LearningStateDto state = objectMapper.readValue(cachedValue,
                        com.lingoflow.dto.LearningStateDto.class);
                state.setCurrentIndex(currentIndex);
                state.setSelectedWords(selectedWords);

                String json = objectMapper.writeValueAsString(state);
                redisTemplate.opsForValue().set(key, json, java.time.Duration.ofHours(1));
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    public int getTotalCount() {
        return wordMapper.count();
    }
}
