import request from './request'
import type { ApiResponse } from './auth'

export interface UserProfile {
    id: number
    username: string
    email: string | null
    avatarUrl: string | null
    dailyGoal: number
    difficultyLevel: string
    createdAt: string
    totalWordsLearned: number
    streakDays: number
}

export interface UpdateProfileData {
    email?: string
    avatarUrl?: string
    dailyGoal?: number
    difficultyLevel?: string
}

export interface ChangePasswordData {
    currentPassword: string
    newPassword: string
    confirmPassword: string
}

/**
 * 获取当前用户个人信息
 */
export function getUserProfile(): Promise<ApiResponse<UserProfile>> {
    return request.get('/user/profile')
}

/**
 * 更新个人信息
 */
export function updateProfile(data: UpdateProfileData): Promise<ApiResponse<UserProfile>> {
    return request.put('/user/profile', data)
}

/**
 * 修改密码
 */
export function changePassword(data: ChangePasswordData): Promise<ApiResponse<null>> {
    return request.put('/user/password', data)
}
