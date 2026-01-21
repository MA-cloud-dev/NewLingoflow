import request from './request'
import type { ApiResponse } from './auth'

export interface Word {
    id: number
    word: string
    phonetic: string
    meaningCn: string
    meaningEn: string
    exampleSentence: string
    difficulty: string
    levelTags?: string  // 词库级别标签，如 "CET-4,CET-6"
}

export interface VocabularyItem {
    id: number
    userId: number
    wordId: number
    familiarity: number
    reviewCount: number
    nextReviewDate?: string
    lastReviewDate?: string
    word: Word
}

export interface ComprehensionQuestion {
    type: 'word_comprehension' | 'main_idea'
    word?: string
    question: string
    options: string[]
    correctAnswer: string
    explanation: string
}

export interface SentenceTask {
    word: string
    theme: string
    chineseExample: string
}

export interface ArticleData {
    title: string
    content: string
    chineseTranslation?: string
    highlightWords: string[]
    comprehensionQuestions?: ComprehensionQuestion[]
    sentenceMakingTasks?: SentenceTask[]
}

export interface SentenceFeedback {
    score: number
    isCorrect: boolean
    feedback: {
        grammar: string
        usage: string
        suggestion: string
    }
}

// 获取学习单词
export function getLearningWords(count: number = 10, difficulty?: string): Promise<ApiResponse<{ words: Word[], total: number, currentIndex?: number, selectedWords?: Word[] }>> {
    const params = new URLSearchParams({ count: count.toString() })
    if (difficulty) params.append('difficulty', difficulty)
    return request.get(`/learning/words?${params}`)
}

// 更新学习进度
export function updateLearningProgress(currentIndex: number, selectedWords: Word[]): Promise<ApiResponse<null>> {
    return request.post('/learning/progress', { currentIndex, selectedWords })
}

// 获取所有单词
export function getAllWords(difficulty?: string): Promise<ApiResponse<{ words: Word[], total: number }>> {
    const params = difficulty ? `?difficulty=${difficulty}` : ''
    return request.get(`/words${params}`)
}

// 获取用户生词本
export function getUserVocabulary(status: string = 'all', page: number = 1, pageSize: number = 20): Promise<ApiResponse<{ vocabulary: VocabularyItem[], total: number }>> {
    return request.get(`/vocabulary?status=${status}&page=${page}&pageSize=${pageSize}`)
}

// 添加单词到生词本
export function addToVocabulary(wordId: number): Promise<ApiResponse<{ vocabularyId: number }>> {
    return request.post('/vocabulary', { wordId })
}

// 批量添加单词到生词本并获取详情
export function batchAddToVocabulary(wordIds: number[]): Promise<ApiResponse<VocabularyItem[]>> {
    return request.post('/vocabulary/batch', { wordIds })
}

// 从生词本删除
export function removeFromVocabulary(id: number): Promise<ApiResponse<null>> {
    return request.delete(`/vocabulary/${id}`)
}

// 生成 AI 文章
export function generateArticle(vocabularyIds: number[], difficulty?: string, length?: string, theme?: string): Promise<ApiResponse<{ sessionId: number, article: ArticleData }>> {
    return request.post('/learning/article', { vocabularyIds, difficulty, length, theme })
}

// 提交造句
export function submitSentence(sessionId: number, vocabularyId: number, sentence: string): Promise<ApiResponse<SentenceFeedback>> {
    return request.post('/learning/sentence', { sessionId, vocabularyId, sentence })
}

// ========== 复习模块 ==========

export interface ReviewWord {
    vocabularyId: number
    word: string
    phonetic: string
    meaningCn: string
    familiarity: number
    reviewCount: number
}

export interface TestQuestion {
    type: string
    word: string
    phonetic: string
    options: string[]
    correctAnswer: string
}

export interface RatingResult {
    needTest: boolean
    testQuestion?: TestQuestion
    correctAnswer?: string
    word?: string
}

export interface AnswerResult {
    isCorrect: boolean
    correctAnswer: string
    nextReviewDate?: string
    newFamiliarity?: number
}

// 获取复习队列
export function getReviewQueue(): Promise<ApiResponse<{ words: ReviewWord[], total: number }>> {
    return request.get('/review/queue')
}

// 提交熟悉度自评
export function submitRating(vocabularyId: number, rating: 'known' | 'unknown' | 'fuzzy'): Promise<ApiResponse<RatingResult>> {
    return request.post('/review/rating', { vocabularyId, rating })
}

// 提交测试答案
export function submitTestAnswer(vocabularyId: number, answer: string, isFromErrorQueue: boolean = false, responseTimeMs?: number): Promise<ApiResponse<AnswerResult>> {
    return request.post('/review/answer', { vocabularyId, answer, isFromErrorQueue, responseTimeMs })
}
