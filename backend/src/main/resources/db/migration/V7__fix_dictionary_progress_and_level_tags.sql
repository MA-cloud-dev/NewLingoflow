-- ========================================
-- V7: 修复词典进度和添加词库标签
-- 数据库: newlingoflow
-- 创建时间: 2026-01-20
-- ========================================
--
-- 本脚本解决以下问题：
-- 1. word_dictionary_tags 表无数据导致进度条始终为 0
-- 2. words 表缺少 level_tags 字段导致无法显示词库标签
--
-- 执行方式：
-- mysql -u root -p newlingoflow < V7__fix_dictionary_progress_and_level_tags.sql
-- 或在 MySQL 客户端中执行

-- ===========================================================================
-- 第一部分: 为 words 表添加 level_tags 字段（如果不存在）
-- ===========================================================================

-- 检查并添加 level_tags 字段
-- 注意: MySQL 的 ALTER TABLE 不支持 IF NOT EXISTS，使用存储过程处理
DELIMITER / /

CREATE PROCEDURE add_level_tags_column()
BEGIN
    DECLARE column_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO column_exists 
    FROM information_schema.columns 
    WHERE table_schema = DATABASE() 
      AND table_name = 'words' 
      AND column_name = 'level_tags';
    
    IF column_exists = 0 THEN
        ALTER TABLE words ADD COLUMN level_tags VARCHAR(100) DEFAULT NULL COMMENT '词库级别标签，逗号分隔，如 CET-4,CET-6';
    END IF;
END //

DELIMITER;

-- 执行存储过程
CALL add_level_tags_column ();

-- 删除存储过程（清理）
DROP PROCEDURE IF EXISTS add_level_tags_column;

-- ===========================================================================
-- 第二部分: 填充 word_dictionary_tags 数据
-- ===========================================================================

-- 清空旧的关联数据（如果有冲突）
DELETE FROM word_dictionary_tags;

-- 将所有现有单词关联到 CET-4 词典 (id=1)
INSERT INTO
    word_dictionary_tags (word_id, dictionary_id)
SELECT id, 1
FROM words;

-- 将 medium 和 hard 难度的单词同时关联到 CET-6 词典 (id=2)
INSERT INTO
    word_dictionary_tags (word_id, dictionary_id)
SELECT id, 2
FROM words
WHERE
    difficulty IN ('medium', 'hard');

-- 将 hard 难度的单词同时关联到 IELTS 词典 (id=3)
INSERT INTO
    word_dictionary_tags (word_id, dictionary_id)
SELECT id, 3
FROM words
WHERE
    difficulty = 'hard';

-- ===========================================================================
-- 第三部分: 更新词典的实际单词数量
-- ===========================================================================

UPDATE dictionaries
SET
    total_words = (
        SELECT COUNT(*)
        FROM word_dictionary_tags
        WHERE
            dictionary_id = dictionaries.id
    );

-- ===========================================================================
-- 第四部分: 更新每个单词的 level_tags 字段
-- ===========================================================================

UPDATE words w
SET
    level_tags = (
        SELECT GROUP_CONCAT(
                d.name
                ORDER BY d.id SEPARATOR ','
            )
        FROM
            word_dictionary_tags wdt
            INNER JOIN dictionaries d ON wdt.dictionary_id = d.id
        WHERE
            wdt.word_id = w.id
    )
WHERE
    EXISTS (
        SELECT 1
        FROM word_dictionary_tags
        WHERE
            word_id = w.id
    );

-- ===========================================================================
-- 验证查询（执行后可查看结果确认修复成功）
-- ===========================================================================

-- 验证1: 查看词典关联数量
SELECT d.name AS '词典名称', d.total_words AS '单词数量'
FROM dictionaries d
ORDER BY d.id;

-- 验证2: 查看单词的标签（前10个）
SELECT id, word, difficulty, level_tags FROM words LIMIT 10;

-- 验证3: 查看 word_dictionary_tags 表数据统计
SELECT d.name AS '词典', COUNT(wdt.id) AS '关联单词数'
FROM
    dictionaries d
    LEFT JOIN word_dictionary_tags wdt ON d.id = wdt.dictionary_id
GROUP BY
    d.id,
    d.name
ORDER BY d.id;

-- 完成提示
SELECT '✅ 数据库修复完成！请重启后端服务。' AS '状态';