package com.lingoflow.mapper;

import com.lingoflow.entity.ReviewRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewRecordMapper {

    ReviewRecord findById(@Param("id") Long id);

    List<ReviewRecord> findByUserIdAndVocabularyId(@Param("userId") Long userId,
            @Param("vocabularyId") Long vocabularyId);

    int insert(ReviewRecord record);

    int update(ReviewRecord record);
}
