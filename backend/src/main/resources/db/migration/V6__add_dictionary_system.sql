-- ========================================
-- Phase 6: 词典系统数据库迁移
-- ========================================

-- 词典表
CREATE TABLE IF NOT EXISTS dictionaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '词典ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '词典名称',
    description TEXT COMMENT '词典描述',
    total_words INT DEFAULT 0 COMMENT '总词数',
    created_at DATETIME DEFAULT NOW() COMMENT '创建时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '词典表';

-- 单词-词典关联表
CREATE TABLE IF NOT EXISTS word_dictionary_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    word_id BIGINT NOT NULL COMMENT '单词ID',
    dictionary_id BIGINT NOT NULL COMMENT '词典ID',
    created_at DATETIME DEFAULT NOW() COMMENT '创建时间',
    FOREIGN KEY (word_id) REFERENCES words (id) ON DELETE CASCADE,
    FOREIGN KEY (dictionary_id) REFERENCES dictionaries (id) ON DELETE CASCADE,
    UNIQUE KEY uk_word_dict (word_id, dictionary_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '单词-词典关联表';

-- 创建索引
CREATE INDEX idx_word_id ON word_dictionary_tags (word_id);

CREATE INDEX idx_dictionary_id ON word_dictionary_tags (dictionary_id);

-- 插入初始词典数据
INSERT INTO
    dictionaries (
        name,
        description,
        total_words
    )
VALUES ('CET-4', '大学英语四级词汇', 4500),
    ('CET-6', '大学英语六级词汇', 6000),
    ('IELTS', '雅思词汇', 8000),
    ('TOEFL', '托福词汇', 10000)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    total_words = VALUES(total_words);