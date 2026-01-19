package com.lingoflow.mapper;

import com.lingoflow.entity.Word;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WordMapper {

    Word findById(@Param("id") Long id);

    List<Word> findAll();

    List<Word> findByDifficulty(@Param("difficulty") String difficulty);

    List<Word> findWordsNotInUserVocabulary(@Param("userId") Long userId,
            @Param("difficulty") String difficulty,
            @Param("limit") int limit);

    List<Word> findByIds(@Param("ids") List<Long> ids);

    int insert(Word word);

    int update(Word word);

    int deleteById(@Param("id") Long id);

    int count();
}
