package com.lingoflow.service;

import com.lingoflow.entity.Dictionary;
import com.lingoflow.mapper.DictionaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 词典服务
 * Phase 6: 词典系统
 */
@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final DictionaryMapper dictionaryMapper;

    /**
     * 获取所有词典列表
     */
    public List<Dictionary> getAllDictionaries() {
        return dictionaryMapper.findAll();
    }

    /**
     * 获取用户在特定词典的学习进度
     */
    public Map<String, Object> getUserDictionaryProgress(Long userId, Long dictionaryId) {
        Dictionary dictionary = dictionaryMapper.findById(dictionaryId);
        if (dictionary == null) {
            throw new RuntimeException("词典不存在");
        }

        Integer learnedWords = dictionaryMapper.countLearnedWords(userId, dictionaryId);
        Integer totalWords = dictionary.getTotalWords();

        double progress = totalWords > 0 ? (learnedWords * 100.0 / totalWords) : 0.0;

        Map<String, Object> result = new HashMap<>();
        result.put("dictionaryId", dictionaryId);
        result.put("dictionaryName", dictionary.getName());
        result.put("totalWords", totalWords);
        result.put("learnedWords", learnedWords);
        result.put("progress", Math.round(progress * 100.0) / 100.0);

        return result;
    }
}
