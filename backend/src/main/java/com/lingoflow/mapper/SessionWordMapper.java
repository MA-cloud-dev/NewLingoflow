package com.lingoflow.mapper;

import com.lingoflow.entity.SessionWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SessionWordMapper {

    SessionWord findById(@Param("id") Long id);

    List<SessionWord> findBySessionId(@Param("sessionId") Long sessionId);

    int insert(SessionWord sessionWord);

    int update(SessionWord sessionWord);
}
