## 数据库扩展 - 词典系统

### 新增表: dictionaries

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 词典ID |
| name | VARCHAR(50) | UNIQUE, NOT NULL | 词典名称 (四级/六级/雅思/托福等) |
| description | TEXT | NULL | 词典描述 |
| total_words | INT | DEFAULT 0 | 总词数 |
| created_at | DATETIME | DEFAULT NOW() | 创建时间 |

**索引**: `uk_name` (name)

### 新增表: word_dictionary_tags

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 标签ID |
| word_id | BIGINT | FK -> words.id | 单词ID |
| dictionary_id | BIGINT | FK -> dictionaries.id | 词典ID |
| created_at | DATETIME | DEFAULT NOW() | 创建时间 |

**索引**: `uk_word_dict` (word_id, dictionary_id)

---

## 数据库扩展 - 学习定制

### 修改表: learning_sessions

新增字段:

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| theme | VARCHAR(50) | NULL | 学习主题 (科幻/文化等) |
| difficulty_preference | VARCHAR(20) | NULL | 难度偏好 (高/中/低) |
| target_word_count | INT | DEFAULT 5 | 目标词汇数量 (5或10) |

---

## 数据库扩展 - 阅读理解题目

### 修改表: session_words

新增字段:

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| question_type | VARCHAR(30) | DEFAULT 'sentence_making' | 题目类型 |
| question_data | JSON | NULL | 题目数据 (选项、答案、解析) |

**question_type 枚举值**:
- `sentence_making`: 造句题
- `word_comprehension`: 词汇理解题
- `main_idea`: 主旨题

**question_data JSON 示例** (词汇理解题):
```json
{
  "question": "In the context of the article, what does 'innovation' mean?",
  "options": ["A. 传统方法", "B. 创新", "C. 保守思想", "D. 旧观念"],
  "correct_answer": "B",
  "explanation": "创新（innovation）在文中指的是新的想法或方法。"
}
```

---

## API 扩展 - 词典模块

### GET /api/dictionaries

**描述**: 获取所有可用词典列表  
**关联用户地图**: [UM-08: 词典选择流]

**Request**: 无

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "CET-4",
      "description": "大学英语四级词汇",
      "totalWords": 4500
    },
    {
      "id": 2,
      "name": "CET-6",
      "description": "大学英语六级词汇",
      "totalWords": 6000
    },
    {
      "id": 3,
      "name": "IELTS",
      "description": "雅思词汇",
      "totalWords": 8000
    },
    {
      "id": 4,
      "name": "TOEFL",
      "description": "托福词汇",
      "totalWords": 10000
    }
  ]
}
```

### GET /api/dictionaries/{id}/progress

**描述**: 获取用户在特定词典的学习进度  
**关联用户地图**: [UM-08: 词典选择流]

**Request Parameters**:
- `id`: 词典ID

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dictionaryId": 1,
    "dictionaryName": "CET-4",
    "totalWords": 4500,
    "learnedWords": 230,
    "progress": 5.11
  }
}
```

---

## API 扩展 - 学习模块

### POST /api/learning/sessions

**描述**: 创建学习会话，支持定制参数  
**关联用户地图**: [UM-09: 学习定制流]

**Request**:
```json
{
  "dictionaryId": 1,
  "theme": "科幻",
  "difficulty": "中",
  "targetWordCount": 5
}
```

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "sessionId": 123,
    "words": [...],
    "article": "...",
    "translation": "...",
    "comprehensionQuestions": [
      {
        "type": "word_comprehension",
        "word": "innovation",
        "question": "In the context of the article, what does 'innovation' mean?",
        "options": ["A. 传统方法", "B. 创新", "C. 保守思想", "D. 旧观念"],
        "correctAnswer": "B",
        "explanation": "创新（innovation）在文中指的是新的想法或方法。"
      },
      {
        "type": "main_idea",
        "question": "What is the main idea of the article?",
        "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
        "correctAnswer": "C",
        "explanation": "..."
      }
    ],
    "sentenceMakingTasks": [
      {
        "word": "innovation",
        "theme": "科技发展",
        "chineseExample": "这家公司通过创新引领了行业变革。"
      }
    ]
  }
}
```

### POST /api/tts/speak

**描述**: 文本转语音接口  
**关联用户地图**: [UM-10: 增强阅读体验流]

**Request**:
```json
{
  "text": "innovation",
  "language": "en"
}
```

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "audioUrl": "https://cdn.example.com/tts/abc123.mp3"
  }
}
```

---

## Ref: Phase 6 - 词典系统

数据库设计参考: [dictionaries 表](#新增表-dictionaries), [word_dictionary_tags 表](#新增表-word_dictionary_tags)

API设计参考: [GET /api/dictionaries](#get-apidictionaries), [GET /api/dictionaries/{id}/progress](#get-apidictionariesidprogress)

## Ref: Phase 7 - 学习定制与文章渲染增强

数据库设计参考: [learning_sessions 表修改](#修改表-learning_sessions)

API设计参考: [POST /api/learning/sessions](#post-apilearningsessions), [POST /api/tts/speak](#post-apittsspeak)

## Ref: Phase 8 - 阅读理解与造句改进

数据库设计参考: [session_words 表修改](#修改表-session_words)

API设计参考: [POST /api/learning/sessions](#post-apilearningsessions) (包含阅读理解题目和改进的造句题)
