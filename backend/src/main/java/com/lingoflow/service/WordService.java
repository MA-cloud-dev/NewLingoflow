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
                com.lingoflow.dto.LearningStateDto cachedState = objectMapper.readValue(cachedValue,
                        com.lingoflow.dto.LearningStateDto.class);
                // 验证缓存数据有效性：如果词汇列表不为空，则返回缓存
                if (cachedState.getWords() != null && !cachedState.getWords().isEmpty()) {
                    return cachedState;
                }
                // 缓存数据无效（空词汇），删除缓存并重新从数据库加载
                redisTemplate.delete(key);
            } catch (Exception e) {
                // 缓存反序列化失败，删除脏数据
                redisTemplate.delete(key);
            }
        }

        List<Word> words = wordMapper.findWordsNotInUserVocabulary(userId, difficulty, count);

        com.lingoflow.dto.LearningStateDto state = new com.lingoflow.dto.LearningStateDto();
        state.setWords(words != null ? words : new java.util.ArrayList<>());
        state.setCurrentIndex(0);
        state.setSelectedWords(new java.util.ArrayList<>());

        // 只有在有单词时才缓存，避免缓存空数据
        if (words != null && !words.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(state);
                redisTemplate.opsForValue().set(key, json, java.time.Duration.ofHours(1));
            } catch (Exception e) {
                // 忽略缓存错误，不影响主流程
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
