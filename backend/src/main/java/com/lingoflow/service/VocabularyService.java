package com.lingoflow.service;

import com.lingoflow.entity.Vocabulary;
import com.lingoflow.exception.BusinessException;
import com.lingoflow.mapper.VocabularyMapper;
import com.lingoflow.mapper.WordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VocabularyService {

    private final VocabularyMapper vocabularyMapper;
    private final WordMapper wordMapper;

    public Map<String, Object> addToVocabulary(Long userId, Long wordId) {
        // 检查单词是否存在
        if (wordMapper.findById(wordId) == null) {
            throw new BusinessException(2002, "单词不存在");
        }

        // 检查是否已在生词本中
        if (vocabularyMapper.existsByUserIdAndWordId(userId, wordId)) {
            throw new BusinessException(2001, "单词已在生词本中");
        }

        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setUserId(userId);
        vocabulary.setWordId(wordId);
        vocabulary.setFamiliarity(0);

        vocabularyMapper.insert(vocabulary);

        Map<String, Object> result = new HashMap<>();
        result.put("vocabularyId", vocabulary.getId());
        return result;
    }

    public List<Vocabulary> batchAddOrGet(Long userId, List<Long> wordIds) {
        // 1. 遍历尝试添加（如果不存在）
        for (Long wordId : wordIds) {
            if (wordMapper.findById(wordId) != null && !vocabularyMapper.existsByUserIdAndWordId(userId, wordId)) {
                Vocabulary vocabulary = new Vocabulary();
                vocabulary.setUserId(userId);
                vocabulary.setWordId(wordId);
                vocabulary.setFamiliarity(0);
                vocabularyMapper.insert(vocabulary);
            }
        }

        // 2. 批量获取并返回
        return vocabularyMapper.findByUserIdAndWordIds(userId, wordIds);
    }

    public List<Vocabulary> getUserVocabulary(Long userId, String status, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return vocabularyMapper.findByUserId(userId, status, offset, pageSize);
    }

    public int getUserVocabularyCount(Long userId, String status) {
        return vocabularyMapper.countByUserId(userId, status);
    }

    public void removeFromVocabulary(Long userId, Long vocabularyId) {
        int deleted = vocabularyMapper.deleteByUserIdAndId(userId, vocabularyId);
        if (deleted == 0) {
            throw new BusinessException(2003, "生词本记录不存在");
        }
    }

    public Vocabulary getVocabularyById(Long id) {
        return vocabularyMapper.findById(id);
    }

    public List<Vocabulary> getVocabularyByIds(List<Long> ids) {
        return vocabularyMapper.findByIds(ids);
    }
}
