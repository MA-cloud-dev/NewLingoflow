import request from './request'
import type { ApiResponse } from './auth'

/**
 * 词典接口类型定义
 * Phase 6: 词典系统
 */
export interface Dictionary {
    id: number
    name: string
    description: string
    totalWords: number
    createdAt: string
}

export interface DictionaryProgress {
    dictionaryId: number
    dictionaryName: string
    totalWords: number
    learnedWords: number
    progress: number
}

/**
 * 获取所有词典列表
 */
export function getAllDictionaries(): Promise<ApiResponse<Dictionary[]>> {
    return request.get('/dictionaries')
}

/**
 * 获取用户在特定词典的学习进度
 */
export function getDictionaryProgress(dictionaryId: number): Promise<ApiResponse<DictionaryProgress>> {
    return request.get(`/dictionaries/${dictionaryId}/progress`)
}
