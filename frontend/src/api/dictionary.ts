import request from './request'
import type { ApiResponse } from './auth'

export interface Dictionary {
    id: number
    name: string
    description: string
    totalWords: number
}

export interface DictionaryProgress {
    dictionaryId: number
    dictionaryName: string
    totalWords: number
    learnedWords: number
    progress: number
}

// 获取所有词典列表
export function getAllDictionaries(): Promise<ApiResponse<Dictionary[]>> {
    return request.get('/dictionaries')
}

// 获取用户在特定词典的学习进度
export function getDictionaryProgress(id: number): Promise<ApiResponse<DictionaryProgress>> {
    return request.get(`/dictionaries/${id}/progress`)
}
