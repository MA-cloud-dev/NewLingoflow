# LingoFlow 系统设计文档

## 1. 数据模型设计 (Data Model)

### 1.1 ER 关系图

```mermaid
erDiagram
    users ||--o{ vocabulary : "owns"
    users ||--o{ learning_sessions : "has"
    users ||--o{ review_records : "has"
    vocabulary ||--o{ review_records : "reviewed_in"
    vocabulary }o--|| words : "references"
    learning_sessions ||--o{ session_words : "contains"
    session_words }o--|| vocabulary : "uses"

    users {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        varchar avatar_url
        int daily_goal
        varchar difficulty_level
        datetime created_at
        datetime updated_at
    }

    words {
        bigint id PK
        varchar word UK
        varchar phonetic
        text meaning_cn
        text meaning_en
        text example_sentence
        varchar difficulty
        datetime created_at
    }

    vocabulary {
        bigint id PK
        bigint user_id FK
        bigint word_id FK
        int familiarity
        int review_count
        float easiness_factor
        int interval_days
        datetime next_review_date
        datetime last_review_date
        datetime created_at
        datetime updated_at
    }

    learning_sessions {
        bigint id PK
        bigint user_id FK
        varchar session_type
        text ai_article
        int words_learned
        int words_correct
        int duration_seconds
        datetime started_at
        datetime ended_at
    }

    session_words {
        bigint id PK
        bigint session_id FK
        bigint vocabulary_id FK
        varchar action_type
        text user_sentence
        text ai_feedback
        int score
        datetime created_at
    }

    review_records {
        bigint id PK
        bigint user_id FK
        bigint vocabulary_id FK
        varchar familiarity_rating
        boolean is_correct
        int response_time_ms
        datetime reviewed_at
    }
```

### 1.2 数据表详细设计

#### users 用户表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 用户名 |
| email | VARCHAR(100) | UNIQUE | 邮箱 |
| password_hash | VARCHAR(255) | NOT NULL | bcrypt 加密密码 |
| avatar_url | VARCHAR(255) | NULL | 头像URL |
| daily_goal | INT | DEFAULT 20 | 每日目标词数 |
| difficulty_level | VARCHAR(20) | DEFAULT 'medium' | 难度偏好 (easy/medium/hard) |
| created_at | DATETIME | DEFAULT NOW() | 创建时间 |
| updated_at | DATETIME | ON UPDATE NOW() | 更新时间 |

**索引**: `idx_username`, `idx_email`

#### words 词库表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 单词ID |
| word | VARCHAR(100) | UNIQUE, NOT NULL | 单词 |
| phonetic | VARCHAR(100) | NULL | 音标 |
| meaning_cn | TEXT | NOT NULL | 中文释义 |
| meaning_en | TEXT | NULL | 英文释义 |
| example_sentence | TEXT | NULL | 例句 |
| difficulty | VARCHAR(20) | DEFAULT 'medium' | 难度等级 |
| created_at | DATETIME | DEFAULT NOW() | 创建时间 |

**索引**: `idx_word`, `idx_difficulty`

#### vocabulary 用户生词本

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | FK -> users.id | 用户ID |
| word_id | BIGINT | FK -> words.id | 单词ID |
| familiarity | INT | DEFAULT 0 | 熟悉度 (0-100) |
| review_count | INT | DEFAULT 0 | 复习次数 |
| easiness_factor | FLOAT | DEFAULT 2.5 | SM-2 简易因子 |
| interval_days | INT | DEFAULT 1 | 复习间隔天数 |
| next_review_date | DATETIME | NULL | 下次复习日期 |
| last_review_date | DATETIME | NULL | 上次复习日期 |
| created_at | DATETIME | DEFAULT NOW() | 添加时间 |
| updated_at | DATETIME | ON UPDATE NOW() | 更新时间 |

**索引**: `idx_user_word` (user_id, word_id), `idx_next_review` (user_id, next_review_date)

#### learning_sessions 学习会话表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 会话ID |
| user_id | BIGINT | FK -> users.id | 用户ID |
| session_type | VARCHAR(20) | NOT NULL | 类型 (learn/review) |
| ai_article | TEXT | NULL | AI生成的文章 |
| words_learned | INT | DEFAULT 0 | 学习单词数 |
| words_correct | INT | DEFAULT 0 | 正确单词数 |
| duration_seconds | INT | DEFAULT 0 | 持续时间(秒) |
| started_at | DATETIME | DEFAULT NOW() | 开始时间 |
| ended_at | DATETIME | NULL | 结束时间 |

**索引**: `idx_user_session` (user_id, started_at)

#### session_words 会话单词记录表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 记录ID |
| session_id | BIGINT | FK -> learning_sessions.id | 会话ID |
| vocabulary_id | BIGINT | FK -> vocabulary.id | 生词ID |
| action_type | VARCHAR(20) | NOT NULL | 操作类型 (skip/add/practice) |
| user_sentence | TEXT | NULL | 用户造句 |
| ai_feedback | TEXT | NULL | AI反馈 |
| score | INT | NULL | 评分 (0-100) |
| created_at | DATETIME | DEFAULT NOW() | 创建时间 |

**索引**: `idx_session` (session_id)

#### review_records 复习记录表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | FK -> users.id | 用户ID |
| vocabulary_id | BIGINT | FK -> vocabulary.id | 生词ID |
| rating | VARCHAR(20) | NOT NULL | 自评 (known/unknown) |
| test_passed | BOOLEAN | NULL | 测试是否通过 |
| response_time_ms | INT | NULL | 响应时间(毫秒) |
| created_at | DATETIME | DEFAULT NOW() | 创建时间 |

**索引**: `idx_user_review` (user_id, created_at)

### 1.3 数据持久化策略

| 策略 | 说明 |
|------|------|
| 主数据库 | MySQL 8.0，存储所有持久化数据 |
| 缓存层 | Redis，缓存用户会话、热门单词、每日统计 |
| Token 存储 | Redis，JWT Refresh Token 黑名单 |

---

## 2. 接口设计 (API Contracts)

### 一、Authentication Module (认证模块)

---

#### 1. Register (用户注册)

**Description**: 用户注册接口，用户点击注册按钮发起请求  
**关联用户地图**: [UM-01: 注册流]

**Endpoint**: `POST /api/auth/register`

**Header**: `null`

**Request**:
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "confirmPassword": "password123"
}
```

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "userId": 1,
    "username": "testuser"
  }
}
```

**Error Scenarios**:
- `400 Bad Request`: Code 1001 (用户名已存在)
- `400 Bad Request`: Code 1002 (邮箱已注册)
- `400 Bad Request`: Code 1003 (两次密码不一致)
- `400 Bad Request`: Code 1004 (密码强度不足)

**Database Logic**:
- Table: `users`
- Action: `INSERT`
- Notes: 密码存储前必须进行 bcrypt 加密，禁止明文存储

---

#### 2. Login (用户登录)

**Description**: 用户登录接口，返回 JWT Token  
**关联用户地图**: [UM-02: 登录流]

**Endpoint**: `POST /api/auth/login`

**Header**: `null`

**Request**:
```json
{
  "username": "testuser",
  "password": "password123"
}
```

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "username": "testuser",
      "avatar": null
    }
  }
}
```

**Error Scenarios**:
- `401 Unauthorized`: Code 1010 (用户名或密码错误)
- `403 Forbidden`: Code 1011 (账号已被禁用)

**Database Logic**:
- Table: `users`
- Action: `SELECT` (验证用户名密码)

---

#### 3. Refresh Token (刷新令牌)

**Description**: 使用 Refresh Token 获取新的 Access Token

**Endpoint**: `POST /api/auth/refresh`

**Header**: `Authorization: Bearer {refreshToken}`

**Request**: `null`

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600
  }
}
```

**Error Scenarios**:
- `401 Unauthorized`: Code 1020 (Refresh Token 无效或已过期)

---

#### 4. Logout (登出)

**Description**: 用户登出，将 Refresh Token 加入黑名单

**Endpoint**: `POST /api/auth/logout`

**Header**: `Authorization: Bearer {accessToken}`

**Request**: `null`

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

**Database Logic**:
- Redis: 将 Refresh Token 加入黑名单

---

### 二、Learning Module (学习模块)

---

#### 5. Get Words For Learning (获取学习单词)

**Description**: 获取待学习的单词列表  
**关联用户地图**: [UM-03: 学习新词流]

**Endpoint**: `GET /api/learning/words`

**Header**: `Authorization: Bearer {accessToken}`

**Query Params**:
- `count`: 获取数量 (默认 10)
- `difficulty`: 难度筛选 (可选)

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "words": [
      {
        "id": 1,
        "word": "ephemeral",
        "phonetic": "/ɪˈfemərəl/",
        "meaningCn": "adj. 短暂的，转瞬即逝的",
        "meaningEn": "lasting for a very short time",
        "exampleSentence": "Fame is often ephemeral."
      }
    ],
    "total": 10
  }
}
```

**Database Logic**:
- Table: `words`
- Action: `SELECT` (排除用户已加入生词本的单词)

---

#### 6. Add Word To Vocabulary (加入生词本)

**Description**: 将单词加入用户生词本  
**关联用户地图**: [UM-03: 学习新词流]

**Endpoint**: `POST /api/vocabulary`

**Header**: `Authorization: Bearer {accessToken}`

**Request**:
```json
{
  "wordId": 1
}
```

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "vocabularyId": 123
  }
}
```

**Error Scenarios**:
- `400 Bad Request`: Code 2001 (单词已在生词本中)
- `404 Not Found`: Code 2002 (单词不存在)

**Database Logic**:
- Table: `vocabulary`
- Action: `INSERT`

---

#### 7. Generate AI Article (AI 生成文章)

**Description**: 调用 AI 生成包含指定生词的文章  
**关联用户地图**: [UM-04: AI 文章学习流]

**Endpoint**: `POST /api/learning/article`

**Header**: `Authorization: Bearer {accessToken}`

**Request**:
```json
{
  "vocabularyIds": [1, 2, 3, 4, 5],
  "difficulty": "medium",
  "length": "short"
}
```

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "sessionId": 456,
    "article": {
      "title": "The Art of Living in the Moment",
      "content": "In our fast-paced modern world, we often forget that life is fundamentally ephemeral...",
      "highlightWords": [
        {"word": "ephemeral", "positions": [12, 45]}
      ]
    }
  }
}
```

**Error Scenarios**:
- `400 Bad Request`: Code 2010 (生词数量不足)
- `503 Service Unavailable`: Code 2011 (AI 服务暂时不可用)

**Database Logic**:
- Table: `learning_sessions`
- Action: `INSERT` (创建新会话)
- External: 调用硅基流动 API

---

#### 8. Submit Sentence (提交造句)

**Description**: 用户提交造句，AI 进行批改  
**关联用户地图**: [UM-05: 造句练习流]

**Endpoint**: `POST /api/learning/sentence`

**Header**: `Authorization: Bearer {accessToken}`

**Request**:
```json
{
  "sessionId": 456,
  "vocabularyId": 1,
  "sentence": "The beauty of cherry blossoms is ephemeral."
}
```

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "score": 95,
    "isCorrect": true,
    "feedback": {
      "grammar": "句式结构完整",
      "usage": "单词使用正确，准确表达了'短暂'的含义",
      "suggestion": "可以尝试使用更复杂的从句结构来丰富表达"
    }
  }
}
```

**Error Scenarios**:
- `400 Bad Request`: Code 2020 (句子为空)
- `400 Bad Request`: Code 2021 (未包含目标单词)

**Database Logic**:
- Table: `session_words`
- Action: `INSERT`
- External: 调用硅基流动 API 进行批改

---

### 三、Review Module (复习模块)

---

#### 9. Get Review Queue (获取复习队列)

**Description**: 获取今日待复习的单词队列  
**关联用户地图**: [UM-06: 复习测试流]

**Endpoint**: `GET /api/review/queue`

**Header**: `Authorization: Bearer {accessToken}`

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "words": [
      {
        "vocabularyId": 123,
        "word": "ubiquitous",
        "phonetic": "/juːˈbɪkwɪtəs/",
        "meaningCn": "adj. 无处不在的",
        "familiarity": 45,
        "reviewCount": 3
      }
    ],
    "total": 8
  }
}
```

**Database Logic**:
- Table: `vocabulary`
- Action: `SELECT WHERE next_review_date <= NOW()`

---

#### 10. Submit Familiarity Rating (提交熟悉度自评)

**Description**: 用户提交对单词的熟悉度自评  
**关联用户地图**: [UM-06: 复习测试流]

**Endpoint**: `POST /api/review/rating`

**Header**: `Authorization: Bearer {accessToken}`

**Request**:
```json
{
  "vocabularyId": 123,
  "rating": "known"
}
```

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "needTest": true,
    "testQuestion": {
      "type": "choice",
      "question": "请选择 'ubiquitous' 的正确释义",
      "options": [
        {"id": "A", "text": "独特的，独一无二的"},
        {"id": "B", "text": "无处不在的，普遍存在的"},
        {"id": "C", "text": "模糊的，不清楚的"},
        {"id": "D", "text": "紧急的，迫切的"}
      ],
      "correctAnswer": "B"
    }
  }
}
```

**Database Logic**:
- Table: `review_records`
- Action: `INSERT`

---

#### 11. Submit Test Answer (提交测试答案)

**Description**: 提交复习测试的答案  
**关联用户地图**: [UM-06: 复习测试流]

**Endpoint**: `POST /api/review/answer`

**Header**: `Authorization: Bearer {accessToken}`

**Request**:
```json
{
  "vocabularyId": 123,
  "answer": "无处不在的，普遍存在的",
  "isFromErrorQueue": false,
  "responseTimeMs": 2500
}
```

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "isCorrect": true,
    "correctAnswer": "B",
    "nextReviewDate": "2026-01-20",
    "newFamiliarity": 60
  }
}
```

**Database Logic**:
- Table: `vocabulary`
- Action: `UPDATE` (更新 SM-2 参数)
- Table: `review_records`
- Action: `UPDATE`

---

### 四、Vocabulary Module (生词本模块)

---

#### 12. Get Vocabulary List (获取生词列表)

**Description**: 获取用户生词本列表

**Endpoint**: `GET /api/vocabulary`

**Header**: `Authorization: Bearer {accessToken}`

**Query Params**:
- `page`: 页码 (默认 1)
- `size`: 每页数量 (默认 20)
- `status`: 状态筛选 (new/learning/mastered)
- `search`: 搜索关键词

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "words": [
      {
        "vocabularyId": 123,
        "word": "ephemeral",
        "meaningCn": "adj. 短暂的",
        "familiarity": 80,
        "status": "mastered",
        "nextReviewDate": "2026-01-25",
        "addedAt": "2026-01-10"
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 56
    }
  }
}
```

**Database Logic**:
- Table: `vocabulary` JOIN `words`
- Action: `SELECT`

---

#### 13. Delete Vocabulary (删除生词)

**Description**: 从生词本中删除单词

**Endpoint**: `DELETE /api/vocabulary/{vocabularyId}`

**Header**: `Authorization: Bearer {accessToken}`

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

**Error Scenarios**:
- `404 Not Found`: Code 3001 (生词不存在)
- `403 Forbidden`: Code 3002 (无权操作)

**Database Logic**:
- Table: `vocabulary`
- Action: `DELETE`

---

### 五、Statistics Module (统计模块)

---

#### 14. Get Dashboard Stats (获取仪表盘统计)

**Description**: 获取首页统计数据  
**关联用户地图**: [UM-07: 查看统计流]

**Endpoint**: `GET /api/stats/overview`

**Header**: `Authorization: Bearer {accessToken}`

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "todayLearned": 12,
    "todayReviewed": 8,
    "pendingReview": 15,
    "totalWords": 256
  }
}
```

**Database Logic**:
- Table: `vocabulary`, `learning_sessions`, `review_records`
- Action: `SELECT` (聚合查询)

---

#### 15. Get Weekly Stats (获取周学习统计)

**Description**: 获取本周每日学习数据

**Endpoint**: `GET /api/stats/weekly`

**Header**: `Authorization: Bearer {accessToken}`

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "weekData": [
      {"day": "Mon", "learned": 15, "reviewed": 10},
      {"day": "Tue", "learned": 20, "reviewed": 12},
      {"day": "Wed", "learned": 12, "reviewed": 8},
      {"day": "Thu", "learned": 25, "reviewed": 15},
      {"day": "Fri", "learned": 18, "reviewed": 11},
      {"day": "Sat", "learned": 10, "reviewed": 5},
      {"day": "Sun", "learned": 16, "reviewed": 9}
    ]
  }
}
```

**Database Logic**:
- Table: `learning_sessions`, `review_records`
- Action: `SELECT GROUP BY DATE`

---

## 3. 状态码规范

| HTTP Status | 业务 Code 范围 | 说明 |
|-------------|----------------|------|
| 200 | 0 | 成功 |
| 400 | 1000-1999 | 认证相关错误 |
| 400 | 2000-2999 | 学习/复习相关错误 |
| 400 | 3000-3999 | 生词本相关错误 |
| 401 | - | 未认证 |
| 403 | - | 无权限 |
| 404 | - | 资源不存在 |
| 500 | 9000-9999 | 服务端错误 |

---

## 4. 安全规范

| 规范 | 说明 |
|------|------|
| 密码加密 | bcrypt (cost=12) |
| Token 类型 | JWT (HS256) |
| Access Token 有效期 | 1 小时 |
| Refresh Token 有效期 | 7 天 |
| 敏感信息 | 禁止日志记录密码、Token |
| CORS | 配置允许的域名 |
| Rate Limit | 登录接口限流 10次/分钟 |
---

## 5. 前端路由架构 (Frontend Routing)

### 5.1 路由结构

应用采用 **嵌套路由** 架构,所有主要功能页面共享统一�?MainLayout 布局,提供持久化侧边栏导航�?

```
/
├── /login                  # 登录�?(独立布局)
├── /register               # 注册�?(独立布局)
└── / (MainLayout)          # 主布局容器
    ├── /                   # 首页 (HomeView)
    ├── /learn              # 学习�?(LearnView)
    ├── /vocabulary         # 生词�?(VocabularyView)
    └── /review             # 复习�?(ReviewView)
```

### 5.2 路由守卫

- **认证守卫**: 所有主要功能页�?(`requiresAuth: true`) 需�?JWT Token
- **重定向逻辑**:
  - 未登录访问受保护页面 �?重定向到 `/login`
  - 已登录访问登�?注册�?�?重定向到 `/` (首页)

### 5.3 MainLayout 组件特�?

| 特�?| 说明 |
|------|------|
| **持久化导�?* | 左侧边栏在页面切换时保持可见 |
| **路由高亮** | 当前活动路由自动高亮显示 |
| **页面过渡动画** | fade-slide 过渡效果 (400ms cubic-bezier) |
| **用户信息展示** | 侧边栏底部显示当前登录用�?|
| **登出功能** | 统一的登出按�?清除 Token 并重定向 |

### 5.4 组件层级结构

```
App.vue
├── LoginView.vue (独立)
├── RegisterView.vue (独立)
└── MainLayout.vue (布局容器)
    ├── Sidebar (导航菜单)
    ├── UserProfile (用户信息)
    └── <router-view> (动态内容区)
        ├── HomeView.vue
        �?  └── WeeklyChart.vue (图表组件)
        ├── LearnView.vue
        ├── VocabularyView.vue
        └── ReviewView.vue
```
