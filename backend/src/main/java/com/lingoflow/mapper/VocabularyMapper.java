package com.lingoflow.mapper;

import com.lingoflow.entity.Vocabulary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VocabularyMapper {

    Vocabulary findById(@Param("id") Long id);

    Vocabulary findByUserIdAndWordId(@Param("userId") Long userId, @Param("wordId") Long wordId);

    List<Vocabulary> findByUserId(@Param("userId") Long userId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    List<Vocabulary> findByIds(@Param("ids") List<Long> ids);

    int countByUserId(@Param("userId") Long userId, @Param("status") String status);

    boolean existsByUserIdAndWordId(@Param("userId") Long userId, @Param("wordId") Long wordId);

    int insert(Vocabulary vocabulary);

    int update(Vocabulary vocabulary);

    int deleteById(@Param("id") Long id);

    int deleteByUserIdAndId(@Param("userId") Long userId, @Param("id") Long id);
}
