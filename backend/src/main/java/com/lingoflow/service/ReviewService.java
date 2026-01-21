package com.lingoflow.service;

import com.lingoflow.entity.ReviewRecord;
import com.lingoflow.entity.Vocabulary;
import com.lingoflow.entity.Word;
import com.lingoflow.exception.BusinessException;
import com.lingoflow.mapper.ReviewRecordMapper;
import com.lingoflow.mapper.VocabularyMapper;
import com.lingoflow.mapper.WordMapper;
import com.lingoflow.util.SM2Algorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final VocabularyMapper vocabularyMapper;
    private final WordMapper wordMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * 获取今日待复习队列
     */
    public Map<String, Object> getReviewQueue(Long userId) {
        String key = "review:queue:" + userId + ":" + java.time.LocalDate.now();
        String cachedValue = redisTemplate.opsForValue().get(key);

        if (cachedValue != null) {
            try {
                return objectMapper.readValue(cachedValue,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });
            } catch (Exception e) {
                // Ignore cache error
            }
        }

        List<Vocabulary> vocabulary = vocabularyMapper.findByUserId(userId, "review", 0, 100);

        List<Map<String, Object>> words = new ArrayList<>();
        for (Vocabulary v : vocabulary) {
            if (v.getWord() != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("vocabularyId", v.getId());
                item.put("word", v.getWord().getWord());
                item.put("phonetic", v.getWord().getPhonetic());
                item.put("meaningCn", v.getWord().getMeaningCn());
                item.put("familiarity", v.getFamiliarity());
                item.put("reviewCount", v.getReviewCount());
                words.add(item);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("words", words);
        result.put("total", words.size());

        if (!words.isEmpty()) {
            try {
                // Cache for 24 hours, but we will invalidate on update
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result),
                        java.time.Duration.ofHours(24));
            } catch (Exception e) {
                // Ignore
            }
        }

        return result;
    }

    /**
     * 提交熟悉度自评
     * 
     * @param rating "known" 或 "unknown"
     */
    public Map<String, Object> submitRating(Long userId, Long vocabularyId, String rating) {
        Vocabulary vocabulary = vocabularyMapper.findById(vocabularyId);
        if (vocabulary == null || !vocabulary.getUserId().equals(userId)) {
            throw new BusinessException(3001, "生词本记录不存在");
        }

        // 记录评价
        ReviewRecord record = new ReviewRecord();
        record.setUserId(userId);
        record.setVocabularyId(vocabularyId);
        record.setRating(rating);
        reviewRecordMapper.insert(record);

        Map<String, Object> result = new HashMap<>();

        if ("unknown".equals(rating)) {
            // 不认识：直接显示答案，SM-2 按 quality=0 处理
            applySM2(vocabulary, 0);

            result.put("needTest", false);
            result.put("correctAnswer", vocabulary.getWord().getMeaningCn());
            result.put("word", vocabulary.getWord().getWord());
        } else if ("fuzzy".equals(rating)) {
            // 模糊：直接显示答案，SM-2 按 quality=1 处理
            applySM2(vocabulary, 1);

            result.put("needTest", false);
            result.put("correctAnswer", vocabulary.getWord().getMeaningCn());
            result.put("word", vocabulary.getWord().getWord());
        } else {
            // 认识：返回四选一题目
            result.put("needTest", true);
            result.put("testQuestion", generateTestQuestion(vocabulary));
        }

        // Invalidate cache
        String key = "review:queue:" + userId + ":" + java.time.LocalDate.now();
        redisTemplate.delete(key);

        return result;
    }

    /**
     * 提交测试答案
     * 
     * @param isFromErrorQueue 是否来自错误队列
     */
    public Map<String, Object> submitAnswer(Long userId, Long vocabularyId, String answer,
            boolean isFromErrorQueue, Integer responseTimeMs) {
        Vocabulary vocabulary = vocabularyMapper.findById(vocabularyId);
        if (vocabulary == null || !vocabulary.getUserId().equals(userId)) {
            throw new BusinessException(3001, "生词本记录不存在");
        }

        String correctAnswer = vocabulary.getWord().getMeaningCn();
        boolean isCorrect = correctAnswer.equals(answer);

        Map<String, Object> result = new HashMap<>();
        result.put("isCorrect", isCorrect);
        result.put("correctAnswer", correctAnswer);

        // 只有非错误队列的答题才影响 SM-2
        if (!isFromErrorQueue) {
            if (isCorrect) {
                // 认识+正确：quality=5
                applySM2(vocabulary, 5);
            } else {
                // 认识+错误：quality=0
                applySM2(vocabulary, 0);
            }

            result.put("nextReviewDate", vocabulary.getNextReviewDate());
            result.put("newFamiliarity", vocabulary.getFamiliarity());
        }

        // 更新复习记录
        List<ReviewRecord> records = reviewRecordMapper.findByUserIdAndVocabularyId(userId, vocabularyId);
        if (!records.isEmpty()) {
            ReviewRecord record = records.get(0);
            record.setTestPassed(isCorrect);
            record.setResponseTimeMs(responseTimeMs);
            reviewRecordMapper.update(record);
        }

        // Invalidate cache if review status changed (SM-2 applied)
        if (!isFromErrorQueue) {
            String key = "review:queue:" + userId + ":" + java.time.LocalDate.now();
            redisTemplate.delete(key);
        }

        return result;
    }

    /**
     * 应用 SM-2 算法更新词汇
     */
    private void applySM2(Vocabulary vocabulary, int quality) {
        SM2Algorithm.SM2Result sm2Result = SM2Algorithm.calculate(
                vocabulary.getIntervalDays() != null ? vocabulary.getIntervalDays() : 0,
                vocabulary.getEasinessFactor() != null ? vocabulary.getEasinessFactor() : 2.5f,
                quality);

        vocabulary.setIntervalDays(sm2Result.getIntervalDays());
        vocabulary.setEasinessFactor(sm2Result.getEasinessFactor());
        vocabulary.setNextReviewDate(LocalDateTime.now().plusDays(sm2Result.getIntervalDays()));
        vocabulary.setLastReviewDate(LocalDateTime.now());
        vocabulary.setReviewCount((vocabulary.getReviewCount() != null ? vocabulary.getReviewCount() : 0) + 1);

        // 更新熟悉度
        int currentFamiliarity = vocabulary.getFamiliarity() != null ? vocabulary.getFamiliarity() : 0;
        int newFamiliarity = Math.max(0, Math.min(100, currentFamiliarity + sm2Result.getFamiliarityDelta()));
        vocabulary.setFamiliarity(newFamiliarity);

        vocabularyMapper.update(vocabulary);
    }

    /**
     * 生成四选一测试题
     */
    private Map<String, Object> generateTestQuestion(Vocabulary vocabulary) {
        Word targetWord = vocabulary.getWord();
        String correctAnswer = targetWord.getMeaningCn();

        // 获取干扰项
        List<Word> allWords = wordMapper.findAll();
        List<String> distractors = allWords.stream()
                .filter(w -> !w.getId().equals(targetWord.getId()))
                .map(Word::getMeaningCn)
                .distinct()
                .collect(Collectors.toList());

        Collections.shuffle(distractors);
        List<String> options = new ArrayList<>();
        options.add(correctAnswer);
        options.addAll(distractors.subList(0, Math.min(3, distractors.size())));
        Collections.shuffle(options);

        Map<String, Object> question = new HashMap<>();
        question.put("type", "choice");
        question.put("word", targetWord.getWord());
        question.put("phonetic", targetWord.getPhonetic());
        question.put("options", options);
        question.put("correctAnswer", correctAnswer);

        return question;
    }
}
