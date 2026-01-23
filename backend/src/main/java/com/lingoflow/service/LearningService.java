package com.lingoflow.service;

import com.lingoflow.entity.LearningSession;
import com.lingoflow.entity.SessionWord;
import com.lingoflow.entity.Vocabulary;
import com.lingoflow.exception.BusinessException;
import com.lingoflow.mapper.LearningSessionMapper;
import com.lingoflow.mapper.SessionWordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LearningService {

    private final LearningSessionMapper learningSessionMapper;
    private final SessionWordMapper sessionWordMapper;
    private final VocabularyService vocabularyService;
    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final WordService wordService;

    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    private static final String STUDY_STATE_KEY_PREFIX = "learning:study:";
    private static final Duration STUDY_STATE_TTL = Duration.ofHours(1);

    /**
     * 保存学习阶段状态到 Redis
     */
    public void saveLearningState(Long userId, Map<String, Object> state) {
        String key = STUDY_STATE_KEY_PREFIX + userId;
        try {
            String json = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(key, json, STUDY_STATE_TTL);
        } catch (Exception e) {
            // 缓存失败不影响主流程
        }
    }

    /**
     * 从 Redis 获取学习阶段状态
     */
    public Map<String, Object> getLearningState(Long userId) {
        String key = STUDY_STATE_KEY_PREFIX + userId;
        String cachedValue = redisTemplate.opsForValue().get(key);

        if (cachedValue != null) {
            try {
                return objectMapper.readValue(cachedValue, new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception e) {
                redisTemplate.delete(key);
            }
        }
        return null;
    }

    /**
     * 清除学习阶段状态
     */
    public void clearLearningState(Long userId) {
        String key = STUDY_STATE_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        wordService.clearLearningProgress(userId);
    }

    public Map<String, Object> generateArticle(Long userId, List<Long> vocabularyIds,
            String difficulty, String length, String theme) {
        if (vocabularyIds == null || vocabularyIds.size() < 1) {
            throw new BusinessException(2010, "至少需要选择 1 个单词");
        }

        // 获取生词本中的单词
        List<Vocabulary> vocabularyList = vocabularyService.getVocabularyByIds(vocabularyIds);
        if (vocabularyList.isEmpty()) {
            throw new BusinessException(2010, "生词不存在");
        }

        List<Map<String, String>> words = new ArrayList<>();
        for (Vocabulary v : vocabularyList) {
            if (v.getWord() != null) {
                Map<String, String> wordMap = new HashMap<>();
                wordMap.put("word", v.getWord().getWord());
                wordMap.put("meaningCn", v.getWord().getMeaningCn());
                words.add(wordMap);
            }
        }

        // 调用 Flask AI 服务
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("words", words);
        requestBody.put("difficulty", difficulty != null ? difficulty : "medium");
        requestBody.put("length", length != null ? length : "short");
        if (theme != null && !theme.isEmpty()) {
            requestBody.put("theme", theme);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServiceUrl + "/api/generate-article",
                    entity,
                    Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && (Integer) responseBody.get("code") == 200) {
                // 清除选词进度
                wordService.clearLearningProgress(userId);

                // 创建学习会话
                LearningSession session = new LearningSession();
                session.setUserId(userId);
                session.setSessionType("article");
                session.setAiArticle(responseBody.get("data").toString());
                session.setTheme(theme);
                session.setDifficultyPreference(difficulty);
                session.setTargetWordCount(vocabularyIds.size());
                learningSessionMapper.insert(session);

                Map<String, Object> result = new HashMap<>();
                result.put("sessionId", session.getId());
                result.put("article", responseBody.get("data"));
                return result;
            } else {
                throw new BusinessException(2011, "AI 服务返回错误");
            }
        } catch (Exception e) {
            throw new BusinessException(2011, "AI 服务暂时不可用: " + e.getMessage());
        }
    }

    public Map<String, Object> submitSentence(Long userId, Long sessionId, Long vocabularyId, String sentence) {
        if (sentence == null || sentence.trim().isEmpty()) {
            throw new BusinessException(2020, "句子不能为空");
        }

        Vocabulary vocabulary = vocabularyService.getVocabularyById(vocabularyId);
        if (vocabulary == null || vocabulary.getWord() == null) {
            throw new BusinessException(2003, "生词记录不存在");
        }

        String targetWord = vocabulary.getWord().getWord();
        Long wordId = vocabulary.getWord().getId();

        if (!sentence.toLowerCase().contains(targetWord.toLowerCase())) {
            throw new BusinessException(2021, "句子中未包含目标单词 '" + targetWord + "'");
        }

        // 调用 Flask AI 服务
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("word", targetWord);
        requestBody.put("meaning", vocabulary.getWord().getMeaningCn());
        requestBody.put("sentence", sentence);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServiceUrl + "/api/evaluate-sentence",
                    entity,
                    Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && (Integer) responseBody.get("code") == 200) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                Integer newScore = (Integer) data.get("score");

                // 查询用户对该单词是否已有造句记录
                SessionWord existingSentence = sessionWordMapper.findSentenceByUserIdAndWordId(userId, wordId);

                Map<String, Object> result = new HashMap<>(data);

                if (existingSentence != null) {
                    // 已有造句记录，只有新分数 >= 旧分数才替换
                    if (newScore >= existingSentence.getScore()) {
                        // 删除旧记录
                        sessionWordMapper.deleteById(existingSentence.getId());

                        // 保存新记录
                        SessionWord sessionWord = new SessionWord();
                        sessionWord.setSessionId(sessionId);
                        sessionWord.setVocabularyId(vocabularyId);
                        sessionWord.setActionType("sentence");
                        sessionWord.setUserSentence(sentence);
                        sessionWord.setAiFeedback(data.toString());
                        sessionWord.setScore(newScore);
                        sessionWordMapper.insert(sessionWord);

                        result.put("replaced", true);
                        result.put("previousScore", existingSentence.getScore());
                        result.put("message", "造句已更新（此前最高分: " + existingSentence.getScore() + "）");
                    } else {
                        // 新分数低于旧分数，不保存，返回旧记录信息
                        result.put("replaced", false);
                        result.put("previousScore", existingSentence.getScore());
                        result.put("message", "此前造句得分更高（" + existingSentence.getScore() + " > " + newScore + "），保留原记录");
                    }
                } else {
                    // 没有旧记录，直接保存
                    SessionWord sessionWord = new SessionWord();
                    sessionWord.setSessionId(sessionId);
                    sessionWord.setVocabularyId(vocabularyId);
                    sessionWord.setActionType("sentence");
                    sessionWord.setUserSentence(sentence);
                    sessionWord.setAiFeedback(data.toString());
                    sessionWord.setScore(newScore);
                    sessionWordMapper.insert(sessionWord);

                    result.put("replaced", false);
                    result.put("message", "造句已保存");
                }

                return result;
            } else {
                throw new BusinessException(2011, "AI 服务返回错误");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(2011, "AI 服务暂时不可用: " + e.getMessage());
        }
    }

    public LearningSession getSessionById(Long id) {
        return learningSessionMapper.findById(id);
    }
}
