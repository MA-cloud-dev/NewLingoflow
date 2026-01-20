package com.lingoflow.mapper;

import com.lingoflow.entity.SessionWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SessionWordMapper {

    SessionWord findById(@Param("id") Long id);

    List<SessionWord> findBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 查询用户对某个单词的造句记录（仅限action_type='sentence'）
     * 用于实现造句只保留最高分的逻辑
     */
    SessionWord findSentenceByUserIdAndWordId(@Param("userId") Long userId, @Param("wordId") Long wordId);

    int insert(SessionWord sessionWord);

    int update(SessionWord sessionWord);

    /**
     * 删除造句记录
     */
    int deleteById(@Param("id") Long id);
}
