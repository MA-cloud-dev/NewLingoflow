-- ========================================
-- Phase 8: 阅读理解与造句改进数据库迁移
-- ========================================

-- 修改 session_words 表，支持不同类型的题目
ALTER TABLE session_words
ADD COLUMN question_type VARCHAR(30) DEFAULT 'sentence_making' COMMENT '题目类型',
ADD COLUMN question_data JSON COMMENT '题目数据';

-- question_type 枚举: 'sentence_making', 'word_comprehension', 'main_idea'