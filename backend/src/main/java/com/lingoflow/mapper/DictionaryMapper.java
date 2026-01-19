package com.lingoflow.mapper;

import com.lingoflow.entity.Dictionary;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 词典 Mapper
 * Phase 6: 词典系统
 */
@Mapper
public interface DictionaryMapper {

    /**
     * 获取所有词典列表
     */
    @Select("SELECT * FROM dictionaries ORDER BY id")
    List<Dictionary> findAll();

    /**
     * 根据ID获取词典
     */
    @Select("SELECT * FROM dictionaries WHERE id = #{id}")
    Dictionary findById(Long id);

    /**
     * 根据名称获取词典
     */
    @Select("SELECT * FROM dictionaries WHERE name = #{name}")
    Dictionary findByName(String name);

    /**
     * 获取用户在特定词典的已学单词数
     */
    @Select("SELECT COUNT(DISTINCT v.word_id) " +
            "FROM vocabulary v " +
            "INNER JOIN word_dictionary_tags wdt ON v.word_id = wdt.word_id " +
            "WHERE v.user_id = #{userId} AND wdt.dictionary_id = #{dictionaryId}")
    Integer countLearnedWords(@Param("userId") Long userId, @Param("dictionaryId") Long dictionaryId);
}
