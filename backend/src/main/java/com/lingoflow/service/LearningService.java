package com.lingoflow.service;

import com.lingoflow.entity.LearningSession;
import com.lingoflow.entity.SessionWord;
import com.lingoflow.entity.Vocabulary;
import com.lingoflow.exception.BusinessException;
import com.lingoflow.mapper.LearningSessionMapper;
import com.lingoflow.mapper.SessionWordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LearningService {

    private final LearningSessionMapper learningSessionMapper;
    private final SessionWordMapper sessionWordMapper;
    private final VocabularyService vocabularyService;
    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

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

                // 保存造句记录
                SessionWord sessionWord = new SessionWord();
                sessionWord.setSessionId(sessionId);
                sessionWord.setVocabularyId(vocabularyId);
                sessionWord.setActionType("sentence");
                sessionWord.setUserSentence(sentence);
                sessionWord.setAiFeedback(data.toString());
                sessionWord.setScore((Integer) data.get("score"));
                sessionWordMapper.insert(sessionWord);

                return data;
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
