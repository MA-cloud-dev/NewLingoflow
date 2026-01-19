import request from './request'
import type { ApiResponse } from './auth'

export interface StatsOverview {
    todayLearned: number
    pendingReview: number
    totalWords: number
    streakDays: number
    dailyGoal: number
}

export interface WeeklyData {
    date: string
    count: number
}

// 获取学习统计概览
export function getStatsOverview(): Promise<ApiResponse<StatsOverview>> {
    return request.get('/stats/overview')
}

// 获取周学习数据
export function getWeeklyStats(): Promise<ApiResponse<WeeklyData[]>> {
    return request.get('/stats/weekly')
}
