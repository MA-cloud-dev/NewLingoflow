-- LingoFlow 数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS newlingoflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE newlingoflow;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(255),
    daily_goal INT DEFAULT 20,
    difficulty_level VARCHAR(20) DEFAULT 'medium',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 词库表
CREATE TABLE IF NOT EXISTS words (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    word VARCHAR(100) NOT NULL UNIQUE,
    phonetic VARCHAR(100),
    meaning_cn TEXT NOT NULL,
    meaning_en TEXT,
    example_sentence TEXT,
    difficulty VARCHAR(20) DEFAULT 'medium',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_word (word),
    INDEX idx_difficulty (difficulty)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 用户生词本
CREATE TABLE IF NOT EXISTS vocabulary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    familiarity INT DEFAULT 0,
    review_count INT DEFAULT 0,
    easiness_factor FLOAT DEFAULT 2.5,
    interval_days INT DEFAULT 1,
    next_review_date DATETIME,
    last_review_date DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (word_id) REFERENCES words (id) ON DELETE CASCADE,
    UNIQUE KEY idx_user_word (user_id, word_id),
    INDEX idx_next_review (user_id, next_review_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 学习会话表
CREATE TABLE IF NOT EXISTS learning_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_type VARCHAR(20) NOT NULL,
    ai_article TEXT,
    words_learned INT DEFAULT 0,
    words_correct INT DEFAULT 0,
    duration_seconds INT DEFAULT 0,
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_user_session (user_id, started_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 会话单词记录表
CREATE TABLE IF NOT EXISTS session_words (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    vocabulary_id BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    user_sentence TEXT,
    ai_feedback TEXT,
    score INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES learning_sessions (id) ON DELETE CASCADE,
    FOREIGN KEY (vocabulary_id) REFERENCES vocabulary (id) ON DELETE CASCADE,
    INDEX idx_session (session_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 复习记录表
CREATE TABLE IF NOT EXISTS review_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    vocabulary_id BIGINT NOT NULL,
    familiarity_rating VARCHAR(20) NOT NULL,
    is_correct BOOLEAN,
    response_time_ms INT,
    reviewed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (vocabulary_id) REFERENCES vocabulary (id) ON DELETE CASCADE,
    INDEX idx_user_review (user_id, reviewed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ========================================
-- Phase 6: 词典系统
-- ========================================

-- 词典表
CREATE TABLE IF NOT EXISTS dictionaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '词典ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '词典名称',
    description TEXT COMMENT '词典描述',
    total_words INT DEFAULT 0 COMMENT '总词数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 单词-词典关联表
CREATE TABLE IF NOT EXISTS word_dictionary_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    word_id BIGINT NOT NULL COMMENT '单词ID',
    dictionary_id BIGINT NOT NULL COMMENT '词典ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (word_id) REFERENCES words (id) ON DELETE CASCADE,
    FOREIGN KEY (dictionary_id) REFERENCES dictionaries (id) ON DELETE CASCADE,
    UNIQUE KEY uk_word_dict (word_id, dictionary_id),
    INDEX idx_word_id (word_id),
    INDEX idx_dictionary_id (dictionary_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;