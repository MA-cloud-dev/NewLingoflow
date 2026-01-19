import request from './request'

export interface RegisterData {
    username: string
    email?: string
    password: string
    confirmPassword: string
}

export interface LoginData {
    username: string
    password: string
}

export interface LoginResponse {
    accessToken: string
    refreshToken: string
    expiresIn: number
    user: {
        id: number
        username: string
        avatar: string | null
    }
}

export interface ApiResponse<T> {
    code: number
    msg: string
    data: T
}

export function register(data: RegisterData): Promise<ApiResponse<{ userId: number; username: string }>> {
    return request.post('/auth/register', data)
}

export function login(data: LoginData): Promise<ApiResponse<LoginResponse>> {
    return request.post('/auth/login', data)
}

export function logout(): Promise<ApiResponse<null>> {
    return request.post('/auth/logout')
}
