-- ========================================
-- Phase 7: 学习定制数据库迁移
-- ========================================

-- 修改 learning_sessions 表，增加定制字段
ALTER TABLE learning_sessions
ADD COLUMN theme VARCHAR(50) DEFAULT NULL COMMENT '学习主题',
ADD COLUMN difficulty_preference VARCHAR(20) DEFAULT NULL COMMENT '难度偏好',
ADD COLUMN target_word_count INT DEFAULT 5 COMMENT '目标词汇数量';