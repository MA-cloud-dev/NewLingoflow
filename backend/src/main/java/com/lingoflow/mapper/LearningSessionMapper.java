package com.lingoflow.mapper;

import com.lingoflow.entity.LearningSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LearningSessionMapper {

    LearningSession findById(@Param("id") Long id);

    List<LearningSession> findByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    int insert(LearningSession session);

    int update(LearningSession session);

    int endSession(@Param("id") Long id, @Param("wordsLearned") int wordsLearned,
            @Param("wordsCorrect") int wordsCorrect, @Param("durationSeconds") int durationSeconds);
}
