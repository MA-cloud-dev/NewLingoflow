# LingoFlow

> AI-driven contextual vocabulary learning application

## 项目简介

LingoFlow 是一款基于AI的英语词汇学习应用，通过生成语境化文章帮助用户深度记忆单词。

### 核心功能

- 🧠 **智能词典系统** - 支持四级、六级、雅思、托福多词典切换
- 📖 **AI文章生成** - 根据选词生成个性化阅读材料
- ✍️ **造句练习** - AI批改造句，提供详细反馈
- 📊 **阅读理解** - 词汇理解和主旨题目
- 🔄 **SM-2复习算法** - 科学安排复习计划

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Element Plus |
| 后端 | Spring Boot 3.x + MyBatis |
| 数据库 | MySQL 8.0 |
| AI服务 | 硅基流动 API |

## 目录结构

```
NewLingoflow/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue 3 前端
├── spec/             # 规格文档
│   └── docs/
│       ├── requirement.md
│       ├── design.md
│       └── prototype/
└── README.md
```

## 快速开始

### 后端

```bash
cd backend
mvn spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

## 文档

- [需求文档](spec/docs/requirement.md)
- [设计文档](spec/docs/design.md)

## License

MIT
